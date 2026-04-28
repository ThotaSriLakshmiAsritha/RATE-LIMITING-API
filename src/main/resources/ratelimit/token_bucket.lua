-- Atomic token bucket using Redis server time (TIME).
--
-- KEYS[1] = bucket key
-- ARGV[1] = capacity (number)
-- ARGV[2] = refill_rate_per_sec (number)
-- ARGV[3] = tokens_required (number)
--
-- Returns array:
-- [1] allowed (1/0)
-- [2] tokens_remaining (number)
-- [3] limit_capacity (number)
-- [4] reset_unix_seconds (number)
-- [5] retry_after_seconds (number)

local key = KEYS[1]
local capacity = tonumber(ARGV[1])
local refill_rate = tonumber(ARGV[2])
local tokens_required = tonumber(ARGV[3])

if capacity == nil or capacity <= 0 then
  return {0, 0, 0, 0, 0}
end
if refill_rate == nil or refill_rate <= 0 then
  return {0, 0, capacity, 0, 0}
end
if tokens_required == nil or tokens_required <= 0 then
  tokens_required = 1
end

local t = redis.call('TIME')
local now_sec = tonumber(t[1])
local now_usec = tonumber(t[2])
local now_ms = (now_sec * 1000) + math.floor(now_usec / 1000)

local existing = redis.call('HMGET', key, 'tokens', 'last_refill_ms')
local tokens = tonumber(existing[1])
local last_refill_ms = tonumber(existing[2])

if tokens == nil then
  tokens = capacity
  last_refill_ms = now_ms
end
if last_refill_ms == nil then
  last_refill_ms = now_ms
end

local elapsed_ms = now_ms - last_refill_ms
if elapsed_ms < 0 then
  elapsed_ms = 0
end

local refill = (elapsed_ms / 1000.0) * refill_rate
tokens = math.min(capacity, tokens + refill)
last_refill_ms = now_ms

local allowed = 0
local remaining = tokens
if tokens >= tokens_required then
  allowed = 1
  remaining = tokens - tokens_required
end

redis.call('HSET', key,
  'tokens', remaining,
  'last_refill_ms', last_refill_ms,
  'capacity', capacity,
  'refill_rate_per_sec', refill_rate
)

local ttl_seconds = math.ceil(2.0 * (capacity / refill_rate))
if ttl_seconds < 1 then
  ttl_seconds = 1
end
redis.call('EXPIRE', key, ttl_seconds)

local missing = math.max(0, tokens_required - tokens)
local retry_after_seconds = 0
if missing > 0 then
  retry_after_seconds = math.ceil(missing / refill_rate)
end

local reset_unix_seconds = now_sec + retry_after_seconds

return {allowed, remaining, capacity, reset_unix_seconds, retry_after_seconds}


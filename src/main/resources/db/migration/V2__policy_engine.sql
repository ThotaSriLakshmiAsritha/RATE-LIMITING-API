CREATE TABLE rate_limit_policies (
  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
  tenant_id VARCHAR(120) NOT NULL,
  scope_type VARCHAR(20) NOT NULL,
  scope_id VARCHAR(120),
  endpoint_pattern VARCHAR(255),
  requests_per_minute INTEGER,
  burst_capacity INTEGER,
  dimension VARCHAR(20),
  error_message TEXT,
  mode VARCHAR(20) NOT NULL DEFAULT 'ENFORCE',
  priority INTEGER NOT NULL DEFAULT 0,
  enabled BOOLEAN NOT NULL DEFAULT TRUE,
  version BIGINT NOT NULL DEFAULT 0,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_at TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_rl_policy_scope ON rate_limit_policies(tenant_id, scope_type, scope_id, enabled);
CREATE INDEX idx_rl_policy_endpoint ON rate_limit_policies(tenant_id, scope_type, enabled);

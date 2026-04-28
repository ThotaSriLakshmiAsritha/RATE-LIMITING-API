const decodeBase64Url = (value: string) => {
  const normalized = value.replace(/-/g, '+').replace(/_/g, '/');
  const padded = normalized.padEnd(normalized.length + ((4 - (normalized.length % 4 || 4)) % 4), '=');
  return atob(padded);
};

export function decodeJwt(token: string) {
  const parts = token.split('.');
  if (parts.length < 2) {
    throw new Error('Invalid JWT');
  }
  return JSON.parse(decodeBase64Url(parts[1]));
}

export function isTokenExpired(token: string) {
  try {
    const payload = decodeJwt(token);
    return typeof payload.exp !== 'number' || Date.now() / 1000 >= payload.exp;
  } catch {
    return true;
  }
}

export function getTokenExpiry(token: string) {
  try {
    const payload = decodeJwt(token);
    return typeof payload.exp === 'number' ? payload.exp * 1000 : null;
  } catch {
    return null;
  }
}

export function getRole(token: string) {
  try {
    const payload = decodeJwt(token);
    if (typeof payload.role === 'string') return payload.role.toUpperCase();
    if (Array.isArray(payload.roles) && typeof payload.roles[0] === 'string') {
      return payload.roles[0].replace(/^ROLE_/, '').toUpperCase();
    }
    return 'USER';
  } catch {
    return 'USER';
  }
}

export function getJwtSubject(token: string) {
  try {
    const payload = decodeJwt(token);
    return payload.preferred_username ?? payload.username ?? payload.sub ?? 'demo';
  } catch {
    return 'demo';
  }
}

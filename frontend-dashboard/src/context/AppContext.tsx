import { createContext, useCallback, useContext, useEffect, useMemo, useReducer, useRef, type ReactNode } from 'react';
import { ApiError, apiRequest } from '../api/client';
import type { AppState, HealthState, Metrics, RateLimitHeaders, RequestEntry, ToastItem } from '../types';
import { extractRateLimitHeaders } from '../utils/headers';
import { getJwtSubject, getRole, getTokenExpiry, isTokenExpired } from '../utils/jwt';

const API_BASE_URL_KEY = 'apiBaseUrl';
const AUTH_TOKEN_KEY = 'authToken';
const USERNAME_KEY = 'authUsername';
const REQUEST_HISTORY_KEY = 'requestHistory';
const HIDE_EXPLAINER_KEY = 'dashboardExplainerHidden';

type RequestConfig<T> = {
  label: string;
  path: string;
  method?: string;
  body?: unknown;
  requiresAuth?: boolean;
  trackHistory?: boolean;
  toastOnSuccess?: boolean;
  historyEndpointLabel?: string;
  onSuccess?: (data: T, status: number, headers: Headers) => void;
};

type AppContextValue = {
  state: AppState;
  metrics: Metrics;
  explainerHidden: boolean;
  setExplainerHidden: (hidden: boolean) => void;
  setApiBaseUrl: (value: string) => void;
  setSidebarCollapsed: (collapsed: boolean) => void;
  setBackendHealth: (health: HealthState) => void;
  addToast: (toast: Omit<ToastItem, 'id'>) => void;
  removeToast: (id: string) => void;
  login: (token: string, username?: string | null) => void;
  logout: (redirectToLogin?: boolean) => void;
  clearHistory: () => void;
  executeTrackedRequest: <T = unknown>(config: RequestConfig<T>) => Promise<T>;
};

type Action =
  | { type: 'SET_AUTH'; token: string; username: string | null }
  | { type: 'LOGOUT' }
  | { type: 'SET_API_BASE_URL'; value: string }
  | { type: 'SET_BACKEND_HEALTH'; value: HealthState }
  | { type: 'SET_SIDEBAR_COLLAPSED'; value: boolean }
  | { type: 'ADD_HISTORY'; value: RequestEntry }
  | { type: 'CLEAR_HISTORY' }
  | { type: 'ADD_TOAST'; value: ToastItem }
  | { type: 'REMOVE_TOAST'; value: string }
  | { type: 'SET_LATEST_HEADERS'; value: RateLimitHeaders | null };

const initialToken = localStorage.getItem(AUTH_TOKEN_KEY);
const initialUsername = localStorage.getItem(USERNAME_KEY);

const getInitialAuth = () => {
  if (!initialToken || isTokenExpired(initialToken)) {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    return { authToken: null, username: null, role: null, tokenExpiry: null };
  }
  return {
    authToken: initialToken,
    username: initialUsername ?? getJwtSubject(initialToken),
    role: getRole(initialToken),
    tokenExpiry: getTokenExpiry(initialToken),
  };
};

const initialState: AppState = {
  ...getInitialAuth(),
  apiBaseUrl: localStorage.getItem(API_BASE_URL_KEY) || 'http://localhost:8080',
  requestHistory: (() => {
    try {
      return JSON.parse(sessionStorage.getItem(REQUEST_HISTORY_KEY) || '[]') as RequestEntry[];
    } catch {
      return [];
    }
  })(),
  latestHeaders: null,
  backendHealth: { status: 'checking', latencyMs: null, lastCheckedAt: null },
  toasts: [],
  sidebarCollapsed: false,
};

const computeMetrics = (history: RequestEntry[]): Metrics =>
  history.reduce(
    (acc, item) => {
      acc.total += 1;
      if (item.statusCode >= 200 && item.statusCode < 300) acc.successful += 1;
      else if (item.statusCode === 429) acc.throttled += 1;
      else acc.errors += 1;
      return acc;
    },
    { total: 0, successful: 0, throttled: 0, errors: 0 },
  );

function reducer(state: AppState, action: Action): AppState {
  switch (action.type) {
    case 'SET_AUTH':
      return {
        ...state,
        authToken: action.token,
        username: action.username ?? getJwtSubject(action.token),
        role: getRole(action.token),
        tokenExpiry: getTokenExpiry(action.token),
      };
    case 'LOGOUT':
      return { ...state, authToken: null, username: null, role: null, tokenExpiry: null };
    case 'SET_API_BASE_URL':
      return { ...state, apiBaseUrl: action.value };
    case 'SET_BACKEND_HEALTH':
      return { ...state, backendHealth: action.value };
    case 'SET_SIDEBAR_COLLAPSED':
      return { ...state, sidebarCollapsed: action.value };
    case 'ADD_HISTORY':
      return { ...state, requestHistory: [action.value, ...state.requestHistory].slice(0, 40) };
    case 'CLEAR_HISTORY':
      return { ...state, requestHistory: [], latestHeaders: null };
    case 'ADD_TOAST':
      return { ...state, toasts: [...state.toasts, action.value] };
    case 'REMOVE_TOAST':
      return { ...state, toasts: state.toasts.filter((toast) => toast.id !== action.value) };
    case 'SET_LATEST_HEADERS':
      return { ...state, latestHeaders: action.value };
    default:
      return state;
  }
}

const AppContext = createContext<AppContextValue | undefined>(undefined);

export function AppProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, initialState);
  const hideExplainerRef = useRef(localStorage.getItem(HIDE_EXPLAINER_KEY) === 'true');

  useEffect(() => {
    sessionStorage.setItem(REQUEST_HISTORY_KEY, JSON.stringify(state.requestHistory));
  }, [state.requestHistory]);

  const addToast = useCallback((toast: Omit<ToastItem, 'id'>) => {
    const id = crypto.randomUUID();
    dispatch({ type: 'ADD_TOAST', value: { ...toast, id } });
    window.setTimeout(() => dispatch({ type: 'REMOVE_TOAST', value: id }), 4000);
  }, []);

  const logout = useCallback((redirectToLogin = false) => {
    localStorage.removeItem(AUTH_TOKEN_KEY);
    localStorage.removeItem(USERNAME_KEY);
    dispatch({ type: 'LOGOUT' });
    if (redirectToLogin) {
      window.location.assign('/login');
    }
  }, []);

  const login = useCallback((token: string, username?: string | null) => {
    localStorage.setItem(AUTH_TOKEN_KEY, token);
    localStorage.setItem(USERNAME_KEY, username ?? getJwtSubject(token));
    dispatch({ type: 'SET_AUTH', token, username: username ?? null });
  }, []);

  const executeTrackedRequest = useCallback(async <T,>({
    label,
    path,
    method = 'GET',
    body,
    requiresAuth = false,
    trackHistory = true,
    toastOnSuccess = true,
    historyEndpointLabel,
    onSuccess,
  }: RequestConfig<T>) => {
    const startedAt = performance.now();
    const token = state.authToken;
    const timestamp = new Date().toISOString();

    try {
      if (requiresAuth && !token) {
        throw new ApiError('Unauthorized', { status: 401 });
      }

      const result = await apiRequest<T>({
        baseUrl: state.apiBaseUrl,
        path,
        method,
        body,
        token: requiresAuth ? token : null,
      });

      const latencyMs = performance.now() - startedAt;
      const headers = extractRateLimitHeaders(result.headers);
      if (trackHistory) {
        dispatch({
          type: 'ADD_HISTORY',
          value: {
            id: crypto.randomUUID(),
            timestamp,
            endpoint: historyEndpointLabel ?? path,
            method,
            statusCode: result.status,
            latencyMs,
            rateLimitHeaders: headers,
            responseBody: result.data,
          },
        });
        dispatch({ type: 'SET_LATEST_HEADERS', value: headers });
      }
      if (toastOnSuccess) {
        addToast({
          tone: 'success',
          title: `${label} succeeded`,
          description: `HTTP ${result.status} received from ${path}`,
        });
      }
      onSuccess?.(result.data, result.status, result.headers);
      return result.data;
    } catch (error) {
      const apiError = error as ApiError;
      const latencyMs = performance.now() - startedAt;
      const headers = apiError.headers ? extractRateLimitHeaders(apiError.headers) : apiError.rateLimitHeaders ?? null;
      const status = apiError.status ?? 0;

      if (trackHistory) {
        dispatch({
          type: 'ADD_HISTORY',
          value: {
            id: crypto.randomUUID(),
            timestamp,
            endpoint: historyEndpointLabel ?? path,
            method,
            statusCode: status,
            latencyMs,
            rateLimitHeaders: headers ?? { limit: null, remaining: null, reset: null, retryAfter: null },
            responseBody: apiError.data ?? apiError.message,
          },
        });
        dispatch({ type: 'SET_LATEST_HEADERS', value: headers });
      }

      if (status === 401) {
        addToast({ tone: 'error', title: 'Unauthorized', description: 'Please log in again.' });
        logout(true);
      } else if (status === 403) {
        addToast({ tone: 'error', title: 'Access denied', description: 'Your current role is not allowed to do that.' });
      } else if (status === 400) {
        addToast({ tone: 'error', title: 'Bad Request', description: 'Check the request payload.' });
      } else if (status === 404) {
        addToast({ tone: 'error', title: 'Endpoint not found', description: `No endpoint responded at ${state.apiBaseUrl}${path}.` });
      } else if (status === 429) {
        addToast({
          tone: 'warning',
          title: 'Rate limit hit!',
          description: `Retry after ${headers?.retryAfter ?? 'a few'} seconds.`,
          countdownSeconds: headers?.retryAfter,
        });
      } else if (status >= 500) {
        addToast({ tone: 'error', title: `Server error ${status}`, description: 'Check backend logs for more detail.' });
      } else if (apiError.isNetworkError) {
        dispatch({
          type: 'SET_BACKEND_HEALTH',
          value: { status: 'offline', latencyMs: null, lastCheckedAt: new Date().toISOString() },
        });
        addToast({ tone: 'error', title: 'Network error', description: `Is the backend running at ${state.apiBaseUrl}?` });
      } else {
        addToast({ tone: 'error', title: label, description: typeof apiError.data === 'string' ? apiError.data : apiError.message });
      }

      throw error;
    }
  }, [addToast, logout, state.apiBaseUrl, state.authToken]);

  const setExplainerHidden = useCallback((hidden: boolean) => {
    hideExplainerRef.current = hidden;
    localStorage.setItem(HIDE_EXPLAINER_KEY, String(hidden));
  }, []);

  const setApiBaseUrl = useCallback((value: string) => {
    localStorage.setItem(API_BASE_URL_KEY, value);
    dispatch({ type: 'SET_API_BASE_URL', value });
  }, []);

  const setSidebarCollapsed = useCallback((collapsed: boolean) => {
    dispatch({ type: 'SET_SIDEBAR_COLLAPSED', value: collapsed });
  }, []);

  const setBackendHealth = useCallback((health: HealthState) => {
    dispatch({ type: 'SET_BACKEND_HEALTH', value: health });
  }, []);

  const removeToast = useCallback((id: string) => {
    dispatch({ type: 'REMOVE_TOAST', value: id });
  }, []);

  const clearHistory = useCallback(() => {
    dispatch({ type: 'CLEAR_HISTORY' });
  }, []);

  const value = useMemo<AppContextValue>(
    () => ({
      state,
      metrics: computeMetrics(state.requestHistory),
      explainerHidden: hideExplainerRef.current,
      setExplainerHidden,
      setApiBaseUrl,
      setSidebarCollapsed,
      setBackendHealth,
      addToast,
      removeToast,
      login,
      logout,
      clearHistory,
      executeTrackedRequest,
    }),
    [addToast, clearHistory, executeTrackedRequest, login, logout, removeToast, setApiBaseUrl, setBackendHealth, setExplainerHidden, setSidebarCollapsed, state],
  );

  return <AppContext.Provider value={value}>{children}</AppContext.Provider>;
}

export function useAppContext() {
  const context = useContext(AppContext);
  if (!context) {
    throw new Error('useAppContext must be used inside AppProvider');
  }
  return context;
}

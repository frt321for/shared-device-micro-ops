export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export class ApiError extends Error {
  status: number;

  constructor(status: number, message: string) {
    super(message);
    this.name = 'ApiError';
    this.status = status;
  }
}

const BASE_URL = 'http://localhost:8080/api/v1';

function getToken(): string | null {
  try {
    return localStorage.getItem('auth_token');
  } catch {
    return null;
  }
}

function buildQuery(params: Record<string, unknown>): string {
  const search = new URLSearchParams();
  for (const [k, v] of Object.entries(params)) {
    if (v !== undefined && v !== null) {
      search.append(k, String(v));
    }
  }
  const s = search.toString();
  return s ? `?${s}` : '';
}

export const AUTH_EXPIRED = 'auth:expired';

async function request<T>(method: string, path: string, body?: unknown): Promise<ApiResponse<T>> {
  const token = getToken();
  const headers: Record<string, string> = {};

  if (token) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const init: RequestInit = { method, headers };

  if (body !== undefined) {
    headers['Content-Type'] = 'application/json';
    init.body = JSON.stringify(body);
  }

  let res: Response;
  try {
    res = await fetch(`${BASE_URL}${path}`, init);
  } catch {
    throw new ApiError(0, '网络错误，请检查连接');
  }

  if (res.status === 401) {
    localStorage.removeItem('auth_token');
    window.dispatchEvent(new Event(AUTH_EXPIRED));
    throw new ApiError(401, '登录已过期，请重新登录');
  }

  const json: ApiResponse<T> = await res.json();

  if (!res.ok) {
    throw new ApiError(res.status, json.message || '请求失败');
  }

  return json;
}

export const api = {
  get: <T>(path: string, params?: Record<string, unknown>) =>
    request<T>('GET', params ? path + buildQuery(params) : path),
  post: <T>(path: string, body?: unknown) => request<T>('POST', path, body),
  put: <T>(path: string, body?: unknown) => request<T>('PUT', path, body),
  del: <T>(path: string) => request<T>('DELETE', path),
};

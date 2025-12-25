export const API_BASE = process.env.NEXT_PUBLIC_API_BASE ?? "http://localhost:8080";

export class ApiError extends Error {
  status: number;

  constructor(message: string, status: number) {
    super(message);
    this.name = "ApiError";
    this.status = status;
  }
}

function emitApiError(message: string, status: number) {
  if (typeof window === "undefined") {
    return;
  }
  window.dispatchEvent(
    new CustomEvent("api-error", {
      detail: { message, status },
    })
  );
}

async function readErrorMessage(response: Response): Promise<string> {
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    const data = await response.json().catch(() => null);
    if (data && typeof data === "object" && "message" in data && typeof data.message === "string") {
      return data.message;
    }
  } else {
    const text = await response.text().catch(() => "");
    if (text) {
      return text;
    }
  }
  return `Request failed: ${response.status}`;
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = typeof window !== "undefined" ? localStorage.getItem("token") : null;
  const headers = new Headers(options.headers);
  headers.set("Content-Type", "application/json");
  if (token) {
    headers.set("Authorization", `Bearer ${token}`);
  }
  let response: Response;
  try {
    response = await fetch(`${API_BASE}${path}`, {
      ...options,
      headers,
    });
  } catch (error) {
    const message = `Unable to reach API at ${API_BASE}. Is the backend running?`;
    emitApiError(message, 0);
    throw new ApiError(message, 0);
  }
  if (!response.ok) {
    const message = await readErrorMessage(response);
    emitApiError(message, response.status);
    throw new ApiError(message, response.status);
  }
  if (response.status === 204) {
    return undefined as T;
  }
  const contentType = response.headers.get("content-type") ?? "";
  if (contentType.includes("application/json")) {
    return response.json();
  }
  const text = await response.text();
  return text as unknown as T;
}

"use client";

import { useEffect, useState } from "react";

interface ApiErrorDetail {
  message: string;
  status: number;
}

export function GlobalErrorBanner() {
  const [error, setError] = useState<ApiErrorDetail | null>(null);

  useEffect(() => {
    const handler = (event: Event) => {
      const detail = (event as CustomEvent<ApiErrorDetail>).detail;
      if (!detail || !detail.message) {
        return;
      }
      setError(detail);
    };
    window.addEventListener("api-error", handler as EventListener);
    return () => window.removeEventListener("api-error", handler as EventListener);
  }, []);

  useEffect(() => {
    if (!error) return;
    const timer = window.setTimeout(() => setError(null), 6000);
    return () => window.clearTimeout(timer);
  }, [error]);

  if (!error) {
    return null;
  }

  const message =
    error.status === 429
      ? "Search rate limit exceeded. Please wait a minute and try again."
      : error.message;

  return (
    <div className="mb-4 rounded-2xl border border-amber-400/60 bg-amber-500/10 px-4 py-3 text-sm text-amber-100">
      <div className="flex items-start justify-between gap-4">
        <span>{message}</span>
        <button
          type="button"
          onClick={() => setError(null)}
          className="text-xs uppercase tracking-[0.2em] text-amber-200/80"
        >
          Close
        </button>
      </div>
    </div>
  );
}

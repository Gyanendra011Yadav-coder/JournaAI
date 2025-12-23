"use client";

interface ErrorBannerProps {
  message?: string | null;
}

export function ErrorBanner({ message }: ErrorBannerProps) {
  if (!message) {
    return null;
  }

  return (
    <div className="rounded-2xl border border-rose-500/40 bg-rose-500/10 p-4 text-sm text-rose-100">
      {message}
    </div>
  );
}

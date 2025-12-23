"use client";

interface ErrorBannerProps {
  message?: string | null;
  tone?: "error" | "warning";
}

const tones = {
  error: "border-rose-500/60 bg-rose-500/10 text-rose-100",
  warning: "border-amber-400/60 bg-amber-500/10 text-amber-100",
};

export function ErrorBanner({ message, tone = "error" }: ErrorBannerProps) {
  if (!message) {
    return null;
  }

  return (
    <div className={`rounded-2xl border p-4 text-sm ${tones[tone]}`}>
      {message}
    </div>
  );
}

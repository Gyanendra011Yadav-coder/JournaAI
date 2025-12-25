"use client";

interface ErrorBannerProps {
  message?: string | null;
  tone?: "error" | "warning";
}

const tones = {
  error: "border-rose-500/40 bg-rose-50 text-rose-700",
  warning: "border-amber-400/50 bg-amber-50 text-amber-800",
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

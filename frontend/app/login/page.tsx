"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "../../lib/api";
import { ErrorBanner } from "../../components/ErrorBanner";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("admin@example.com");
  const [password, setPassword] = useState("password");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const handleSubmit = async () => {
    setError(null);
    const payload = { email, password };
    setSubmitting(true);
    try {
      const data = await apiFetch<{ token: string }>("/api/auth/login", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      localStorage.setItem("token", data.token);
      const profile = await apiFetch<{ defaultSidebarMode?: string }>("/api/me/profile");
      const defaultMode = profile.defaultSidebarMode === "SEARCH" ? "/search" : "/trending";
      router.push(defaultMode);
    } catch (error) {
      setError(error instanceof Error ? error.message : "Authentication failed.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-6rem)] items-center justify-center">
      <div className="grid w-full max-w-5xl gap-8 rounded-3xl border border-slate-800/80 bg-slate-900/60 p-8 shadow-[0_0_0_1px_rgba(59,130,246,0.1)] lg:grid-cols-[1.1fr_0.9fr]">
        <div className="relative overflow-hidden rounded-2xl border border-slate-800/80 bg-gradient-to-br from-slate-950 via-slate-900 to-slate-950 p-8">
          <div className="absolute -top-10 right-0 h-40 w-40 rounded-full bg-cyan-500/20 blur-3xl" />
          <div className="absolute bottom-0 left-10 h-40 w-40 rounded-full bg-indigo-500/20 blur-3xl" />
          <div className="relative space-y-6">
            <div className="text-xs uppercase tracking-[0.3em] text-cyan-300/80">Journa AI</div>
            <h1 className="text-3xl font-semibold">PR News & Outreach</h1>
            <p className="text-slate-300">
              Cache-first newsroom intelligence and outreach workflows that keep your team aligned.
            </p>
            <div className="space-y-3 text-sm text-slate-300">
              {[
                "Admin-configurable beats and query recipes.",
                "Cache-first refresh with stale-mode protection.",
                "Publish workflows with audit trails.",
              ].map((item) => (
                <div key={item} className="flex items-center gap-3">
                  <span className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-cyan-500/20 text-cyan-100">
                    ✓
                  </span>
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-950/80 p-8">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold">Welcome back</h2>
            <button
              type="button"
              onClick={() => router.push("/signup")}
              className="text-sm text-cyan-300 hover:underline"
            >
              Create account
            </button>
          </div>
          <p className="text-slate-400 mt-2">
            Sign in to review your cached coverage.
          </p>
          <div className="mt-6 space-y-4">
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Email</label>
              <input
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 focus:border-cyan-500/60 focus:outline-none"
                value={email}
                onChange={(event) => setEmail(event.target.value)}
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Password</label>
              <input
                type="password"
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 focus:border-cyan-500/60 focus:outline-none"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
              />
            </div>
            <ErrorBanner message={error} />
            <button
              onClick={handleSubmit}
              disabled={submitting}
              className="w-full rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 py-3 font-semibold text-slate-900 shadow-lg shadow-cyan-500/20 transition hover:translate-y-[-1px] disabled:opacity-60"
            >
              <span className="inline-flex items-center gap-2">
                {submitting && (
                  <span className="h-3 w-3 animate-spin rounded-full border border-slate-700 border-t-transparent" />
                )}
                Continue
              </span>
            </button>
          </div>
          <div className="mt-6 rounded-xl border border-slate-800/80 bg-slate-900/60 p-4 text-sm text-slate-300">
            Primary workflow: search beats, refresh cache, publish coverage.
          </div>
        </div>
      </div>
    </div>
  );
}

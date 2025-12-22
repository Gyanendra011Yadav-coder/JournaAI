"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "../../lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("admin@example.com");
  const [password, setPassword] = useState("password");
  const [mode, setMode] = useState<"login" | "register">("register");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    setError(null);
    const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
    const payload = { email, password };
    const response = await fetch(`${API_BASE}${endpoint}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(payload),
    });
    if (!response.ok) {
      setError("Authentication failed.");
      return;
    }
    const data = await response.json();
    localStorage.setItem("token", data.token);
    router.push("/dashboard");
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
                "Curated beat coverage from compliant sources.",
                "Journalist discovery with seeded data.",
                "Audit trails for every search and send.",
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
            <div className="flex rounded-full border border-slate-800/80 bg-slate-900/60 p-1 text-sm">
              <button
                className={`px-4 py-1.5 rounded-full transition ${mode === "register" ? "bg-cyan-500 text-slate-900" : "text-slate-300"}`}
                onClick={() => setMode("register")}
              >
                Register
              </button>
              <button
                className={`px-4 py-1.5 rounded-full transition ${mode === "login" ? "bg-cyan-500 text-slate-900" : "text-slate-300"}`}
                onClick={() => setMode("login")}
              >
                Login
              </button>
            </div>
          </div>
          <p className="text-slate-400 mt-2">
            {mode === "login" ? "Sign in to review your cached coverage." : "Create an account and start tracking beats."}
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
            {error && <p className="text-red-400 text-sm">{error}</p>}
            <button
              onClick={handleSubmit}
              className="w-full rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 py-3 font-semibold text-slate-900 shadow-lg shadow-cyan-500/20 transition hover:translate-y-[-1px]"
            >
              Continue
            </button>
          </div>
          <div className="mt-6 rounded-xl border border-slate-800/80 bg-slate-900/60 p-4 text-sm text-slate-300">
            Primary workflow: search beats, refresh cache, find journalists, compose outreach.
          </div>
        </div>
      </div>
    </div>
  );
}

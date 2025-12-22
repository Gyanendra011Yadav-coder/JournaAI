"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { API_BASE } from "../../lib/api";

export default function LoginPage() {
  const router = useRouter();
  const [email, setEmail] = useState("admin@example.com");
  const [password, setPassword] = useState("password");
  const [workspace, setWorkspace] = useState("Agency Workspace");
  const [mode, setMode] = useState<"login" | "register">("register");
  const [error, setError] = useState<string | null>(null);

  const handleSubmit = async () => {
    setError(null);
    const endpoint = mode === "login" ? "/api/auth/login" : "/api/auth/register";
    const payload = mode === "login" ? { email, password } : { email, password, workspaceName: workspace };
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
    <div className="max-w-lg mx-auto mt-20 bg-slate-900 border border-slate-800 p-8 rounded-xl">
      <h1 className="text-2xl font-semibold mb-4">PR Control Tower</h1>
      <p className="text-slate-400 mb-6">Sign in to streamline news tracking and outreach.</p>
      <div className="flex gap-2 mb-4">
        <button
          className={`px-3 py-1 rounded-lg text-sm ${mode === "register" ? "bg-cyan-500 text-slate-900" : "bg-slate-800"}`}
          onClick={() => setMode("register")}
        >
          Register
        </button>
        <button
          className={`px-3 py-1 rounded-lg text-sm ${mode === "login" ? "bg-cyan-500 text-slate-900" : "bg-slate-800"}`}
          onClick={() => setMode("login")}
        >
          Login
        </button>
      </div>
      <div className="space-y-4">
        {mode === "register" && (
          <div>
            <label className="text-sm text-slate-300">Workspace name</label>
            <input
              className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 p-2"
              value={workspace}
              onChange={(event) => setWorkspace(event.target.value)}
            />
          </div>
        )}
        <div>
          <label className="text-sm text-slate-300">Email</label>
          <input
            className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 p-2"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>
        <div>
          <label className="text-sm text-slate-300">Password</label>
          <input
            type="password"
            className="mt-1 w-full rounded-lg bg-slate-950 border border-slate-700 p-2"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
          />
        </div>
        {error && <p className="text-red-400 text-sm">{error}</p>}
        <button
          onClick={handleSubmit}
          className="w-full bg-cyan-500 text-slate-900 font-semibold py-2 rounded-lg"
        >
          Continue
        </button>
      </div>
    </div>
  );
}

"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";

interface IntegrationStatus {
  gnewsEnabled: boolean;
  rssFeedCount: number;
  circuitStatus: Record<string, string>;
}

export default function IntegrationsPage() {
  const [status, setStatus] = useState<IntegrationStatus | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<IntegrationStatus>("/api/settings/integrations")
      .then((data) => {
        setStatus(data);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unable to load integration status.");
      });
  }, []);

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">Integrations</h1>
        <p className="text-slate-400">Backend-managed providers with cache-first ingestion.</p>
      </header>
      {error && (
        <div className="rounded-2xl border border-amber-400/60 bg-amber-500/10 p-4 text-amber-100 text-sm">
          {error}
        </div>
      )}
      <div className="grid md:grid-cols-2 gap-4">
        <div className="border border-slate-800 rounded-xl p-4 bg-slate-900">
          <h2 className="font-semibold">GNews Provider</h2>
          <p className="text-sm text-slate-400">Env var: GNEWS_API_KEY</p>
          <span className={status?.gnewsEnabled ? "text-xs text-emerald-400" : "text-xs text-amber-300"}>
            {status?.gnewsEnabled ? "Enabled" : "Missing API key"}
          </span>
        </div>
        <div className="border border-slate-800 rounded-xl p-4 bg-slate-900">
          <h2 className="font-semibold">RSS Fallback</h2>
          <p className="text-sm text-slate-400">Curated feeds by beat</p>
          <span className="text-xs text-emerald-400">{status?.rssFeedCount ?? 0} feeds configured</span>
        </div>
        <div className="border border-slate-800 rounded-xl p-4 bg-slate-900">
          <h2 className="font-semibold">Email Provider</h2>
          <p className="text-sm text-slate-400">MockEmailProvider (local)</p>
          <span className="text-xs text-emerald-400">Mock enabled</span>
        </div>
        <div className="border border-slate-800 rounded-xl p-4 bg-slate-900">
          <h2 className="font-semibold">Circuit Breakers</h2>
          {status?.circuitStatus && Object.keys(status.circuitStatus).length > 0 ? (
            <ul className="text-xs text-amber-300 space-y-1 mt-2">
              {Object.entries(status.circuitStatus).map(([key, value]) => (
                <li key={key}>{key}: {value}</li>
              ))}
            </ul>
          ) : (
            <p className="text-xs text-emerald-400 mt-2">All clear</p>
          )}
        </div>
      </div>
    </div>
  );
}

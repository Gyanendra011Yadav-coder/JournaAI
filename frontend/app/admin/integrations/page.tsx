"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";

interface IntegrationSettings {
  providerType: string;
  enabled: boolean;
  configured: boolean;
  defaultLang: string;
  defaultCountry: string;
  refreshIntervalMinutes: number;
  ttlMinutes: number;
  maxPerRequest: number;
  updatedAt?: string;
  updatedBy?: string;
}

export default function AdminIntegrationsPage() {
  const [settings, setSettings] = useState<IntegrationSettings | null>(null);
  const [apiKey, setApiKey] = useState("");
  const [status, setStatus] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<IntegrationSettings>("/api/admin/integrations/gnews").then(setSettings);
  }, []);

  const handleSaveSettings = async () => {
    if (!settings) return;
    const response = await apiFetch<IntegrationSettings>("/api/admin/integrations/gnews", {
      method: "PUT",
      body: JSON.stringify({
        enabled: settings.enabled,
        defaultLang: settings.defaultLang,
        defaultCountry: settings.defaultCountry,
        refreshIntervalMinutes: settings.refreshIntervalMinutes,
        ttlMinutes: settings.ttlMinutes,
        maxPerRequest: settings.maxPerRequest,
      }),
    });
    setSettings(response);
    setStatus("Settings updated.");
  };

  const handleUpdateKey = async () => {
    if (!apiKey) return;
    const response = await apiFetch<IntegrationSettings>("/api/admin/integrations/gnews/key", {
      method: "POST",
      body: JSON.stringify({ apiKey }),
    });
    setSettings(response);
    setApiKey("");
    setStatus("API key updated.");
  };

  if (!settings) {
    return <div>Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">GNews Integration</h1>
        <p className="text-slate-400">
          Manage API credentials, defaults, and refresh cadence. API keys are never displayed.
        </p>
      </header>

      {status && <div className="rounded-2xl border border-emerald-500/30 bg-emerald-500/10 p-4 text-emerald-100">{status}</div>}

      <section className="grid gap-4 md:grid-cols-2">
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <h2 className="text-lg font-semibold">Connection</h2>
          <p className="text-sm text-slate-400">
            Status: {settings.configured ? "Configured" : "Missing key"}
          </p>
          <label className="text-xs uppercase tracking-[0.2em] text-slate-400">New API key</label>
          <input
            type="password"
            value={apiKey}
            onChange={(event) => setApiKey(event.target.value)}
            placeholder="Enter new key"
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          />
          <button
            onClick={handleUpdateKey}
            className="rounded-xl bg-emerald-500 text-slate-900 px-4 py-2 font-semibold"
          >
            Update key
          </button>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <h2 className="text-lg font-semibold">Defaults</h2>
          <div className="grid gap-3 md:grid-cols-2">
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Default lang</label>
              <input
                value={settings.defaultLang}
                onChange={(event) => setSettings({ ...settings, defaultLang: event.target.value })}
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Default country</label>
              <input
                value={settings.defaultCountry}
                onChange={(event) => setSettings({ ...settings, defaultCountry: event.target.value })}
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Refresh interval (min)</label>
              <input
                type="number"
                value={settings.refreshIntervalMinutes}
                onChange={(event) => setSettings({ ...settings, refreshIntervalMinutes: Number(event.target.value) })}
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">TTL (min)</label>
              <input
                type="number"
                value={settings.ttlMinutes}
                onChange={(event) => setSettings({ ...settings, ttlMinutes: Number(event.target.value) })}
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Max per request</label>
              <input
                type="number"
                value={settings.maxPerRequest}
                onChange={(event) => setSettings({ ...settings, maxPerRequest: Number(event.target.value) })}
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              />
            </div>
            <div className="flex items-center gap-3 pt-6">
              <input
                type="checkbox"
                checked={settings.enabled}
                onChange={(event) => setSettings({ ...settings, enabled: event.target.checked })}
              />
              <span className="text-sm text-slate-300">Enabled</span>
            </div>
          </div>
          <button
            onClick={handleSaveSettings}
            className="rounded-xl bg-cyan-500 text-slate-900 px-4 py-2 font-semibold"
          >
            Save settings
          </button>
          {settings.updatedAt && (
            <p className="text-xs text-slate-500">
              Updated {new Date(settings.updatedAt).toLocaleString()} by {settings.updatedBy ?? "system"}
            </p>
          )}
        </div>
      </section>
    </div>
  );
}

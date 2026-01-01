"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "../../../lib/api";
import { getCountryOptions, getLanguageOptions } from "../../../lib/locale";

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

type AuthType = "BEARER" | "API_KEY_HEADER" | "QUERY_PARAM";

interface LlmProvider {
  id: number;
  name: string;
  enabled: boolean;
  configured: boolean;
  baseUrl: string;
  authType: AuthType;
  authHeaderName?: string | null;
  authQueryParamName?: string | null;
  model?: string | null;
  requestTemplateJsonb: string;
  responseJsonpath: string;
  timeoutMs: number;
  retryPolicyJsonb?: string | null;
  updatedAt?: string;
  updatedBy?: string;
}

const authOptions: AuthType[] = ["BEARER", "API_KEY_HEADER", "QUERY_PARAM"];

const normalizeJson = (value: string | null | undefined) => {
  if (!value) return "";
  try {
    return JSON.stringify(JSON.parse(value), null, 2);
  } catch {
    return value;
  }
};

export default function AdminIntegrationsPage() {
  const [settings, setSettings] = useState<IntegrationSettings | null>(null);
  const [apiKey, setApiKey] = useState("");
  const [gnewsStatus, setGnewsStatus] = useState<string | null>(null);
  const [gnewsError, setGnewsError] = useState<string | null>(null);
  const [savingKey, setSavingKey] = useState(false);
  const [savingSettings, setSavingSettings] = useState(false);
  const countryOptions = useMemo(() => getCountryOptions(), []);
  const languageOptions = useMemo(() => getLanguageOptions(), []);
  const isGnewsLoading = !settings && !gnewsError;

  useEffect(() => {
    apiFetch<IntegrationSettings>("/api/admin/integrations/gnews")
      .then((data) => {
        setSettings(data);
        setGnewsError(null);
      })
      .catch((err) =>
        setGnewsError(err instanceof Error ? err.message : "Unable to load integration settings."),
      );
  }, []);

  const handleSaveSettings = async () => {
    if (!settings) return;
    setSavingSettings(true);
    setGnewsError(null);
    try {
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
      setGnewsStatus("Settings updated.");
    } catch (err) {
      setGnewsError(err instanceof Error ? err.message : "Unable to save settings.");
    } finally {
      setSavingSettings(false);
    }
  };

  const handleUpdateKey = async () => {
    if (!apiKey) return;
    setSavingKey(true);
    setGnewsError(null);
    try {
      const response = await apiFetch<IntegrationSettings>("/api/admin/integrations/gnews/key", {
        method: "POST",
        body: JSON.stringify({ apiKey }),
      });
      setSettings(response);
      setApiKey("");
      setGnewsStatus("API key updated.");
    } catch (err) {
      setGnewsError(err instanceof Error ? err.message : "Unable to update API key.");
    } finally {
      setSavingKey(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Admin</p>
        <h1 className="text-3xl font-semibold">Integrations & AI</h1>
        <p className="text-slate-600">Configure data sources and AI providers in one place.</p>
      </header>

      {gnewsError && (
        <div className="rounded-2xl border border-amber-300/70 bg-amber-50 p-4 text-amber-800">
          {gnewsError}
        </div>
      )}
      {gnewsStatus && (
        <div className="rounded-2xl border border-emerald-300/70 bg-emerald-50 p-4 text-emerald-700">
          {gnewsStatus}
        </div>
      )}

      <section className="grid gap-4 md:grid-cols-2">
        <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 space-y-4">
          <h2 className="text-lg font-semibold">GNews Connection</h2>
          {isGnewsLoading ? (
            <p className="text-sm text-slate-600">Loading integration settings...</p>
          ) : (
            <>
              <p className="text-sm text-slate-600">
                Status: {settings?.configured ? "Configured" : "Missing key"}
              </p>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-600">New API key</label>
              <input
                type="password"
                value={apiKey}
                onChange={(event) => setApiKey(event.target.value)}
                placeholder="Enter new key"
                className="rounded-xl bg-white/80 border border-slate-200 p-3"
              />
              <button
                onClick={handleUpdateKey}
                disabled={savingKey || !settings}
                className="rounded-xl border border-emerald-300 bg-emerald-50 px-4 py-2 font-semibold text-emerald-700 disabled:opacity-60"
              >
                <span className="inline-flex items-center gap-2">
                  {savingKey && (
                    <span className="h-3 w-3 animate-spin rounded-full border border-emerald-600 border-t-transparent" />
                  )}
                  Update key
                </span>
              </button>
            </>
          )}
        </div>
        <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 space-y-4">
          <h2 className="text-lg font-semibold">GNews Defaults</h2>
          {settings ? (
            <>
              <div className="grid gap-3 md:grid-cols-2">
                <div>
                  <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Default lang</label>
                  <select
                    value={settings.defaultLang}
                    onChange={(event) => setSettings({ ...settings, defaultLang: event.target.value })}
                    className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  >
                    {languageOptions.map((option) => (
                      <option key={option.code} value={option.code}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Default country</label>
                  <select
                    value={settings.defaultCountry}
                    onChange={(event) => setSettings({ ...settings, defaultCountry: event.target.value })}
                    className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  >
                    {countryOptions.map((option) => (
                      <option key={option.code} value={option.code}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Refresh interval (min)</label>
                  <input
                    type="number"
                    value={settings.refreshIntervalMinutes}
                    onChange={(event) =>
                      setSettings({ ...settings, refreshIntervalMinutes: Number(event.target.value) })
                    }
                    className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  />
                </div>
                <div>
                  <label className="text-xs uppercase tracking-[0.2em] text-slate-600">TTL (min)</label>
                  <input
                    type="number"
                    value={settings.ttlMinutes}
                    onChange={(event) => setSettings({ ...settings, ttlMinutes: Number(event.target.value) })}
                    className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  />
                </div>
                <div>
                  <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Max per request</label>
                  <input
                    type="number"
                    value={settings.maxPerRequest}
                    onChange={(event) => setSettings({ ...settings, maxPerRequest: Number(event.target.value) })}
                    className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  />
                </div>
                <div className="flex items-center gap-3 pt-6">
                  <input
                    type="checkbox"
                    checked={settings.enabled}
                    onChange={(event) => setSettings({ ...settings, enabled: event.target.checked })}
                  />
                  <span className="text-sm text-slate-600">Enabled</span>
                </div>
              </div>
              <button
                onClick={handleSaveSettings}
                disabled={savingSettings}
                className="rounded-xl border border-cyan-300 bg-cyan-50 px-4 py-2 font-semibold text-cyan-700 disabled:opacity-60"
              >
                <span className="inline-flex items-center gap-2">
                  {savingSettings && (
                    <span className="h-3 w-3 animate-spin rounded-full border border-cyan-600 border-t-transparent" />
                  )}
                  Save settings
                </span>
              </button>
              {settings.updatedAt && (
                <p className="text-xs text-slate-500">
                  Updated {new Date(settings.updatedAt).toLocaleString()} by {settings.updatedBy ?? "system"}
                </p>
              )}
            </>
          ) : (
            <p className="text-sm text-slate-600">GNews defaults unavailable.</p>
          )}
        </div>
      </section>

      <LlmProvidersSection />
    </div>
  );
}

function LlmProvidersSection() {
  const [providers, setProviders] = useState<LlmProvider[]>([]);
  const [keys, setKeys] = useState<Record<number, string>>({});
  const [status, setStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [savingId, setSavingId] = useState<number | null>(null);
  const [savingKeyId, setSavingKeyId] = useState<number | null>(null);

  useEffect(() => {
    apiFetch<LlmProvider[]>("/api/admin/llm/providers")
      .then((data) => {
        setProviders(
          data.map((provider) => ({
            ...provider,
            requestTemplateJsonb: normalizeJson(provider.requestTemplateJsonb),
            retryPolicyJsonb: normalizeJson(provider.retryPolicyJsonb ?? ""),
          })),
        );
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load LLM providers."));
  }, []);

  const updateProvider = (id: number, updates: Partial<LlmProvider>) => {
    setProviders((prev) => prev.map((provider) => (provider.id === id ? { ...provider, ...updates } : provider)));
  };

  const handleSave = async (provider: LlmProvider) => {
    setSavingId(provider.id);
    setError(null);
    setStatus(null);
    try {
      const response = await apiFetch<LlmProvider>(`/api/admin/llm/providers/${provider.id}`, {
        method: "PUT",
        body: JSON.stringify({
          enabled: provider.enabled,
          baseUrl: provider.baseUrl,
          authType: provider.authType,
          authHeaderName: provider.authHeaderName ?? "",
          authQueryParamName: provider.authQueryParamName ?? "",
          model: provider.model ?? "",
          requestTemplateJsonb: provider.requestTemplateJsonb,
          responseJsonpath: provider.responseJsonpath,
          timeoutMs: provider.timeoutMs,
          retryPolicyJsonb: provider.retryPolicyJsonb ?? "",
        }),
      });
      updateProvider(provider.id, {
        ...response,
        requestTemplateJsonb: normalizeJson(response.requestTemplateJsonb),
        retryPolicyJsonb: normalizeJson(response.retryPolicyJsonb ?? ""),
      });
      setStatus(`${provider.name} updated.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to update provider.");
    } finally {
      setSavingId(null);
    }
  };

  const handleUpdateKey = async (provider: LlmProvider) => {
    const key = keys[provider.id];
    if (!key) return;
    setSavingKeyId(provider.id);
    setError(null);
    setStatus(null);
    try {
      const response = await apiFetch<LlmProvider>(`/api/admin/llm/providers/${provider.id}/key`, {
        method: "POST",
        body: JSON.stringify({ apiKey: key }),
      });
      updateProvider(provider.id, response);
      setKeys((prev) => ({ ...prev, [provider.id]: "" }));
      setStatus(`${provider.name} key updated.`);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to update key.");
    } finally {
      setSavingKeyId(null);
    }
  };

  if (!providers.length && !error) {
    return (
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6">
        <h2 className="text-lg font-semibold">LLM Providers</h2>
        <p className="text-sm text-slate-600">Loading providers...</p>
      </section>
    );
  }

  return (
    <section className="space-y-4">
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6">
        <h2 className="text-xl font-semibold">LLM Providers</h2>
        <p className="text-sm text-slate-600">
          Configure models, request templates, and rotation keys without leaving the integrations hub.
        </p>
      </div>

      {error && (
        <div className="rounded-2xl border border-amber-300/70 bg-amber-50 p-4 text-amber-800">{error}</div>
      )}
      {status && (
        <div className="rounded-2xl border border-emerald-300/70 bg-emerald-50 p-4 text-emerald-700">
          {status}
        </div>
      )}

      <div className="space-y-6">
        {providers.map((provider) => (
          <div key={provider.id} className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 space-y-5">
            <div className="flex flex-wrap items-center justify-between gap-4">
              <div>
                <h3 className="text-lg font-semibold">{provider.name}</h3>
                <p className="text-sm text-slate-600">
                  Status: {provider.configured ? "Configured" : "Missing key"}
                </p>
              </div>
              <label className="flex items-center gap-2 text-sm text-slate-600">
                <input
                  type="checkbox"
                  checked={provider.enabled}
                  onChange={(event) => updateProvider(provider.id, { enabled: event.target.checked })}
                />
                Enabled
              </label>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Base URL</label>
                <input
                  value={provider.baseUrl}
                  onChange={(event) => updateProvider(provider.id, { baseUrl: event.target.value })}
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Model</label>
                <input
                  value={provider.model ?? ""}
                  onChange={(event) => updateProvider(provider.id, { model: event.target.value })}
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Auth type</label>
                <select
                  value={provider.authType}
                  onChange={(event) => updateProvider(provider.id, { authType: event.target.value as AuthType })}
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                >
                  {authOptions.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Timeout (ms)</label>
                <input
                  type="number"
                  value={provider.timeoutMs}
                  onChange={(event) => updateProvider(provider.id, { timeoutMs: Number(event.target.value) })}
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Auth header name</label>
                <input
                  value={provider.authHeaderName ?? ""}
                  onChange={(event) => updateProvider(provider.id, { authHeaderName: event.target.value })}
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Auth query param name</label>
                <input
                  value={provider.authQueryParamName ?? ""}
                  onChange={(event) => updateProvider(provider.id, { authQueryParamName: event.target.value })}
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Response JSONPath</label>
                <input
                  value={provider.responseJsonpath}
                  onChange={(event) => updateProvider(provider.id, { responseJsonpath: event.target.value })}
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                />
              </div>
            </div>

            <div className="grid gap-4 md:grid-cols-2">
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Request template (JSON)</label>
                <textarea
                  value={provider.requestTemplateJsonb}
                  onChange={(event) => updateProvider(provider.id, { requestTemplateJsonb: event.target.value })}
                  className="mt-2 h-40 w-full rounded-xl bg-white/80 border border-slate-200 p-3 font-mono text-xs"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Retry policy (JSON)</label>
                <textarea
                  value={provider.retryPolicyJsonb ?? ""}
                  onChange={(event) => updateProvider(provider.id, { retryPolicyJsonb: event.target.value })}
                  className="mt-2 h-40 w-full rounded-xl bg-white/80 border border-slate-200 p-3 font-mono text-xs"
                />
              </div>
            </div>

            <div className="flex flex-wrap items-center gap-3">
              <input
                type="password"
                value={keys[provider.id] ?? ""}
                onChange={(event) => setKeys((prev) => ({ ...prev, [provider.id]: event.target.value }))}
                placeholder="Enter new API key"
                className="flex-1 rounded-xl bg-white/80 border border-slate-200 p-3"
              />
              <button
                onClick={() => handleUpdateKey(provider)}
                disabled={savingKeyId === provider.id}
                className="rounded-xl border border-emerald-300 bg-emerald-50 px-4 py-2 font-semibold text-emerald-700 disabled:opacity-60"
              >
                <span className="inline-flex items-center gap-2">
                  {savingKeyId === provider.id && (
                    <span className="h-3 w-3 animate-spin rounded-full border border-emerald-600 border-t-transparent" />
                  )}
                  Update key
                </span>
              </button>
              <button
                onClick={() => handleSave(provider)}
                disabled={savingId === provider.id}
                className="rounded-xl border border-cyan-300 bg-cyan-50 px-4 py-2 font-semibold text-cyan-700 disabled:opacity-60"
              >
                <span className="inline-flex items-center gap-2">
                  {savingId === provider.id && (
                    <span className="h-3 w-3 animate-spin rounded-full border border-cyan-600 border-t-transparent" />
                  )}
                  Save settings
                </span>
              </button>
            </div>

            {provider.updatedAt && (
              <p className="text-xs text-slate-500">
                Updated {new Date(provider.updatedAt).toLocaleString()} by {provider.updatedBy ?? "system"}
              </p>
            )}
          </div>
        ))}
      </div>
    </section>
  );
}

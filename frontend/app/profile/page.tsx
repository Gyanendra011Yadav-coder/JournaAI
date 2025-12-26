"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "../../lib/api";
import { ErrorBanner } from "../../components/ErrorBanner";
import { getCountryOptions, getLanguageOptions } from "../../lib/locale";

interface Beat {
  id: number;
  name: string;
}

interface Profile {
  preferredCountries: string[];
  preferredLangs: string[];
  beatIds: number[];
  clientKeywords: string[];
  excludeKeywords: string[];
  defaultSidebarMode?: string;
  clientLensRatio: number;
  trendingLocalRatio: number;
}

interface Client {
  id: number;
  displayName: string;
  shortName?: string | null;
  aliases: string[];
}

export default function ProfilePage() {
  const [profile, setProfile] = useState<Profile | null>(null);
  const [beats, setBeats] = useState<Beat[]>([]);
  const [clients, setClients] = useState<Client[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [clientName, setClientName] = useState("");
  const [clientAliases, setClientAliases] = useState("");
  const [savingProfile, setSavingProfile] = useState(false);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);
  const [saveSuccess, setSaveSuccess] = useState(false);
  const [addingClient, setAddingClient] = useState(false);
  const [clientBeatIds, setClientBeatIds] = useState<number[]>([]);
  const countryOptions = useMemo(() => getCountryOptions(), []);
  const languageOptions = useMemo(() => getLanguageOptions(), []);

  useEffect(() => {
    const load = async () => {
      try {
        const [profileData, beatsData, clientsData] = await Promise.all([
          apiFetch<Profile>("/api/me/profile"),
          apiFetch<Beat[]>("/api/beats"),
          apiFetch<Client[]>("/api/me/clients"),
        ]);
        setProfile(profileData);
        setBeats(beatsData);
        setClients(clientsData);
        setClientBeatIds(profileData.beatIds ?? []);
      } catch (err) {
        setError(err instanceof Error ? err.message : "Unable to load profile.");
      }
    };
    load();
  }, []);

  const toggleBeat = (beatId: number) => {
    if (!profile) return;
    const next = profile.beatIds.includes(beatId)
      ? profile.beatIds.filter((id) => id !== beatId)
      : [...profile.beatIds, beatId];
    setProfile({ ...profile, beatIds: next });
    setClientBeatIds(next);
  };

  const toggleClientBeat = (beatId: number) => {
    setClientBeatIds((prev) =>
      prev.includes(beatId) ? prev.filter((id) => id !== beatId) : [...prev, beatId]
    );
  };

  const saveProfile = async () => {
    if (!profile) return;
    setError(null);
    setSavingProfile(true);
    try {
      await apiFetch("/api/me/profile", {
        method: "PUT",
        body: JSON.stringify(profile),
      });
      setSaveNotice("Preferences saved.");
      setSaveSuccess(true);
      setTimeout(() => setSaveSuccess(false), 2000);
      setTimeout(() => setSaveNotice(null), 3500);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save profile.");
    } finally {
      setSavingProfile(false);
    }
  };

  const addClient = async () => {
    setError(null);
    setAddingClient(true);
    try {
      const response = await apiFetch<Client>("/api/me/clients", {
        method: "POST",
        body: JSON.stringify({
          displayName: clientName,
          aliases: clientAliases
            .split(",")
            .map((alias) => alias.trim())
            .filter(Boolean),
        }),
      });
      setClients((prev) => [...prev, response]);
      setClientName("");
      setClientAliases("");
      if (profile && clientBeatIds.length > 0) {
        const nextBeatIds = Array.from(new Set([...profile.beatIds, ...clientBeatIds]));
        const nextProfile = { ...profile, beatIds: nextBeatIds };
        setProfile(nextProfile);
        await apiFetch("/api/me/profile", {
          method: "PUT",
          body: JSON.stringify(nextProfile),
        });
      }
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to add client.");
    } finally {
      setAddingClient(false);
    }
  };

  if (!profile) {
    return <div>Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Profile</p>
        <h1 className="text-3xl font-semibold">Preferences & personalization</h1>
        <p className="text-slate-600">Update your countries, languages, beats, and client lens.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Country</label>
              <select
                className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                value={profile.preferredCountries?.[0] ?? ""}
                onChange={(event) =>
                  setProfile({ ...profile, preferredCountries: [event.target.value] })
                }
              >
                {countryOptions.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Language</label>
              <select
                className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                value={profile.preferredLangs?.[0] ?? ""}
                onChange={(event) => setProfile({ ...profile, preferredLangs: [event.target.value] })}
              >
                {languageOptions.map((option) => (
                  <option key={option.code} value={option.code}>
                    {option.label}
                  </option>
                ))}
              </select>
            </div>
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Beats</label>
            <div className="mt-3 flex flex-wrap gap-2">
              {beats.map((beat) => (
                <button
                  key={beat.id}
                  type="button"
                  onClick={() => toggleBeat(beat.id)}
                  className={`rounded-full border px-3 py-1 text-xs transition ${
                    profile.beatIds.includes(beat.id)
                      ? "border-cyan-300/70 bg-cyan-50 text-cyan-700"
                      : "border-slate-200 text-slate-600 hover:border-cyan-200 hover:text-slate-900"
                  }`}
                >
                  {beat.name}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Client keywords</label>
            <input
              className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
              value={profile.clientKeywords?.join(", ") ?? ""}
              onChange={(event) =>
                setProfile({ ...profile, clientKeywords: event.target.value.split(",").map((v) => v.trim()) })
              }
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Exclude keywords</label>
            <input
              className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
              value={profile.excludeKeywords?.join(", ") ?? ""}
              onChange={(event) =>
                setProfile({ ...profile, excludeKeywords: event.target.value.split(",").map((v) => v.trim()) })
              }
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Default view</label>
            <select
              value={profile.defaultSidebarMode ?? "TRENDING"}
              onChange={(event) => setProfile({ ...profile, defaultSidebarMode: event.target.value })}
              className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
            >
              <option value="TRENDING">Trending</option>
              <option value="SEARCH">Search</option>
            </select>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Client lens ratio</label>
              <input
                type="number"
                className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                value={profile.clientLensRatio ?? 40}
                onChange={(event) => setProfile({ ...profile, clientLensRatio: Number(event.target.value) })}
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Trending local ratio</label>
              <input
                type="number"
                className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                value={profile.trendingLocalRatio ?? 40}
                onChange={(event) => setProfile({ ...profile, trendingLocalRatio: Number(event.target.value) })}
              />
            </div>
          </div>
          {saveNotice && (
            <div className="rounded-2xl border border-emerald-200 bg-emerald-50 px-4 py-2 text-sm text-emerald-700">
              {saveNotice}
            </div>
          )}
          <button
            onClick={saveProfile}
            disabled={savingProfile}
            className="rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 px-4 py-2 text-sm font-semibold text-slate-900 disabled:opacity-60"
          >
            <span className="inline-flex items-center gap-2">
              {savingProfile && (
                <span className="h-3 w-3 animate-spin rounded-full border border-slate-200 border-t-transparent" />
              )}
              {savingProfile ? "Saving..." : saveSuccess ? "Saved ✓" : "Save preferences"}
            </span>
          </button>
        </div>
        <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 space-y-4">
          <h2 className="text-lg font-semibold">Clients & aliases</h2>
          <div className="space-y-2">
            {clients.length === 0 && <p className="text-sm text-slate-600">No clients yet.</p>}
            {clients.map((client) => (
              <div key={client.id} className="rounded-xl border border-slate-200/70 bg-white p-3">
                <p className="text-sm font-semibold">{client.displayName}</p>
                {client.aliases.length > 0 && (
                  <p className="text-xs text-slate-600">Aliases: {client.aliases.join(", ")}</p>
                )}
              </div>
            ))}
          </div>
          <div className="space-y-2">
            <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Add client</label>
            <input
              className="w-full rounded-xl bg-white/80 border border-slate-200 p-3"
              value={clientName}
              onChange={(event) => setClientName(event.target.value)}
              placeholder="Client name"
            />
            <input
              className="w-full rounded-xl bg-white/80 border border-slate-200 p-3"
              value={clientAliases}
              onChange={(event) => setClientAliases(event.target.value)}
              placeholder="Aliases (comma-separated)"
            />
            <div>
              <p className="text-xs uppercase tracking-[0.2em] text-slate-600">Client beats (optional)</p>
              <div className="mt-2 flex flex-wrap gap-2">
                {beats.map((beat) => (
                  <button
                    key={beat.id}
                    type="button"
                    onClick={() => toggleClientBeat(beat.id)}
                    className={`rounded-full border px-3 py-1 text-xs transition ${
                      clientBeatIds.includes(beat.id)
                        ? "border-cyan-300/70 bg-cyan-50 text-cyan-700"
                        : "border-slate-200 text-slate-600 hover:border-cyan-200 hover:text-slate-900"
                    }`}
                  >
                    {beat.name}
                  </button>
                ))}
              </div>
            </div>
            <button
              onClick={addClient}
              disabled={addingClient}
              className="rounded-xl border border-slate-200 px-4 py-2 text-sm text-slate-700 disabled:opacity-60"
            >
              <span className="inline-flex items-center gap-2">
                {addingClient && (
                  <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />
                )}
                Add client
              </span>
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

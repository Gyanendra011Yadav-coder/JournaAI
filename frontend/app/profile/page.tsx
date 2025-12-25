"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";
import { ErrorBanner } from "../../components/ErrorBanner";

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
  };

  const saveProfile = async () => {
    if (!profile) return;
    setError(null);
    try {
      await apiFetch("/api/me/profile", {
        method: "PUT",
        body: JSON.stringify(profile),
      });
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save profile.");
    }
  };

  const addClient = async () => {
    setError(null);
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
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to add client.");
    }
  };

  if (!profile) {
    return <div>Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Profile</p>
        <h1 className="text-2xl font-semibold">Preferences & personalization</h1>
        <p className="text-slate-400">Update your countries, languages, beats, and client lens.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Country</label>
              <input
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
                value={profile.preferredCountries?.[0] ?? ""}
                onChange={(event) =>
                  setProfile({ ...profile, preferredCountries: [event.target.value] })
                }
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Language</label>
              <input
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
                value={profile.preferredLangs?.[0] ?? ""}
                onChange={(event) => setProfile({ ...profile, preferredLangs: [event.target.value] })}
              />
            </div>
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Beats</label>
            <div className="mt-3 flex flex-wrap gap-2">
              {beats.map((beat) => (
                <button
                  key={beat.id}
                  type="button"
                  onClick={() => toggleBeat(beat.id)}
                  className={`rounded-full border px-3 py-1 text-xs ${
                    profile.beatIds.includes(beat.id)
                      ? "border-cyan-500/60 bg-cyan-500/10 text-cyan-100"
                      : "border-slate-700 text-slate-300"
                  }`}
                >
                  {beat.name}
                </button>
              ))}
            </div>
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Client keywords</label>
            <input
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              value={profile.clientKeywords?.join(", ") ?? ""}
              onChange={(event) =>
                setProfile({ ...profile, clientKeywords: event.target.value.split(",").map((v) => v.trim()) })
              }
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Exclude keywords</label>
            <input
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              value={profile.excludeKeywords?.join(", ") ?? ""}
              onChange={(event) =>
                setProfile({ ...profile, excludeKeywords: event.target.value.split(",").map((v) => v.trim()) })
              }
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Default view</label>
            <select
              value={profile.defaultSidebarMode ?? "TRENDING"}
              onChange={(event) => setProfile({ ...profile, defaultSidebarMode: event.target.value })}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            >
              <option value="TRENDING">Trending</option>
              <option value="SEARCH">Search</option>
            </select>
          </div>
          <div className="grid gap-4 md:grid-cols-2">
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Client lens ratio</label>
              <input
                type="number"
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
                value={profile.clientLensRatio ?? 40}
                onChange={(event) => setProfile({ ...profile, clientLensRatio: Number(event.target.value) })}
              />
            </div>
            <div>
              <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Trending local ratio</label>
              <input
                type="number"
                className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
                value={profile.trendingLocalRatio ?? 40}
                onChange={(event) => setProfile({ ...profile, trendingLocalRatio: Number(event.target.value) })}
              />
            </div>
          </div>
          <button
            onClick={saveProfile}
            className="rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 px-4 py-2 text-sm font-semibold text-slate-900"
          >
            Save preferences
          </button>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <h2 className="text-lg font-semibold">Clients & aliases</h2>
          <div className="space-y-2">
            {clients.length === 0 && <p className="text-sm text-slate-400">No clients yet.</p>}
            {clients.map((client) => (
              <div key={client.id} className="rounded-xl border border-slate-800/80 bg-slate-950/60 p-3">
                <p className="text-sm font-semibold">{client.displayName}</p>
                {client.aliases.length > 0 && (
                  <p className="text-xs text-slate-400">Aliases: {client.aliases.join(", ")}</p>
                )}
              </div>
            ))}
          </div>
          <div className="space-y-2">
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Add client</label>
            <input
              className="w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              value={clientName}
              onChange={(event) => setClientName(event.target.value)}
              placeholder="Client name"
            />
            <input
              className="w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              value={clientAliases}
              onChange={(event) => setClientAliases(event.target.value)}
              placeholder="Aliases (comma-separated)"
            />
            <button
              onClick={addClient}
              className="rounded-xl border border-slate-700 px-4 py-2 text-sm text-slate-200"
            >
              Add client
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

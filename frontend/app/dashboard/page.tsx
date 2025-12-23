"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";
interface Beat {
  id: number;
  name: string;
}

interface BeatStatus {
  id: number;
  name: string;
  lastRefreshedAt: string | null;
}

export default function DashboardPage() {
  const [beats, setBeats] = useState<BeatStatus[]>([]);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      try {
        const beatList = await apiFetch<Beat[]>("/api/beats");
        const statusList = await Promise.all(
          beatList.map(async (beat) => {
            const result = await apiFetch<{ lastRefreshedAt?: string }>(
              `/api/articles?beatId=${beat.id}&page=0&size=1`
            );
            return {
              id: beat.id,
              name: beat.name,
              lastRefreshedAt: result.lastRefreshedAt ?? null,
            };
          })
        );
        setBeats(statusList);
      } finally {
        setLoading(false);
      }
    };
    load();
  }, []);

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Dashboard</p>
            <h1 className="text-2xl font-semibold">Command your PR narrative</h1>
            <p className="text-slate-400">A clear snapshot of cached coverage by beat.</p>
          </div>
          <div className="rounded-xl border border-cyan-500/40 bg-cyan-500/10 px-4 py-2 text-sm text-cyan-100">
            {beats.length} beats tracked
          </div>
        </div>
      </header>
      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Beat refresh status</h2>
          <span className="text-xs uppercase tracking-[0.2em] text-slate-400">
            {loading ? "Loading..." : "Cache-first"}
          </span>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          {beats.map((beat) => (
            <div key={beat.id} className="rounded-xl border border-slate-800/80 bg-slate-950/60 p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold">{beat.name}</p>
                  <p className="text-xs text-slate-400">
                    Last refreshed{" "}
                    {beat.lastRefreshedAt ? new Date(beat.lastRefreshedAt).toLocaleString() : "never"}
                  </p>
                </div>
                <button
                  onClick={async () => {
                    await apiFetch(`/api/ingest/refresh?beatId=${beat.id}`, { method: "POST" });
                    const response = await apiFetch<{ lastRefreshedAt?: string }>(
                      `/api/articles?beatId=${beat.id}&page=0&size=1`
                    );
                    setBeats((prev) =>
                      prev.map((item) =>
                        item.id === beat.id
                          ? { ...item, lastRefreshedAt: response.lastRefreshedAt ?? null }
                          : item
                      )
                    );
                  }}
                  className="rounded-lg border border-cyan-500/60 bg-cyan-500/10 px-3 py-1.5 text-xs text-cyan-100"
                >
                  Refresh
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

"use client";

import { useEffect, useState } from "react";
import { BeatSelector } from "../../components/BeatSelector";
import { TimeframePicker } from "../../components/TimeframePicker";
import { ArticlesTable } from "../../components/ArticlesTable";
import { apiFetch } from "../../lib/api";

interface Article {
  id: number;
  headline: string;
  source: string;
  author: string;
  publishedAt: string;
  summary: string;
  beats: string[];
}

interface ArticleSearchResponse {
  items: Article[];
  total: number;
  page: number;
  size: number;
  lastRefreshedAt?: string;
}

interface Beat {
  id: number;
  name: string;
  slug: string;
}

interface RefreshResponse {
  status: string;
  staleCache: boolean;
  lastRefreshedAt?: string;
  message?: string;
}

export default function SearchPage() {
  const [beats, setBeats] = useState<Beat[]>([]);
  const [beat, setBeat] = useState<string>("");
  const [timeframe, setTimeframe] = useState("24h");
  const [customFrom, setCustomFrom] = useState<string>("");
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshStatus, setRefreshStatus] = useState<RefreshResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Beat[]>("/api/beats")
      .then((data) => {
        setBeats(data);
        if (data.length > 0) {
          setBeat(data[0].name);
        }
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unable to load beats.");
      });
  }, []);

  const handleSearch = async () => {
    if (!beat) return;
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams({
        beat,
        timeframe,
        page: "0",
        size: "20",
      });
      if (timeframe.toLowerCase() === "custom" && customFrom) {
        params.set("from", new Date(customFrom).toISOString());
      }
      const result = await apiFetch<ArticleSearchResponse>(`/api/articles?${params.toString()}`);
      setArticles(result.items);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load articles.");
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    if (!beat) return;
    setError(null);
    const payload: { beat: string; timeframe: string; from?: string } = { beat, timeframe };
    if (timeframe.toLowerCase() === "custom" && customFrom) {
      payload.from = new Date(customFrom).toISOString();
    }
    try {
      const response = await apiFetch<RefreshResponse>("/api/articles/refresh", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setRefreshStatus(response);
      await handleSearch();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to refresh articles.");
    }
  };

  const handleManualAdd = async (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();
    setError(null);
    const formData = new FormData(event.currentTarget);
    const payload = {
      beat,
      headline: String(formData.get("headline")),
      url: String(formData.get("url")),
      source: String(formData.get("source")),
      author: String(formData.get("author")),
      summary: String(formData.get("summary")),
    };
    try {
      await apiFetch("/api/articles/manual", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      event.currentTarget.reset();
      await handleSearch();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to add article.");
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Discovery</p>
        <h1 className="text-2xl font-semibold">News Search</h1>
        <p className="text-slate-400">Track cached news by beat and timeframe, then refresh on demand.</p>
      </header>
      {error && (
        <div className="rounded-2xl border border-rose-500/60 bg-rose-500/10 p-4 text-rose-100 text-sm">
          {error}
        </div>
      )}
      {refreshStatus?.staleCache && (
        <div className="rounded-2xl border border-amber-400/60 bg-amber-500/10 p-4 text-amber-100">
          <p className="text-sm font-semibold">Stale-cache mode</p>
          <p className="text-sm text-amber-200">{refreshStatus.message}</p>
        </div>
      )}
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4 shadow-[0_0_0_1px_rgba(59,130,246,0.1)]">
        <div>
          <p className="text-sm text-slate-400 mb-2">Select beat</p>
          <BeatSelector value={beat} beats={beats.map((item) => item.name)} onChange={setBeat} />
        </div>
        <div>
          <p className="text-sm text-slate-400 mb-2">Timeframe</p>
          <TimeframePicker value={timeframe} onChange={setTimeframe} />
          {timeframe.toLowerCase() === "custom" && (
            <input
              type="datetime-local"
              value={customFrom}
              onChange={(event) => setCustomFrom(event.target.value)}
              className="mt-3 rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 text-sm"
            />
          )}
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleSearch}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 text-slate-900 font-semibold shadow-lg shadow-cyan-500/20"
          >
            {loading ? "Searching..." : "Search"}
          </button>
          <button
            onClick={handleRefresh}
            className="px-4 py-2 rounded-xl border border-slate-700 text-slate-200"
          >
            Refresh cache
          </button>
        </div>
      </div>
      <ArticlesTable articles={articles} />
      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
        <h2 className="text-lg font-semibold">Add article URL (manual ingestion)</h2>
        <form className="grid gap-3 md:grid-cols-2" onSubmit={handleManualAdd}>
          <input name="headline" required placeholder="Headline" className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3" />
          <input name="url" required placeholder="Canonical URL" className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3" />
          <input name="source" placeholder="Source" className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3" />
          <input name="author" placeholder="Author" className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3" />
          <textarea name="summary" placeholder="Summary" className="md:col-span-2 rounded-xl bg-slate-900/60 border border-slate-700/80 p-3" />
          <button
            type="submit"
            className="md:col-span-2 px-4 py-2 rounded-xl bg-emerald-500 text-slate-900 font-semibold"
          >
            Add article
          </button>
        </form>
      </section>
    </div>
  );
}

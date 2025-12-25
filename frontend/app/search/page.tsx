"use client";

import { useEffect, useState } from "react";
import { BeatSelector } from "../../components/BeatSelector";
import { TimeframePicker } from "../../components/TimeframePicker";
import { ArticlesTable } from "../../components/ArticlesTable";
import { apiFetch } from "../../lib/api";
import { ErrorBanner } from "../../components/ErrorBanner";

interface Article {
  id: number;
  title: string;
  sourceName: string | null;
  publishedAtUtc: string | null;
  status: string;
  beatName: string;
}

interface ArticleSearchResponse {
  items: Article[];
  total: number;
  page: number;
  size: number;
  lastRefreshedAt?: string;
  staleCache?: boolean;
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
  const [beatId, setBeatId] = useState<number | null>(null);
  const [timeframe, setTimeframe] = useState("24h");
  const [customFrom, setCustomFrom] = useState<string>("");
  const [lens, setLens] = useState<"ALL" | "CLIENT" | "BEAT">("ALL");
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshStatus, setRefreshStatus] = useState<RefreshResponse | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = useState<string | null>(null);
  const [staleCache, setStaleCache] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const showOfflineHint = error?.includes("Unable to reach API");

  useEffect(() => {
    apiFetch<Beat[]>("/api/beats")
      .then((data) => {
        setBeats(data);
        if (data.length > 0) {
          setBeatId(data[0].id);
        }
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unable to load beats.");
      });
  }, []);

  const resolveFrom = () => {
    const now = new Date();
    if (timeframe === "7d") {
      return new Date(now.getTime() - 7 * 24 * 60 * 60 * 1000).toISOString();
    }
    if (timeframe === "30d") {
      return new Date(now.getTime() - 30 * 24 * 60 * 60 * 1000).toISOString();
    }
    if (timeframe === "24h") {
      return new Date(now.getTime() - 24 * 60 * 60 * 1000).toISOString();
    }
    return null;
  };

  const handleSearch = async () => {
    if (!beatId) return;
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set("beatId", String(beatId));
      params.set("mode", "SEARCH");
      params.set("lens", lens);
      params.set("page", "0");
      params.set("size", "20");
      const from = resolveFrom();
      if (from) {
        params.set("from", from);
      }
      if (timeframe.toLowerCase() === "custom" && customFrom) {
        params.set("from", new Date(customFrom).toISOString());
      }
      const result = await apiFetch<ArticleSearchResponse>(`/api/articles?${params.toString()}`);
      setArticles(result.items);
      setLastRefreshedAt(result.lastRefreshedAt ?? null);
      setStaleCache(Boolean(result.staleCache));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load articles.");
    } finally {
      setLoading(false);
    }
  };

  const handleRefresh = async () => {
    if (!beatId) return;
    setError(null);
    setRefreshing(true);
    try {
      if (lens === "ALL") {
        await apiFetch(`/api/ingest/refresh?mode=SEARCH&beatId=${beatId}&lensOrTrack=CLIENT`, { method: "POST" });
        const response = await apiFetch<RefreshResponse>(
          `/api/ingest/refresh?mode=SEARCH&beatId=${beatId}&lensOrTrack=BEAT`,
          { method: "POST" }
        );
        setRefreshStatus(response);
      } else {
        const response = await apiFetch<RefreshResponse>(
          `/api/ingest/refresh?mode=SEARCH&beatId=${beatId}&lensOrTrack=${lens}`,
          {
            method: "POST",
          }
        );
        setRefreshStatus(response);
      }
      await handleSearch();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to refresh cache.");
    } finally {
      setRefreshing(false);
    }
  };


  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Discovery</p>
        <h1 className="text-2xl font-semibold">News Search</h1>
        <p className="text-slate-400">Track cached news by beat and timeframe, then refresh on demand.</p>
      </header>
      <ErrorBanner message={error} />
      {(refreshStatus?.staleCache || staleCache) && (
        <div className="rounded-2xl border border-amber-400/60 bg-amber-500/10 p-4 text-amber-100">
          <p className="text-sm font-semibold">Stale-cache mode</p>
          <p className="text-sm text-amber-200">
            {refreshStatus?.message ?? "Serving cached results from the last successful refresh."}
          </p>
        </div>
      )}
      {lastRefreshedAt && (
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/40 p-4 text-sm text-slate-300">
          Last refreshed at {new Date(lastRefreshedAt).toLocaleString()}
        </div>
      )}
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4 shadow-[0_0_0_1px_rgba(59,130,246,0.1)]">
        <div>
          <p className="text-sm text-slate-400 mb-2">Select beat</p>
          <BeatSelector value={beatId} beats={beats} onChange={setBeatId} />
          {showOfflineHint && (
            <p className="mt-2 text-xs text-amber-300">
              Backend offline — start <span className="font-semibold">./gradlew bootRun</span> or set{" "}
              <span className="font-semibold">NEXT_PUBLIC_API_BASE</span>.
            </p>
          )}
        </div>
        <div>
          <p className="text-sm text-slate-400 mb-2">Timeframe</p>
          <TimeframePicker value={timeframe} onChange={setTimeframe} />
          {timeframe.toLowerCase() === "custom" && (
            <input
              type="datetime-local"
              value={customFrom}
              onChange={(event) => setCustomFrom(event.target.value)}
              className="mt-3 rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 text-sm text-slate-200"
              style={{ colorScheme: "dark" }}
            />
          )}
        </div>
        <div>
          <p className="text-sm text-slate-400 mb-2">Lens</p>
          <div className="flex flex-wrap gap-2">
            {(["ALL", "CLIENT", "BEAT"] as const).map((option) => (
              <button
                key={option}
                onClick={() => setLens(option)}
                className={`rounded-full border px-3 py-1 text-xs ${
                  lens === option
                    ? "border-cyan-500/60 bg-cyan-500/10 text-cyan-100"
                    : "border-slate-700 text-slate-300"
                }`}
              >
                {option === "ALL" ? "All" : option === "CLIENT" ? "Client-focused" : "Beat-only"}
              </button>
            ))}
          </div>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleSearch}
            disabled={loading || refreshing}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 text-slate-900 font-semibold shadow-lg shadow-cyan-500/20 disabled:opacity-60"
          >
            {loading ? "Searching..." : "Search"}
          </button>
          <button
            onClick={handleRefresh}
            disabled={refreshing || loading}
            className="px-4 py-2 rounded-xl border border-slate-700 text-slate-200 disabled:opacity-60"
          >
            <span className="inline-flex items-center gap-2">
              {refreshing && <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />}
              {refreshing ? "Refreshing..." : "Refresh cache"}
            </span>
          </button>
        </div>
      </div>
      <ArticlesTable articles={articles} />
    </div>
  );
}

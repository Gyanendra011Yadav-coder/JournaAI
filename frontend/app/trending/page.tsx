"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";
import { ArticlesTable } from "../../components/ArticlesTable";
import { ErrorBanner } from "../../components/ErrorBanner";

interface Article {
  id: number;
  title: string;
  sourceName: string | null;
  publishedAtUtc: string | null;
  status: string;
  beatName: string | null;
}

interface ArticleListResponse {
  items: Article[];
  total: number;
  page: number;
  size: number;
  lastRefreshedAt?: string;
  staleCache?: boolean;
}

interface RefreshResponse {
  status: string;
  staleCache: boolean;
  lastRefreshedAt?: string;
  message?: string;
}

const categories = [
  "general",
  "world",
  "nation",
  "business",
  "technology",
  "entertainment",
  "sports",
  "science",
  "health",
];

export default function TrendingPage() {
  const [view, setView] = useState<"MIX" | "LOCAL" | "GLOBAL">("MIX");
  const [category, setCategory] = useState("general");
  const [articles, setArticles] = useState<Article[]>([]);
  const [lastRefreshedAt, setLastRefreshedAt] = useState<string | null>(null);
  const [staleCache, setStaleCache] = useState(false);
  const [refreshStatus, setRefreshStatus] = useState<RefreshResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);

  const loadTrending = async () => {
    setError(null);
    const params = new URLSearchParams();
    params.set("mode", "TRENDING");
    params.set("lens", view);
    params.set("category", category);
    params.set("page", "0");
    params.set("size", "20");
    try {
      const result = await apiFetch<ArticleListResponse>(`/api/articles?${params.toString()}`);
      setArticles(result.items);
      setLastRefreshedAt(result.lastRefreshedAt ?? null);
      setStaleCache(Boolean(result.staleCache));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load trending headlines.");
    }
  };

  useEffect(() => {
    loadTrending();
  }, [view, category]);

  const handleRefresh = async () => {
    setError(null);
    setRefreshing(true);
    try {
      if (view === "MIX") {
        await apiFetch(`/api/ingest/refresh?mode=TRENDING&lensOrTrack=LOCAL&category=${category}`, { method: "POST" });
        const global = await apiFetch<RefreshResponse>(
          `/api/ingest/refresh?mode=TRENDING&lensOrTrack=GLOBAL&category=${category}`,
          { method: "POST" }
        );
        setRefreshStatus(global);
      } else {
        const response = await apiFetch<RefreshResponse>(
          `/api/ingest/refresh?mode=TRENDING&lensOrTrack=${view}&category=${category}`,
          { method: "POST" }
        );
        setRefreshStatus(response);
      }
      await loadTrending();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to refresh trending cache.");
    } finally {
      setRefreshing(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Trending</p>
        <h1 className="text-2xl font-semibold">Top headlines mix</h1>
        <p className="text-slate-400">
          40% local + 60% global headlines, filtered by your preferred language.
        </p>
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
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
        <div className="flex flex-wrap items-center gap-3">
          <div className="flex gap-2">
            {(["MIX", "LOCAL", "GLOBAL"] as const).map((option) => (
              <button
                key={option}
                onClick={() => setView(option)}
                className={`rounded-full border px-3 py-1 text-xs ${
                  view === option
                    ? "border-cyan-500/60 bg-cyan-500/10 text-cyan-100"
                    : "border-slate-700 text-slate-300"
                }`}
              >
                {option === "MIX" ? "Mix" : option === "LOCAL" ? "Local only" : "Global only"}
              </button>
            ))}
          </div>
          <select
            value={category}
            onChange={(event) => setCategory(event.target.value)}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 px-3 py-2 text-sm"
          >
            {categories.map((option) => (
              <option key={option} value={option}>
                {option}
              </option>
            ))}
          </select>
          <button
            onClick={handleRefresh}
            disabled={refreshing}
            className="rounded-xl border border-slate-700 px-4 py-2 text-sm text-slate-200 disabled:opacity-60"
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

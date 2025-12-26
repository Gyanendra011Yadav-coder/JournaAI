"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { BeatSelector } from "../../components/BeatSelector";
import { TimeframePicker } from "../../components/TimeframePicker";
import { ArticlesTable } from "../../components/ArticlesTable";
import { apiFetch } from "../../lib/api";
import { ErrorBanner } from "../../components/ErrorBanner";

interface Article {
  id: number;
  title: string;
  authorRaw?: string | null;
  journalistName?: string | null;
  journalistId?: number | null;
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

interface Profile {
  clientKeywords?: string[];
}

interface Client {
  id: number;
}

interface RefreshResponse {
  status: string;
  staleCache: boolean;
  lastRefreshedAt?: string;
  message?: string;
}

export default function SearchPage() {
  const searchParams = useSearchParams();
  const [beats, setBeats] = useState<Beat[]>([]);
  const [beatId, setBeatId] = useState<number | null>(null);
  const [timeframe, setTimeframe] = useState("24h");
  const [customFrom, setCustomFrom] = useState<string>("");
  const [lens, setLens] = useState<"ALL" | "CLIENT" | "BEAT">("ALL");
  const [pageSize, setPageSize] = useState(20);
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshStatus, setRefreshStatus] = useState<RefreshResponse | null>(null);
  const [lastRefreshedAt, setLastRefreshedAt] = useState<string | null>(null);
  const [staleCache, setStaleCache] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [hasClientTerms, setHasClientTerms] = useState(false);
  const [autoSearchReady, setAutoSearchReady] = useState(false);
  const [journalistFilterId, setJournalistFilterId] = useState<number | null>(null);
  const [journalistFilterName, setJournalistFilterName] = useState<string | null>(null);
  const showOfflineHint = error?.includes("Unable to reach API");

  useEffect(() => {
    let cancelled = false;
    const load = async () => {
      try {
        const data = await apiFetch<Beat[]>("/api/beats");
        if (cancelled) return;
        setBeats(data);
        setError(null);
      } catch (err) {
        if (cancelled) return;
        setError(err instanceof Error ? err.message : "Unable to load beats.");
      }
      try {
        const [profileData, clientsData] = await Promise.all([
          apiFetch<Profile>("/api/me/profile"),
          apiFetch<Client[]>("/api/me/clients"),
        ]);
        if (cancelled) return;
        const keywordCount = (profileData.clientKeywords ?? []).filter(Boolean).length;
        setHasClientTerms(keywordCount > 0 || clientsData.length > 0);
      } catch {
        if (cancelled) return;
        setHasClientTerms(false);
      }
      if (!cancelled) {
        setAutoSearchReady(true);
      }
    };
    load();
    return () => {
      cancelled = true;
    };
  }, []);

  useEffect(() => {
    const journalistIdParam = searchParams.get("journalistId");
    if (!journalistIdParam) {
      setJournalistFilterId(null);
      setJournalistFilterName(null);
      return;
    }
    const parsed = Number(journalistIdParam);
    if (Number.isFinite(parsed)) {
      setJournalistFilterId(parsed);
      setJournalistFilterName(searchParams.get("journalistName"));
      setBeatId(null);
      setLens("ALL");
    }
  }, [searchParams]);

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
    if (lens === "CLIENT" && !hasClientTerms) {
      setError("Add client keywords or clients to use the client-focused lens.");
      return;
    }
    setLoading(true);
    setError(null);
    try {
      const params = new URLSearchParams();
      params.set("mode", "SEARCH");
      params.set("lens", lens);
      params.set("page", "0");
      params.set("size", String(pageSize));
      if (beatId !== null) {
        params.set("beatId", String(beatId));
      }
      if (journalistFilterId !== null) {
        params.set("journalistId", String(journalistFilterId));
      }
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
    if (journalistFilterId !== null) {
      setError("Refresh is disabled when filtering by journalist.");
      return;
    }
    if (!beatId) {
      setError("Select a beat before refreshing the cache.");
      return;
    }
    if (lens === "CLIENT" && !hasClientTerms) {
      setError("Add client keywords or clients to use the client-focused lens.");
      setRefreshStatus({
        status: "SKIPPED",
        staleCache: true,
        message: "No client keywords configured.",
      });
      return;
    }
    setError(null);
    setRefreshing(true);
    try {
      if (lens === "ALL") {
        if (hasClientTerms) {
          await apiFetch(`/api/ingest/refresh?mode=SEARCH&beatId=${beatId}&lensOrTrack=CLIENT`, { method: "POST" });
        }
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

  useEffect(() => {
    if (!autoSearchReady) return;
    if (timeframe.toLowerCase() === "custom" && !customFrom) return;
    if (lens === "CLIENT" && !hasClientTerms) return;
    handleSearch();
  }, [autoSearchReady, beatId, timeframe, customFrom, lens, journalistFilterId, hasClientTerms, pageSize]);


  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Discovery</p>
        <h1 className="text-3xl font-semibold">News Search</h1>
        <p className="text-slate-600">Track cached news by beat and timeframe, then refresh on demand.</p>
      </header>
      <ErrorBanner message={error} />
      {(refreshStatus?.staleCache || staleCache) && (
        <div className="rounded-2xl border border-amber-300/70 bg-amber-50 p-4 text-amber-800">
          <p className="text-sm font-semibold">Stale-cache mode</p>
          <p className="text-sm text-amber-700">
            {refreshStatus?.message ?? "Serving cached results from the last successful refresh."}
          </p>
        </div>
      )}
      {lastRefreshedAt && (
        <div className="rounded-2xl border border-slate-200/70 bg-white/70 p-4 text-sm text-slate-600">
          Last refreshed at {new Date(lastRefreshedAt).toLocaleString()}
        </div>
      )}
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 space-y-5 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
        <div>
          <p className="text-sm text-slate-600 mb-2">Select beat</p>
          <BeatSelector
            value={beatId}
            beats={beats}
            onChange={(value) => {
              setBeatId(value);
              if (journalistFilterId !== null) {
                setJournalistFilterId(null);
                setJournalistFilterName(null);
              }
            }}
          />
          {journalistFilterId !== null && (
            <div className="mt-3 flex items-center gap-2 rounded-xl border border-cyan-200 bg-cyan-50 px-3 py-2 text-xs text-cyan-800">
              <span>
                Filtering by journalist: {journalistFilterName ?? `ID ${journalistFilterId}`}
              </span>
              <button
                type="button"
                onClick={() => {
                  setJournalistFilterId(null);
                  setJournalistFilterName(null);
                }}
                className="rounded-full border border-cyan-300/70 px-2 py-0.5 text-[10px] uppercase tracking-[0.15em]"
              >
                Clear
              </button>
            </div>
          )}
          {showOfflineHint && (
            <p className="mt-2 text-xs text-amber-700">
              Backend offline — start <span className="font-semibold">./gradlew bootRun</span> or set{" "}
              <span className="font-semibold">NEXT_PUBLIC_API_BASE</span>.
            </p>
          )}
        </div>
        <div>
          <p className="text-sm text-slate-600 mb-2">Timeframe</p>
          <TimeframePicker value={timeframe} onChange={setTimeframe} />
          {timeframe.toLowerCase() === "custom" && (
            <input
              type="datetime-local"
              value={customFrom}
              onChange={(event) => setCustomFrom(event.target.value)}
              className="mt-3 rounded-xl bg-white/80 border border-slate-200 p-3 text-sm text-slate-700"
            />
          )}
        </div>
        <div>
          <p className="text-sm text-slate-600 mb-2">Lens</p>
          <div className="flex flex-wrap gap-2">
            {(["ALL", "CLIENT", "BEAT"] as const).map((option) => (
              <button
                key={option}
                onClick={() => setLens(option)}
                disabled={journalistFilterId !== null || (option === "CLIENT" && !hasClientTerms)}
                className={`rounded-full border px-3 py-1 text-xs transition disabled:cursor-not-allowed disabled:opacity-60 ${
                  lens === option
                    ? "border-cyan-300/70 bg-cyan-50 text-cyan-700"
                    : "border-slate-200 text-slate-600 hover:border-cyan-200 hover:text-slate-900"
                }`}
              >
                {option === "ALL" ? "All" : option === "CLIENT" ? "Client-focused" : "Beat-only"}
              </button>
            ))}
          </div>
          {!hasClientTerms && (
            <p className="mt-2 text-xs text-slate-500">Add clients or keywords in Profile to enable client-focused.</p>
          )}
        </div>
        <div>
          <p className="text-sm text-slate-600 mb-2">Results per page</p>
          <select
            value={pageSize}
            onChange={(event) => setPageSize(Number(event.target.value))}
            className="rounded-xl border border-slate-200 bg-white/80 px-3 py-2 text-sm text-slate-700"
          >
            {[10, 20, 50].map((size) => (
              <option key={size} value={size}>
                {size}
              </option>
            ))}
          </select>
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={handleSearch}
            disabled={loading || refreshing}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 text-slate-900 font-semibold shadow-lg shadow-cyan-500/20 transition hover:-translate-y-0.5 hover:shadow-xl disabled:opacity-60"
          >
            {loading ? "Searching..." : "Search"}
          </button>
          <button
            onClick={handleRefresh}
            disabled={refreshing || loading}
            className="px-4 py-2 rounded-xl border border-slate-200 text-slate-700 transition hover:border-cyan-300 hover:bg-slate-50 disabled:opacity-60"
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

"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";
import { AuditTimeline } from "../../components/AuditTimeline";
import { ErrorBanner } from "../../components/ErrorBanner";
import Link from "next/link";
interface BeatStatus {
  id: number;
  name: string;
  lastRefreshedAt: string | null;
}

interface SavedArticle {
  articleId: number;
  pinned: boolean;
  article: {
    id: number;
    title: string;
    beatName: string | null;
    publishedAtUtc: string | null;
    status: string;
  };
}

interface MeResponse {
  email: string;
  role: string;
}

interface AuditResponse {
  id: number;
  action: string;
  entityType: string;
  entityId?: string;
  createdAt?: string;
  actorEmail?: string;
}

export default function DashboardPage() {
  const [beats, setBeats] = useState<BeatStatus[]>([]);
  const [savedArticles, setSavedArticles] = useState<SavedArticle[]>([]);
  const [auditEvents, setAuditEvents] = useState<AuditResponse[]>([]);
  const [loading, setLoading] = useState(false);
  const [refreshingBeat, setRefreshingBeat] = useState<number | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);

  useEffect(() => {
    const load = async () => {
      setLoading(true);
      setError(null);
      try {
        const me = await apiFetch<MeResponse>("/api/auth/me");
        setRole(me.role);
        const statusList = await apiFetch<BeatStatus[]>("/api/beats/status");
        setBeats(statusList);
        const saved = await apiFetch<SavedArticle[]>("/api/saved-articles");
        setSavedArticles(saved.sort((a, b) => Number(b.pinned) - Number(a.pinned)));
        if (me.role === "ADMIN") {
          const audit = await apiFetch<AuditResponse[]>("/api/admin/audit");
          setAuditEvents(audit.slice(0, 8));
        } else {
          setAuditEvents([]);
        }
      } catch (err) {
        setError(err instanceof Error ? err.message : "Unable to load dashboard data.");
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
      <ErrorBanner message={error} />
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
                    setRefreshingBeat(beat.id);
                    setError(null);
                    try {
                      const response = await apiFetch<{ lastRefreshedAt?: string }>(
                        `/api/ingest/refresh?mode=SEARCH&beatId=${beat.id}&lensOrTrack=BEAT`,
                        {
                          method: "POST",
                        }
                      );
                      setBeats((prev) =>
                        prev.map((item) =>
                          item.id === beat.id
                            ? { ...item, lastRefreshedAt: response.lastRefreshedAt ?? null }
                            : item
                        )
                      );
                    } catch (err) {
                      setError(err instanceof Error ? err.message : "Unable to refresh beat.");
                    } finally {
                      setRefreshingBeat(null);
                    }
                  }}
                  disabled={refreshingBeat === beat.id}
                  className="rounded-lg border border-cyan-500/60 bg-cyan-500/10 px-3 py-1.5 text-xs text-cyan-100 disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {refreshingBeat === beat.id && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-cyan-200 border-t-transparent" />
                    )}
                    {refreshingBeat === beat.id ? "Refreshing..." : "Refresh"}
                  </span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
      <section className="grid gap-6 lg:grid-cols-[1.1fr_0.9fr]">
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Saved articles</h2>
            <span className="text-xs uppercase tracking-[0.2em] text-slate-400">
              {savedArticles.length} recent
            </span>
          </div>
          <div className="mt-4 space-y-3">
            {savedArticles.length === 0 && (
              <p className="text-sm text-slate-400">No saved articles yet.</p>
            )}
            {savedArticles.map((article) => (
              <Link
                key={article.articleId}
                href={`/articles/${article.articleId}`}
                className="block rounded-xl border border-slate-800/80 bg-slate-950/60 p-4 transition hover:border-cyan-500/60"
              >
                <p className="text-sm font-semibold">{article.article.title}</p>
                <p className="text-xs text-slate-400">{article.article.beatName ?? "Trending"}</p>
                <p className="text-xs text-slate-500 mt-1">
                  {article.article.publishedAtUtc
                    ? new Date(article.article.publishedAtUtc).toLocaleString()
                    : "Not published"}
                </p>
              </Link>
            ))}
          </div>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Audit log</h2>
            <span className="text-xs uppercase tracking-[0.2em] text-slate-400">
              {role === "ADMIN" ? "Admin view" : "Restricted"}
            </span>
          </div>
          <div className="mt-4">
            {role !== "ADMIN" && (
              <p className="text-sm text-slate-400">Audit log is available to admins only.</p>
            )}
            {role === "ADMIN" && auditEvents.length === 0 && (
              <p className="text-sm text-slate-400">No audit events yet.</p>
            )}
            {role === "ADMIN" && auditEvents.length > 0 && (
              <AuditTimeline
                events={auditEvents.map((event) => ({
                  id: event.id,
                  action: event.action,
                  entity: event.entityId ? `${event.entityType}:${event.entityId}` : event.entityType,
                  timestamp: event.createdAt ?? new Date().toISOString(),
                }))}
              />
            )}
          </div>
        </div>
      </section>
    </div>
  );
}

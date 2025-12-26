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
    authorRaw?: string | null;
    journalistName?: string | null;
    journalistId?: number | null;
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
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Dashboard</p>
            <h1 className="text-3xl font-semibold">Command your PR narrative</h1>
            <p className="text-slate-600">A clear snapshot of cached coverage by beat.</p>
          </div>
          <div className="rounded-full border border-cyan-200 bg-cyan-50 px-4 py-2 text-sm text-cyan-700">
            {beats.length} beats tracked
          </div>
        </div>
      </header>
      <ErrorBanner message={error} />
      <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-8">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Beat refresh status</h2>
          <span className="text-xs uppercase tracking-[0.2em] text-slate-600">
            {loading ? "Loading..." : "Cache-first"}
          </span>
        </div>
        <div className="mt-4 grid gap-4 md:grid-cols-2">
          {beats.map((beat) => (
            <div key={beat.id} className="rounded-xl border border-slate-200/70 bg-white p-4">
              <div className="flex items-center justify-between">
                <div>
                  <p className="text-sm font-semibold">{beat.name}</p>
                  <p className="text-xs text-slate-600">
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
                  className="rounded-lg border border-cyan-300/70 bg-cyan-50 px-3 py-1.5 text-xs text-cyan-700 transition hover:bg-cyan-100 disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {refreshingBeat === beat.id && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-cyan-600 border-t-transparent" />
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
        <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-8">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Saved articles</h2>
            <span className="text-xs uppercase tracking-[0.2em] text-slate-600">
              {savedArticles.length} recent
            </span>
          </div>
          <div className="mt-4 space-y-3">
            {savedArticles.length === 0 && (
              <p className="text-sm text-slate-600">No saved articles yet.</p>
            )}
            {savedArticles.map((article) => (
              <Link
                key={article.articleId}
                href={`/articles/${article.articleId}`}
                className="block rounded-xl border border-slate-200/70 bg-white p-4 transition hover:border-cyan-400/60"
              >
                <p className="text-sm font-semibold">{article.article.title}</p>
                <p className="text-xs text-slate-600">
                  {article.article.journalistName ?? article.article.authorRaw ?? "Unknown author"} ·{" "}
                  {article.article.beatName ?? "Trending"}
                </p>
                <p className="text-xs text-slate-500 mt-1">
                  {article.article.publishedAtUtc
                    ? new Date(article.article.publishedAtUtc).toLocaleString()
                    : "Not published"}
                </p>
              </Link>
            ))}
          </div>
        </div>
        <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-8">
          <div className="flex items-center justify-between">
            <h2 className="text-lg font-semibold">Audit log</h2>
            <span className="text-xs uppercase tracking-[0.2em] text-slate-600">
              {role === "ADMIN" ? "Admin view" : "Restricted"}
            </span>
          </div>
          <div className="mt-4">
            {role !== "ADMIN" && (
              <p className="text-sm text-slate-600">Audit log is available to admins only.</p>
            )}
            {role === "ADMIN" && auditEvents.length === 0 && (
              <p className="text-sm text-slate-600">No audit events yet.</p>
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

"use client";

import { useEffect, useMemo, useState } from "react";
import { useParams, useRouter } from "next/navigation";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface JournalistArticleSummary {
  articleId: number;
  title: string;
  url: string;
  publishedAtUtc?: string;
}

interface JournalistContact {
  id: number;
  email?: string;
  phone?: string;
  visibility: string;
  sourceType: string;
  verifiedAt?: string;
  verifiedBy?: string;
}

interface JournalistResponse {
  id: number;
  fullName: string;
  publicationName?: string;
  publicationDomain?: string;
  designation?: string;
  linkedin?: string;
  beats: string[];
  country?: string;
  city?: string;
  journeySummary?: string;
  verificationStatus: string;
  completenessScore: number;
  articles: JournalistArticleSummary[];
  contacts?: JournalistContact[];
}

export default function JournalistProfilePage() {
  const params = useParams();
  const router = useRouter();
  const journalistId = params?.id as string;
  const [journalist, setJournalist] = useState<JournalistResponse | null>(null);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (!journalistId) return;
    apiFetch<JournalistResponse>(`/api/journalists/${journalistId}`)
      .then((data) => {
        setJournalist(data);
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load journalist."));
  }, [journalistId]);

  const missingFields = useMemo(() => {
    if (!journalist) return [];
    const missing: string[] = [];
    if (!journalist.designation) missing.push("Designation");
    if (!journalist.beats || journalist.beats.length === 0) missing.push("Beats");
    if (!journalist.publicationName) missing.push("Publication");
    return missing;
  }, [journalist]);

  const handleSearchWeb = () => {
    if (!journalist) return;
    const query = `"${journalist.fullName}" "${journalist.publicationName ?? ""}" journalist`;
    window.open(`https://www.google.com/search?q=${encodeURIComponent(query)}`, "_blank");
  };

  const handleSearchArticles = () => {
    if (!journalist) return;
    router.push(`/search?journalistId=${journalist.id}`);
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Journalist</p>
        <h1 className="text-2xl font-semibold">{journalist?.fullName ?? "Loading"}</h1>
        <p className="text-slate-400">
          {journalist?.publicationName ?? "Publication pending"} · {journalist?.designation ?? "Role pending"}
        </p>
      </header>
      <ErrorBanner message={error} />
      {journalist && (
        <div className="grid gap-6 lg:grid-cols-[2fr,1fr]">
          <section className="space-y-4">
            <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-2">
              <div className="flex flex-wrap gap-2">
                <span className="rounded-full bg-slate-800/80 px-3 py-1 text-xs text-slate-200">
                  {journalist.verificationStatus}
                </span>
                <span className="rounded-full bg-cyan-500/10 px-3 py-1 text-xs text-cyan-200">
                  Completeness {journalist.completenessScore}%
                </span>
              </div>
              <p className="text-sm text-slate-300">{journalist.journeySummary ?? "No verified bio yet."}</p>
            </div>
            <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
              <h2 className="text-lg font-semibold">Linked Articles</h2>
              {journalist.articles.length === 0 ? (
                <p className="text-sm text-slate-400 mt-2">No linked articles yet.</p>
              ) : (
                <ul className="mt-3 space-y-3">
                  {journalist.articles.map((article) => (
                    <li key={article.articleId} className="rounded-xl border border-slate-800/80 bg-slate-950/60 p-4">
                      <a href={article.url} target="_blank" rel="noreferrer" className="text-sm text-cyan-200">
                        {article.title}
                      </a>
                      <p className="text-xs text-slate-500 mt-1">{article.publishedAtUtc ?? ""}</p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>
          <aside className="space-y-4">
            <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-3">
              <h2 className="text-sm font-semibold text-slate-200">Search tools</h2>
              <button
                onClick={handleSearchWeb}
                className="w-full rounded-xl border border-cyan-500/60 bg-cyan-500/10 px-4 py-2 text-sm font-semibold text-cyan-100 hover:bg-cyan-500/20"
              >
                Search Web
              </button>
              <button
                onClick={handleSearchArticles}
                className="w-full rounded-xl border border-slate-700/80 bg-slate-900/60 px-4 py-2 text-sm font-semibold text-slate-200 hover:bg-slate-800/60"
              >
                Search Articles
              </button>
            </div>
            <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
              <h2 className="text-sm font-semibold text-slate-200">Why incomplete?</h2>
              {missingFields.length === 0 ? (
                <p className="text-xs text-emerald-300 mt-2">All required fields captured.</p>
              ) : (
                <ul className="mt-3 space-y-1 text-xs text-slate-400">
                  {missingFields.map((field) => (
                    <li key={field}>• {field}</li>
                  ))}
                </ul>
              )}
            </div>
          </aside>
        </div>
      )}
    </div>
  );
}

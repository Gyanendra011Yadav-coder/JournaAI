"use client";

import { useEffect, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface JournalistSummary {
  id: number;
  fullName: string;
  publicationName?: string;
  designation?: string;
  beats?: string[];
  verificationStatus: string;
  completenessScore: number;
}

export default function AdminJournalistsPage() {
  const [journalists, setJournalists] = useState<JournalistSummary[]>([]);
  const [missingField, setMissingField] = useState("all");
  const [searchBy, setSearchBy] = useState("name");
  const [query, setQuery] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const router = useRouter();

  const loadJournalists = () => {
    setLoading(true);
    const params = new URLSearchParams();
    if (missingField) {
      params.set("missing", missingField);
    }
    if (query.trim()) {
      params.set("q", query.trim());
    }
    if (searchBy) {
      params.set("searchBy", searchBy);
    }
    const suffix = params.toString();
    apiFetch<JournalistSummary[]>(`/api/admin/journalists/incomplete${suffix ? `?${suffix}` : ""}`)
      .then((data) => {
        setJournalists(data);
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load journalists."))
      .finally(() => setLoading(false));
  };

  useEffect(() => {
    loadJournalists();
  }, [missingField, query, searchBy]);

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Admin</p>
        <h1 className="text-3xl font-semibold">Journalist Enrichment</h1>
        <p className="text-slate-600">Focus on incomplete profiles and approve updates.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="flex flex-wrap items-center gap-3">
        <label className="text-sm text-slate-600">Missing</label>
        <select
          value={missingField}
          onChange={(event) => setMissingField(event.target.value)}
          className="rounded-xl border border-slate-200 bg-white/80 px-3 py-2 text-sm text-slate-700"
        >
          <option value="all">All</option>
          <option value="email">Email</option>
          <option value="contacts">Contact details</option>
          <option value="beats">Beats</option>
          <option value="publication">Publication</option>
          <option value="designation">Designation</option>
          <option value="bio">Bio summary</option>
          <option value="links">Public links</option>
          <option value="location">Location</option>
          <option value="aliases">Aliases</option>
          <option value="topics">Topic keywords</option>
          <option value="languages">Languages</option>
          <option value="regions">Coverage regions</option>
        </select>
        <label className="text-sm text-slate-600">Search by</label>
        <select
          value={searchBy}
          onChange={(event) => setSearchBy(event.target.value)}
          className="rounded-xl border border-slate-200 bg-white/80 px-3 py-2 text-sm text-slate-700"
        >
          <option value="any">Any</option>
          <option value="name">Name</option>
          <option value="alias">Alias</option>
          <option value="email">Email</option>
          <option value="phone">Phone</option>
          <option value="beat">Beat</option>
          <option value="topic">Topic</option>
          <option value="publication">Publication</option>
          <option value="designation">Designation</option>
          <option value="location">Location</option>
          <option value="language">Language</option>
          <option value="region">Coverage region</option>
        </select>
        <input
          value={query}
          onChange={(event) => setQuery(event.target.value)}
          placeholder="Search journalists"
          className="rounded-xl border border-slate-200 bg-white/80 px-3 py-2 text-sm text-slate-700"
        />
        <button
          type="button"
          onClick={loadJournalists}
          disabled={loading}
          className="rounded-xl border border-cyan-300/70 bg-cyan-50 px-3 py-2 text-sm font-semibold text-cyan-700 hover:bg-cyan-100 disabled:opacity-60"
        >
          {loading ? "Searching..." : "Search"}
        </button>
      </div>
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
        {journalists.length === 0 ? (
          <p className="text-sm text-slate-600">No journalists for this filter.</p>
        ) : (
          <div className="space-y-3">
            {journalists.map((journalist) => (
              <div
                key={journalist.id}
                role="button"
                tabIndex={0}
                onClick={() => router.push(`/journalists/${journalist.id}`)}
                onKeyDown={(event) => {
                  if (event.key === "Enter" || event.key === " ") {
                    event.preventDefault();
                    router.push(`/journalists/${journalist.id}`);
                  }
                }}
                className="flex flex-wrap items-center justify-between gap-3 rounded-2xl border border-slate-200/70 bg-white p-4 transition hover:border-cyan-200 hover:bg-cyan-50/40"
              >
                <div>
                  <p className="text-sm font-semibold text-slate-900">{journalist.fullName}</p>
                  <p className="text-xs text-slate-500">{journalist.publicationName ?? "Publication pending"}</p>
                  {(journalist.designation || (journalist.beats && journalist.beats.length > 0)) && (
                    <p className="text-[11px] text-slate-500">
                      {journalist.designation ?? "Role pending"}
                      {journalist.beats && journalist.beats.length > 0
                        ? ` · ${journalist.beats.slice(0, 3).join(", ")}`
                        : ""}
                    </p>
                  )}
                </div>
                <div className="flex items-center gap-3">
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
                    {journalist.verificationStatus}
                  </span>
                  <span className="rounded-xl border border-cyan-300/70 bg-cyan-50 px-3 py-2 text-xs font-semibold text-cyan-700">
                    Open
                  </span>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

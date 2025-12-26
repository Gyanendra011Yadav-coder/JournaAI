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

interface JournalistForm {
  fullName: string;
  publicationName: string;
  publicationDomain: string;
  designation: string;
  linkedin: string;
  beats: string;
  country: string;
  city: string;
  journeySummary: string;
  verificationStatus: string;
  completenessScore: number;
}

export default function JournalistProfilePage() {
  const params = useParams();
  const router = useRouter();
  const journalistId = params?.id as string;
  const [journalist, setJournalist] = useState<JournalistResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [draft, setDraft] = useState<JournalistForm | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<{ role: string }>("/api/auth/me")
      .then((data) => setRole(data.role))
      .catch(() => setRole(null));
  }, []);

  useEffect(() => {
    if (!journalistId) return;
    apiFetch<JournalistResponse>(`/api/journalists/${journalistId}`)
      .then((data) => {
        setJournalist(data);
        setDraft({
          fullName: data.fullName ?? "",
          publicationName: data.publicationName ?? "",
          publicationDomain: data.publicationDomain ?? "",
          designation: data.designation ?? "",
          linkedin: data.linkedin ?? "",
          beats: (data.beats ?? []).join(", "),
          country: data.country ?? "",
          city: data.city ?? "",
          journeySummary: data.journeySummary ?? "",
          verificationStatus: data.verificationStatus ?? "UNVERIFIED",
          completenessScore: data.completenessScore ?? 0,
        });
        setSaveNotice(null);
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
    router.push(`/search?journalistId=${journalist.id}&journalistName=${encodeURIComponent(journalist.fullName)}`);
  };

  const handleSave = async () => {
    if (!draft || !journalist) return;
    setSaving(true);
    setError(null);
    try {
      const payload = {
        fullName: draft.fullName,
        publicationName: draft.publicationName,
        publicationDomain: draft.publicationDomain,
        designation: draft.designation,
        linkedin: draft.linkedin,
        beats: draft.beats
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        country: draft.country,
        city: draft.city,
        journeySummary: draft.journeySummary,
        verificationStatus: draft.verificationStatus,
        completenessScore: draft.completenessScore,
      };
      const updated = await apiFetch<JournalistResponse>(`/api/admin/journalists/${journalist.id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setJournalist(updated);
      setDraft({
        fullName: updated.fullName ?? "",
        publicationName: updated.publicationName ?? "",
        publicationDomain: updated.publicationDomain ?? "",
        designation: updated.designation ?? "",
        linkedin: updated.linkedin ?? "",
        beats: (updated.beats ?? []).join(", "),
        country: updated.country ?? "",
        city: updated.city ?? "",
        journeySummary: updated.journeySummary ?? "",
        verificationStatus: updated.verificationStatus ?? "UNVERIFIED",
        completenessScore: updated.completenessScore ?? 0,
      });
      setSaveNotice("Journalist profile updated.");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save journalist updates.");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Journalist</p>
        <h1 className="text-3xl font-semibold">{journalist?.fullName ?? "Loading"}</h1>
        <p className="text-slate-600">
          {journalist?.publicationName ?? "Publication pending"} · {journalist?.designation ?? "Role pending"}
        </p>
      </header>
      <ErrorBanner message={error} />
      {journalist && (
        <div className="grid gap-6 lg:grid-cols-[2fr,1fr]">
          <section className="space-y-4">
            <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-2 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
              <div className="flex flex-wrap gap-2">
                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
                  {journalist.verificationStatus}
                </span>
                <span className="rounded-full bg-cyan-500/10 px-3 py-1 text-xs text-cyan-700">
                  Completeness {journalist.completenessScore}%
                </span>
              </div>
              <p className="text-sm text-slate-600">{journalist.journeySummary ?? "No verified bio yet."}</p>
            </div>
            <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
              <h2 className="text-lg font-semibold">Linked Articles</h2>
              {journalist.articles.length === 0 ? (
                <p className="text-sm text-slate-600 mt-2">No linked articles yet.</p>
              ) : (
                <ul className="mt-3 space-y-3">
                  {journalist.articles.map((article) => (
                    <li key={article.articleId} className="rounded-xl border border-slate-200/70 bg-white p-4">
                      <a href={article.url} target="_blank" rel="noreferrer" className="text-sm text-cyan-700">
                        {article.title}
                      </a>
                      <p className="text-xs text-slate-500 mt-1">
                        {article.publishedAtUtc ? new Date(article.publishedAtUtc).toLocaleString() : ""}
                      </p>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </section>
          <aside className="space-y-4">
            <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-3 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
              <h2 className="text-sm font-semibold text-slate-700">Search tools</h2>
              <button
                onClick={handleSearchWeb}
                className="w-full rounded-xl border border-cyan-300/70 bg-cyan-50 px-4 py-2 text-sm font-semibold text-cyan-700 hover:bg-cyan-100"
              >
                Search Web
              </button>
              <button
                onClick={handleSearchArticles}
                className="w-full rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-cyan-300 hover:bg-slate-50"
              >
                Search Articles
              </button>
            </div>
            <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
              <h2 className="text-sm font-semibold text-slate-700">Why incomplete?</h2>
              {missingFields.length === 0 ? (
                <p className="text-xs text-emerald-600 mt-2">All required fields captured.</p>
              ) : (
                <ul className="mt-3 space-y-1 text-xs text-slate-500">
                  {missingFields.map((field) => (
                    <li key={field}>• {field}</li>
                  ))}
                </ul>
              )}
            </div>
            {role === "ADMIN" && draft && (
              <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-4 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <h2 className="text-sm font-semibold text-slate-700">Admin updates</h2>
                <div className="space-y-3 text-sm text-slate-600">
                  <div>
                    <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Full name</label>
                    <input
                      className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                      value={draft.fullName}
                      onChange={(event) => setDraft({ ...draft, fullName: event.target.value })}
                    />
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Publication</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.publicationName}
                        onChange={(event) => setDraft({ ...draft, publicationName: event.target.value })}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Publication domain</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.publicationDomain}
                        onChange={(event) => setDraft({ ...draft, publicationDomain: event.target.value })}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Designation</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.designation}
                        onChange={(event) => setDraft({ ...draft, designation: event.target.value })}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">LinkedIn</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.linkedin}
                        onChange={(event) => setDraft({ ...draft, linkedin: event.target.value })}
                      />
                    </div>
                  </div>
                  <div>
                    <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Beats (comma-separated)</label>
                    <input
                      className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                      value={draft.beats}
                      onChange={(event) => setDraft({ ...draft, beats: event.target.value })}
                    />
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Country</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.country}
                        onChange={(event) => setDraft({ ...draft, country: event.target.value })}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">City</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.city}
                        onChange={(event) => setDraft({ ...draft, city: event.target.value })}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Verification</label>
                      <select
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.verificationStatus}
                        onChange={(event) => setDraft({ ...draft, verificationStatus: event.target.value })}
                      >
                        <option value="UNVERIFIED">Unverified</option>
                        <option value="VERIFIED">Verified</option>
                      </select>
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Completeness</label>
                      <input
                        type="number"
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.completenessScore}
                        onChange={(event) =>
                          setDraft({ ...draft, completenessScore: Number(event.target.value) })
                        }
                      />
                    </div>
                  </div>
                  <div>
                    <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Bio summary</label>
                    <textarea
                      rows={3}
                      className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                      value={draft.journeySummary}
                      onChange={(event) => setDraft({ ...draft, journeySummary: event.target.value })}
                    />
                  </div>
                  {saveNotice && (
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
                      {saveNotice}
                    </div>
                  )}
                  <button
                    onClick={handleSave}
                    disabled={saving}
                    className="w-full rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 px-4 py-2 text-sm font-semibold text-slate-900 disabled:opacity-60"
                  >
                    <span className="inline-flex items-center gap-2">
                      {saving && (
                        <span className="h-3 w-3 animate-spin rounded-full border border-slate-200 border-t-transparent" />
                      )}
                      {saving ? "Saving..." : "Save updates"}
                    </span>
                  </button>
                </div>
              </div>
            )}
          </aside>
        </div>
      )}
    </div>
  );
}

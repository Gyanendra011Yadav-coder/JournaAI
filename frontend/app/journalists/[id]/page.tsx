"use client";

import { useCallback, useEffect, useMemo, useState } from "react";
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
  twitter?: string;
  authorPageUrl?: string;
  beats: string[];
  country?: string;
  city?: string;
  journeySummary?: string;
  bioSummary?: string;
  verificationStatus: string;
  completenessScore: number;
  articles: JournalistArticleSummary[];
  contacts?: JournalistContact[];
  pendingReviewId?: number | null;
  pendingReviewStatus?: string | null;
  pendingProfileJsonb?: string | null;
}

interface JournalistForm {
  fullName: string;
  publicationName: string;
  publicationDomain: string;
  designation: string;
  linkedin: string;
  twitter: string;
  authorPageUrl: string;
  beats: string;
  country: string;
  city: string;
  bioSummary: string;
  verificationStatus: string;
  completenessScore: number;
}

interface PendingProfile {
  full_name?: string;
  publication_name?: string;
  publication_domain?: string;
  designation?: string | null;
  beats?: string[];
  location?: {
    country?: string | null;
    city?: string | null;
  };
  public_links?: {
    author_page?: string | null;
    twitter?: string | null;
    linkedin?: string | null;
  };
  bio_summary?: string | null;
}

export default function JournalistProfilePage() {
  const params = useParams();
  const router = useRouter();
  const journalistId = params?.id as string;
  const [journalist, setJournalist] = useState<JournalistResponse | null>(null);
  const [pendingProfile, setPendingProfile] = useState<PendingProfile | null>(null);
  const [pendingStatus, setPendingStatus] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [role, setRole] = useState<string | null>(null);
  const [draft, setDraft] = useState<JournalistForm | null>(null);
  const [saving, setSaving] = useState(false);
  const [saveNotice, setSaveNotice] = useState<string | null>(null);
  const [refreshing, setRefreshing] = useState(false);
  const [refreshNotice, setRefreshNotice] = useState<string | null>(null);
  const [enrichmentStatus, setEnrichmentStatus] = useState<string | null>(null);
  const [enrichmentNotes, setEnrichmentNotes] = useState<string | null>(null);
  const [polling, setPolling] = useState(false);
  const [showContacts, setShowContacts] = useState(false);

  const enrichmentActive =
    refreshing || polling || enrichmentStatus === "PENDING" || enrichmentStatus === "RUNNING";

  useEffect(() => {
    apiFetch<{ role: string }>("/api/auth/me")
      .then((data) => setRole(data.role))
      .catch(() => setRole(null));
  }, []);

  const parsePendingProfile = (raw?: string | null) => {
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as Partial<PendingProfile> & {
        author_page_url?: string | null;
        author_page?: string | null;
        twitter?: string | null;
        linkedin?: string | null;
        country?: string | null;
        city?: string | null;
        beats?: string[] | string | null;
      };
      if (parsed && typeof parsed === "object") {
        if (!parsed.public_links) {
          const authorPage = parsed.author_page_url ?? parsed.author_page ?? null;
          const twitter = parsed.twitter ?? null;
          const linkedin = parsed.linkedin ?? null;
          if (authorPage || twitter || linkedin) {
            parsed.public_links = { author_page: authorPage, twitter, linkedin };
          }
        }
        if (!parsed.location && (parsed.country || parsed.city)) {
          parsed.location = { country: parsed.country ?? null, city: parsed.city ?? null };
        }
        if (typeof parsed.beats === "string") {
          parsed.beats = parsed.beats
            .split(",")
            .map((beat) => beat.trim())
            .filter(Boolean);
        }
      }
      return parsed as PendingProfile;
    } catch {
      return null;
    }
  };

  const loadJournalist = useCallback(async () => {
    if (!journalistId) return;
    try {
      const data = await apiFetch<JournalistResponse>(`/api/journalists/${journalistId}`);
      setJournalist(data);
      setPendingStatus(data.pendingReviewStatus ?? null);
      setPendingProfile(parsePendingProfile(data.pendingProfileJsonb));
      setDraft({
        fullName: data.fullName ?? "",
        publicationName: data.publicationName ?? "",
        publicationDomain: data.publicationDomain ?? "",
        designation: data.designation ?? "",
        linkedin: data.linkedin ?? "",
        twitter: data.twitter ?? "",
        authorPageUrl: data.authorPageUrl ?? "",
        beats: (data.beats ?? []).join(", "),
        country: data.country ?? "",
        city: data.city ?? "",
        bioSummary: data.bioSummary ?? data.journeySummary ?? "",
        verificationStatus: data.verificationStatus ?? "UNVERIFIED",
        completenessScore: data.completenessScore ?? 0,
      });
      setSaveNotice(null);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load journalist.");
    }
  }, [journalistId]);

  useEffect(() => {
    loadJournalist();
  }, [loadJournalist]);

  const missingFields = useMemo(() => {
    if (!journalist) return [];
    const missing: string[] = [];
    if (!journalist.designation) {
      missing.push(pendingProfile?.designation ? "Designation (pending verification)" : "Designation");
    }
    if (!journalist.beats || journalist.beats.length === 0) {
      missing.push(pendingProfile?.beats?.length ? "Beats (pending verification)" : "Beats");
    }
    if (!journalist.publicationName) {
      missing.push(pendingProfile?.publication_name ? "Publication (pending verification)" : "Publication");
    }
    return missing;
  }, [journalist, pendingProfile]);

  const summaryText = useMemo(() => {
    if (!journalist) return "No verified bio yet.";
    if (journalist.journeySummary) return journalist.journeySummary;
    if (journalist.bioSummary) return journalist.bioSummary;
    if (pendingProfile?.bio_summary) {
      return `${pendingProfile.bio_summary} (pending verification)`;
    }
    return "No verified bio yet.";
  }, [journalist, pendingProfile]);

  const profileLinks = useMemo(() => {
    if (!journalist) return [];
    const links: Array<{ label: string; value: string; pending?: boolean }> = [];
    const authorPage = journalist.authorPageUrl ?? pendingProfile?.public_links?.author_page;
    if (authorPage) {
      links.push({ label: "Author page", value: authorPage, pending: !journalist.authorPageUrl });
    }
    const twitter = journalist.twitter ?? pendingProfile?.public_links?.twitter;
    if (twitter) {
      links.push({ label: "Twitter", value: twitter, pending: !journalist.twitter });
    }
    const linkedin = journalist.linkedin ?? pendingProfile?.public_links?.linkedin;
    if (linkedin) {
      links.push({ label: "LinkedIn", value: linkedin, pending: !journalist.linkedin });
    }
    return links;
  }, [journalist, pendingProfile]);

  const contactSummary = useMemo(() => {
    if (!journalist?.contacts || journalist.contacts.length === 0) {
      return { count: 0, emails: 0, phones: 0 };
    }
    let emails = 0;
    let phones = 0;
    journalist.contacts.forEach((contact) => {
      if (contact.email) emails += 1;
      if (contact.phone) phones += 1;
    });
    return { count: journalist.contacts.length, emails, phones };
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
      const payload: Record<string, unknown> = {
        fullName: draft.fullName,
        publicationName: draft.publicationName,
        publicationDomain: draft.publicationDomain,
        designation: draft.designation,
        linkedin: draft.linkedin,
        twitter: draft.twitter,
        authorPageUrl: draft.authorPageUrl,
        beats: draft.beats
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        country: draft.country,
        city: draft.city,
        journeySummary: draft.bioSummary,
        bioSummary: draft.bioSummary,
        verificationStatus: draft.verificationStatus,
      };
      if (draft.completenessScore !== journalist.completenessScore) {
        payload.completenessScore = draft.completenessScore;
      }
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
        twitter: updated.twitter ?? "",
        authorPageUrl: updated.authorPageUrl ?? "",
        beats: (updated.beats ?? []).join(", "),
        country: updated.country ?? "",
        city: updated.city ?? "",
        bioSummary: updated.bioSummary ?? updated.journeySummary ?? "",
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

  const handleRefresh = async () => {
    if (!journalist) return;
    setRefreshing(true);
    setError(null);
    setRefreshNotice(null);
    setEnrichmentNotes(null);
    try {
      await apiFetch(`/api/admin/enrichment/run?journalistId=${journalist.id}`, { method: "POST" });
      setRefreshNotice("Journalist enrichment started.");
      setEnrichmentStatus("PENDING");
      setPolling(true);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to trigger enrichment.");
    } finally {
      setRefreshing(false);
    }
  };

  const loadEnrichmentStatus = useCallback(async () => {
    if (!journalistId) return;
    try {
      const tasks = await apiFetch<{ status: string; notes?: string | null }[]>(
        `/api/admin/enrichment/tasks?journalistId=${journalistId}&taskType=ENRICH_PROFILE`
      );
      if (tasks.length > 0) {
        const latest = tasks[0];
        setEnrichmentStatus(latest.status);
        setEnrichmentNotes(latest.notes ?? null);
        if (["DONE", "NEEDS_REVIEW", "FAILED"].includes(latest.status)) {
          setPolling(false);
          if (latest.notes) {
            setRefreshNotice(`Enrichment ${latest.status.toLowerCase()}: ${latest.notes}`);
          } else {
            setRefreshNotice(`Enrichment ${latest.status.toLowerCase()}.`);
          }
        }
      } else {
        setEnrichmentStatus(null);
        setEnrichmentNotes(null);
        setPolling(false);
      }
    } catch {
      setEnrichmentStatus("FAILED");
      setEnrichmentNotes("Unable to load status.");
      setPolling(false);
    }
    await loadJournalist();
  }, [journalistId, loadJournalist]);

  useEffect(() => {
    if (!polling) {
      return;
    }
    loadEnrichmentStatus();
    const interval = setInterval(() => {
      loadEnrichmentStatus();
    }, 4000);
    return () => clearInterval(interval);
  }, [polling, loadEnrichmentStatus]);

  const pendingEntries = useMemo(() => {
    if (!pendingProfile) return [];
    const entries: Array<{ label: string; value: string }> = [];
    if (pendingProfile.full_name) entries.push({ label: "Full name", value: pendingProfile.full_name });
    if (pendingProfile.publication_name) entries.push({ label: "Publication", value: pendingProfile.publication_name });
    if (pendingProfile.publication_domain) entries.push({ label: "Publication domain", value: pendingProfile.publication_domain });
    if (pendingProfile.designation) entries.push({ label: "Designation", value: pendingProfile.designation });
    if (pendingProfile.beats) {
      const beatsValue = Array.isArray(pendingProfile.beats)
        ? pendingProfile.beats.join(", ")
        : String(pendingProfile.beats);
      if (beatsValue.trim()) {
        entries.push({ label: "Beats", value: beatsValue });
      }
    }
    if (pendingProfile.location?.country) entries.push({ label: "Country", value: pendingProfile.location.country });
    if (pendingProfile.location?.city) entries.push({ label: "City", value: pendingProfile.location.city });
    if (pendingProfile.public_links?.author_page) entries.push({ label: "Author page", value: pendingProfile.public_links.author_page });
    if (pendingProfile.public_links?.twitter) entries.push({ label: "Twitter", value: pendingProfile.public_links.twitter });
    if (pendingProfile.public_links?.linkedin) entries.push({ label: "LinkedIn", value: pendingProfile.public_links.linkedin });
    if (pendingProfile.bio_summary) entries.push({ label: "Bio summary", value: pendingProfile.bio_summary });
    return entries;
  }, [pendingProfile]);

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Journalist</p>
        <h1 className="text-3xl font-semibold">{journalist?.fullName ?? "Loading"}</h1>
        <p className="text-slate-600">
          {journalist?.publicationName ?? "Publication pending"} · {journalist?.designation ?? "Role pending"}
        </p>
        {enrichmentActive && (
          <div className="mt-4 inline-flex items-center gap-2 rounded-full border border-cyan-200 bg-cyan-50 px-3 py-1 text-xs font-semibold text-cyan-700">
            <span className="h-3 w-3 animate-spin rounded-full border border-cyan-400 border-t-transparent" />
            Refreshing profile details...
          </div>
        )}
      </header>
      <ErrorBanner message={error} />
      {journalist && (
        <div className="grid gap-6 lg:grid-cols-[2fr,1fr]">
          <section className="space-y-4">
            <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-2 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
              <div className="flex flex-wrap gap-2">
                <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
                  {pendingStatus === "PENDING" ? "Pending verification" : journalist.verificationStatus}
                </span>
                <span className="rounded-full bg-cyan-500/10 px-3 py-1 text-xs text-cyan-700">
                  Completeness {journalist.completenessScore}%
                </span>
              </div>
              <p className="text-sm text-slate-600">{summaryText}</p>
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
            {profileLinks.length > 0 && (
              <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-3 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <h2 className="text-sm font-semibold text-slate-700">Profile links</h2>
                <div className="space-y-2 text-xs text-slate-600">
                  {profileLinks.map((link) => (
                    <div key={link.label} className="space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] uppercase tracking-[0.2em] text-slate-500">
                          {link.label}
                        </span>
                        {link.pending && (
                          <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-700">
                            Pending
                          </span>
                        )}
                      </div>
                      <a
                        href={link.value}
                        target="_blank"
                        rel="noreferrer"
                        className="block truncate rounded-lg border border-slate-200/70 bg-white px-3 py-2 text-xs text-cyan-700 hover:underline"
                      >
                        {link.value}
                      </a>
                    </div>
                  ))}
                </div>
              </div>
            )}
            {journalist && (
              <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-3 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <div className="flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-slate-700">Contact details</h2>
                  {contactSummary.count > 0 && (
                    <button
                      onClick={() => setShowContacts((prev) => !prev)}
                      className="rounded-full border border-slate-200 px-3 py-1 text-[11px] font-semibold text-slate-600 hover:border-cyan-300 hover:text-cyan-700"
                    >
                      {showContacts ? "Hide" : "Show"}
                    </button>
                  )}
                </div>
                {contactSummary.count === 0 ? (
                  <p className="text-xs text-slate-500">No contact details captured yet.</p>
                ) : (
                  <>
                    <p className="text-xs text-slate-500">
                      {contactSummary.emails} emails · {contactSummary.phones} phones
                    </p>
                    {showContacts && (
                      <div className="space-y-2 text-xs text-slate-600">
                        {journalist.contacts?.map((contact) => (
                          <div key={contact.id} className="rounded-xl border border-slate-200/70 bg-white px-3 py-2">
                            <div className="flex items-center justify-between text-[10px] uppercase tracking-[0.2em] text-slate-500">
                              <span>{contact.visibility.replace("_", " ")}</span>
                              <span>{contact.sourceType.replace("_", " ")}</span>
                            </div>
                            {contact.email && <p className="mt-1 text-sm text-slate-700">{contact.email}</p>}
                            {contact.phone && <p className="text-sm text-slate-700">{contact.phone}</p>}
                            {contact.verifiedAt && (
                              <p className="text-[11px] text-emerald-600">
                                Verified {new Date(contact.verifiedAt).toLocaleDateString()}
                                {contact.verifiedBy ? ` · ${contact.verifiedBy}` : ""}
                              </p>
                            )}
                          </div>
                        ))}
                      </div>
                    )}
                  </>
                )}
              </div>
            )}
            {role === "ADMIN" && (
              <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-3 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <h2 className="text-sm font-semibold text-slate-700">Enrichment</h2>
                <button
                  onClick={handleRefresh}
                  disabled={refreshing}
                  className="w-full rounded-xl border border-slate-200 px-4 py-2 text-sm font-semibold text-slate-700 hover:border-cyan-300 hover:bg-slate-50 disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {refreshing && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />
                    )}
                    {refreshing ? "Refreshing..." : "Refresh profile via LLM"}
                  </span>
                </button>
                {enrichmentStatus && (
                  <p className="text-xs text-slate-500">
                    Status: {enrichmentStatus}
                    {enrichmentNotes ? ` · ${enrichmentNotes}` : ""}
                  </p>
                )}
                {refreshNotice && (
                  <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
                    {refreshNotice}
                  </div>
                )}
              </div>
            )}
            {pendingEntries.length > 0 && (
              <div className="rounded-2xl border border-amber-200/70 bg-amber-50/70 p-6 space-y-3 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <h2 className="text-sm font-semibold text-amber-800">Pending verification</h2>
                <p className="text-xs text-amber-700">
                  The details below are awaiting admin verification.
                </p>
                <div className="space-y-2 text-xs text-slate-700">
                  {pendingEntries.map((entry) => (
                    <div key={entry.label} className="flex flex-col gap-1">
                      <span className="text-[10px] uppercase tracking-[0.2em] text-amber-700">{entry.label}</span>
                      <span className="rounded-lg border border-amber-200 bg-white px-3 py-2 text-sm text-slate-700">
                        {entry.value}
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}
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
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Twitter</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.twitter}
                        onChange={(event) => setDraft({ ...draft, twitter: event.target.value })}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Author page</label>
                      <input
                        className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
                        value={draft.authorPageUrl}
                        onChange={(event) => setDraft({ ...draft, authorPageUrl: event.target.value })}
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
                      value={draft.bioSummary}
                      onChange={(event) => setDraft({ ...draft, bioSummary: event.target.value })}
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

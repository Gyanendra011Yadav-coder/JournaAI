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
  aliases?: string[];
  publicationAliases?: string[];
  topicKeywords?: string[];
  languages?: string[];
  coverageRegions?: string[];
  otherLinks?: string[];
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
  aliases: string;
  publicationAliases: string;
  topicKeywords: string;
  languages: string;
  coverageRegions: string;
  otherLinks: string;
  country: string;
  city: string;
  journeySummary: string;
  bioSummary: string;
  verificationStatus: string;
  completenessScore: number;
}

interface PendingProfile {
  full_name?: string;
  aliases?: string[] | string;
  publication_name?: string;
  publication_domain?: string;
  publication_aliases?: string[] | string;
  designation?: string | null;
  beats?: string[];
  topic_keywords?: string[] | string;
  languages?: string[] | string;
  coverage_regions?: string[] | string;
  contacts?: {
    emails?: string[];
    inferred_emails?: string[];
    phones?: string[];
  };
  location?: {
    country?: string | null;
    city?: string | null;
  };
  public_links?: {
    author_page?: string | null;
    twitter?: string | null;
    linkedin?: string | null;
    other?: string[] | string | null;
  };
  bio_summary?: string | null;
  journey_summary?: string | null;
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
  const [contactEmail, setContactEmail] = useState("");
  const [contactPhone, setContactPhone] = useState("");
  const [contactSaving, setContactSaving] = useState(false);
  const [contactNotice, setContactNotice] = useState<string | null>(null);
  const [contactError, setContactError] = useState<string | null>(null);
  const [editMode, setEditMode] = useState(false);

  const enrichmentActive =
    refreshing || polling || enrichmentStatus === "PENDING" || enrichmentStatus === "RUNNING";
  const isAdmin = role === "ADMIN";
  const canEdit = isAdmin && editMode;
  const editFieldClass = canEdit
    ? "mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-2"
    : "mt-2 w-full rounded-xl bg-slate-100 border border-slate-200 p-2 text-slate-500";
  const contactFieldClass = canEdit
    ? "rounded-lg border border-slate-200 px-3 py-2 text-xs"
    : "rounded-lg border border-slate-200 bg-slate-100 px-3 py-2 text-xs text-slate-500";

  useEffect(() => {
    apiFetch<{ role: string }>("/api/auth/me")
      .then((data) => setRole(data.role))
      .catch(() => setRole(null));
  }, []);

  const parsePendingProfile = (raw?: string | null) => {
    if (!raw) return null;
    try {
      const parsed = JSON.parse(raw) as {
        proposed_profile?: Partial<PendingProfile>;
        contacts?: PendingProfile["contacts"];
        proposed_contacts?: PendingProfile["contacts"];
      } & Partial<PendingProfile> & {
        author_page_url?: string | null;
        author_page?: string | null;
        twitter?: string | null;
        linkedin?: string | null;
        country?: string | null;
        city?: string | null;
        beats?: string[] | string | null;
      };
      if (parsed && typeof parsed === "object") {
        const contacts = parsed.contacts ?? parsed.proposed_contacts ?? null;
        const baseProfile =
          parsed.proposed_profile && typeof parsed.proposed_profile === "object"
            ? parsed.proposed_profile
            : parsed;
        const normalized: Partial<PendingProfile> & {
          author_page_url?: string | null;
          author_page?: string | null;
          twitter?: string | null;
          linkedin?: string | null;
          country?: string | null;
          city?: string | null;
          beats?: string[] | string | null;
        } = { ...baseProfile };
        const normalizeList = (value?: string[] | string | null) => {
          if (!value) return undefined;
          if (Array.isArray(value)) return value;
          return value
            .split(",")
            .map((item) => item.trim())
            .filter(Boolean);
        };
        if (contacts) {
          normalized.contacts = contacts;
        }
        if (!normalized.public_links) {
          const authorPage = normalized.author_page_url ?? normalized.author_page ?? null;
          const twitter = normalized.twitter ?? null;
          const linkedin = normalized.linkedin ?? null;
          if (authorPage || twitter || linkedin) {
            normalized.public_links = { author_page: authorPage, twitter, linkedin };
          }
        }
        if (!normalized.location && (normalized.country || normalized.city)) {
          normalized.location = { country: normalized.country ?? null, city: normalized.city ?? null };
        }
        if (typeof normalized.beats === "string") {
          normalized.beats = normalizeList(normalized.beats);
        }
        normalized.aliases = normalizeList(normalized.aliases);
        normalized.publication_aliases = normalizeList(normalized.publication_aliases);
        normalized.topic_keywords = normalizeList(normalized.topic_keywords);
        normalized.languages = normalizeList(normalized.languages);
        normalized.coverage_regions = normalizeList(normalized.coverage_regions);
        if (normalized.public_links?.other && !Array.isArray(normalized.public_links.other)) {
          normalized.public_links.other = normalizeList(normalized.public_links.other) ?? null;
        }
        return normalized as PendingProfile;
      }
      return null;
    } catch {
      return null;
    }
  };

  const buildDraft = (data: JournalistResponse): JournalistForm => ({
    fullName: data.fullName ?? "",
    publicationName: data.publicationName ?? "",
    publicationDomain: data.publicationDomain ?? "",
    designation: data.designation ?? "",
    linkedin: data.linkedin ?? "",
    twitter: data.twitter ?? "",
    authorPageUrl: data.authorPageUrl ?? "",
    beats: (data.beats ?? []).join(", "),
    aliases: (data.aliases ?? []).join(", "),
    publicationAliases: (data.publicationAliases ?? []).join(", "),
    topicKeywords: (data.topicKeywords ?? []).join(", "),
    languages: (data.languages ?? []).join(", "),
    coverageRegions: (data.coverageRegions ?? []).join(", "),
    otherLinks: (data.otherLinks ?? []).join(", "),
    country: data.country ?? "",
    city: data.city ?? "",
    journeySummary: data.journeySummary ?? "",
    bioSummary: data.bioSummary ?? "",
    verificationStatus: data.verificationStatus ?? "UNVERIFIED",
    completenessScore: data.completenessScore ?? 0,
  });

  const loadJournalist = useCallback(async () => {
    if (!journalistId) return;
    try {
      const data = await apiFetch<JournalistResponse>(`/api/journalists/${journalistId}`);
      setJournalist(data);
      setPendingStatus(data.pendingReviewStatus ?? null);
      setPendingProfile(parsePendingProfile(data.pendingProfileJsonb));
      setDraft(buildDraft(data));
      setEditMode(false);
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
    if (!journalist.bioSummary && !journalist.journeySummary) {
      missing.push(
        pendingProfile?.bio_summary || pendingProfile?.journey_summary
          ? "Bio summary (pending verification)"
          : "Bio summary",
      );
    }
    if (!journalist.authorPageUrl && !journalist.twitter && !journalist.linkedin && (!journalist.otherLinks || journalist.otherLinks.length === 0)) {
      missing.push("Public links");
    }
    if (!journalist.country && !journalist.city && (!journalist.coverageRegions || journalist.coverageRegions.length === 0)) {
      missing.push(pendingProfile?.location?.country || pendingProfile?.location?.city ? "Location (pending verification)" : "Location");
    }
    if (!journalist.aliases || journalist.aliases.length === 0) {
      missing.push(pendingProfile?.aliases?.length ? "Aliases (pending verification)" : "Aliases");
    }
    if (!journalist.topicKeywords || journalist.topicKeywords.length === 0) {
      missing.push(pendingProfile?.topic_keywords?.length ? "Topic keywords (pending verification)" : "Topic keywords");
    }
    if (!journalist.languages || journalist.languages.length === 0) {
      missing.push(pendingProfile?.languages?.length ? "Languages (pending verification)" : "Languages");
    }
    if (!journalist.coverageRegions || journalist.coverageRegions.length === 0) {
      missing.push(pendingProfile?.coverage_regions?.length ? "Coverage regions (pending verification)" : "Coverage regions");
    }
    if (!journalist.contacts || journalist.contacts.length === 0) {
      missing.push("Contact details");
    }
    return missing;
  }, [journalist, pendingProfile]);

  const summaryText = useMemo(() => {
    if (!journalist) return "No verified bio yet.";
    if (journalist.journeySummary) return journalist.journeySummary;
    if (journalist.bioSummary) return journalist.bioSummary;
    if (pendingProfile?.journey_summary) {
      return `${pendingProfile.journey_summary} (pending verification)`;
    }
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
    const existingOtherLinks = journalist.otherLinks ?? [];
    const pendingOtherLinks = pendingProfile?.public_links?.other ?? [];
    const allOtherLinks = Array.from(new Set([...existingOtherLinks, ...pendingOtherLinks].filter(Boolean)));
    allOtherLinks.forEach((link) => {
      if (!link) return;
      const pending = !existingOtherLinks.includes(link);
      links.push({ label: "Other link", value: link, pending });
    });
    return links;
  }, [journalist, pendingProfile]);

  const profileDetails = useMemo(() => {
    if (!journalist) return [];
    const entries: Array<{ label: string; value: string; pending?: boolean }> = [];
    const beats = journalist.beats?.length
      ? journalist.beats.join(", ")
      : pendingProfile?.beats?.length
        ? pendingProfile.beats.join(", ")
        : null;
    if (beats) {
      entries.push({ label: "Beats", value: beats, pending: !journalist.beats?.length });
    }
    const aliases = journalist.aliases?.length
      ? journalist.aliases.join(", ")
      : pendingProfile?.aliases && Array.isArray(pendingProfile.aliases)
        ? pendingProfile.aliases.join(", ")
        : null;
    if (aliases) {
      entries.push({ label: "Aliases", value: aliases, pending: !journalist.aliases?.length });
    }
    const publicationAliases = journalist.publicationAliases?.length
      ? journalist.publicationAliases.join(", ")
      : pendingProfile?.publication_aliases && Array.isArray(pendingProfile.publication_aliases)
        ? pendingProfile.publication_aliases.join(", ")
        : null;
    if (publicationAliases) {
      entries.push({
        label: "Publication aliases",
        value: publicationAliases,
        pending: !journalist.publicationAliases?.length,
      });
    }
    const topicKeywords = journalist.topicKeywords?.length
      ? journalist.topicKeywords.join(", ")
      : pendingProfile?.topic_keywords && Array.isArray(pendingProfile.topic_keywords)
        ? pendingProfile.topic_keywords.join(", ")
        : null;
    if (topicKeywords) {
      entries.push({
        label: "Topic keywords",
        value: topicKeywords,
        pending: !journalist.topicKeywords?.length,
      });
    }
    const languages = journalist.languages?.length
      ? journalist.languages.join(", ")
      : pendingProfile?.languages && Array.isArray(pendingProfile.languages)
        ? pendingProfile.languages.join(", ")
        : null;
    if (languages) {
      entries.push({ label: "Languages", value: languages, pending: !journalist.languages?.length });
    }
    const coverageRegions = journalist.coverageRegions?.length
      ? journalist.coverageRegions.join(", ")
      : pendingProfile?.coverage_regions && Array.isArray(pendingProfile.coverage_regions)
        ? pendingProfile.coverage_regions.join(", ")
        : null;
    if (coverageRegions) {
      entries.push({
        label: "Coverage regions",
        value: coverageRegions,
        pending: !journalist.coverageRegions?.length,
      });
    }
    return entries;
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

  const pendingContactSummary = useMemo(() => {
    const pending = pendingProfile?.contacts;
    if (!pending) {
      return { emails: [] as string[], inferred: [] as string[], phones: [] as string[] };
    }
    const existingEmails = new Set(
      (journalist?.contacts ?? [])
        .map((contact) => contact.email?.trim().toLowerCase())
        .filter((value): value is string => Boolean(value))
    );
    const existingPhones = new Set(
      (journalist?.contacts ?? [])
        .map((contact) => (contact.phone ? contact.phone.replace(/[^0-9]/g, "") : ""))
        .filter(Boolean)
    );
    const emails: string[] = [];
    const inferred: string[] = [];
    const phones: string[] = [];

    const addEmail = (value: string | undefined, target: string[]) => {
      if (!value) return;
      const normalized = value.trim().toLowerCase();
      if (!normalized || existingEmails.has(normalized)) return;
      if (!target.some((item) => item.toLowerCase() === normalized)) {
        target.push(value.trim());
      }
    };

    const addPhone = (value: string | undefined) => {
      if (!value) return;
      const normalized = value.replace(/[^0-9]/g, "");
      if (!normalized || existingPhones.has(normalized)) return;
      if (!phones.some((item) => item.replace(/[^0-9]/g, "") === normalized)) {
        phones.push(value.trim());
      }
    };

    (pending.emails ?? []).forEach((email) => addEmail(email, emails));
    (pending.inferred_emails ?? []).forEach((email) => addEmail(email, inferred));
    (pending.phones ?? []).forEach((phone) => addPhone(phone));

    return { emails, inferred, phones };
  }, [journalist, pendingProfile]);

  const handleSearchWeb = () => {
    if (!journalist) return;
    const query = `"${journalist.fullName}" "${journalist.publicationName ?? ""}" journalist`;
    window.open(`https://www.google.com/search?q=${encodeURIComponent(query)}`, "_blank");
  };

  const handleSearchArticles = () => {
    if (!journalist) return;
    router.push(`/search?journalistId=${journalist.id}&journalistName=${encodeURIComponent(journalist.fullName)}`);
  };

  const toggleEditMode = () => {
    if (!isAdmin) return;
    if (editMode) {
      if (journalist) {
        setDraft(buildDraft(journalist));
      }
      setSaveNotice(null);
      setContactError(null);
      setContactNotice(null);
      setContactEmail("");
      setContactPhone("");
    }
    setEditMode(!editMode);
  };

  const handleSave = async () => {
    if (!draft || !journalist || !canEdit) return;
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
        aliases: draft.aliases
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        publicationAliases: draft.publicationAliases
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        topicKeywords: draft.topicKeywords
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        languages: draft.languages
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        coverageRegions: draft.coverageRegions
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        otherLinks: draft.otherLinks
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        country: draft.country,
        city: draft.city,
        journeySummary: draft.journeySummary,
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
      setDraft(buildDraft(updated));
      setSaveNotice("Journalist profile updated.");
      setEditMode(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to save journalist updates.");
    } finally {
      setSaving(false);
    }
  };

  const handleAddContact = async () => {
    if (!journalist) return;
    if (!canEdit) {
      setContactError("Enable edit mode to add contact details.");
      return;
    }
    if (!contactEmail.trim() && !contactPhone.trim()) {
      setContactError("Enter an email or phone number.");
      return;
    }
    setContactSaving(true);
    setContactError(null);
    setContactNotice(null);
    try {
      const payload = {
        email: contactEmail.trim() || null,
        phone: contactPhone.trim() || null,
      };
      const contact = await apiFetch<JournalistContact>(`/api/journalists/${journalist.id}/contacts`, {
        method: "POST",
        body: JSON.stringify(payload),
      });
      setJournalist((prev) => {
        if (!prev) return prev;
        const existing = prev.contacts ?? [];
        return { ...prev, contacts: [...existing, contact] };
      });
      setContactEmail("");
      setContactPhone("");
      setContactNotice("Contact added.");
    } catch (err) {
      setContactError(err instanceof Error ? err.message : "Unable to add contact.");
    } finally {
      setContactSaving(false);
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
    if (pendingProfile.aliases) {
      const aliasesValue = Array.isArray(pendingProfile.aliases)
        ? pendingProfile.aliases.join(", ")
        : String(pendingProfile.aliases);
      if (aliasesValue.trim()) {
        entries.push({ label: "Aliases", value: aliasesValue });
      }
    }
    if (pendingProfile.publication_name) entries.push({ label: "Publication", value: pendingProfile.publication_name });
    if (pendingProfile.publication_domain) entries.push({ label: "Publication domain", value: pendingProfile.publication_domain });
    if (pendingProfile.publication_aliases) {
      const publicationAliasesValue = Array.isArray(pendingProfile.publication_aliases)
        ? pendingProfile.publication_aliases.join(", ")
        : String(pendingProfile.publication_aliases);
      if (publicationAliasesValue.trim()) {
        entries.push({ label: "Publication aliases", value: publicationAliasesValue });
      }
    }
    if (pendingProfile.designation) entries.push({ label: "Designation", value: pendingProfile.designation });
    if (pendingProfile.beats) {
      const beatsValue = Array.isArray(pendingProfile.beats)
        ? pendingProfile.beats.join(", ")
        : String(pendingProfile.beats);
      if (beatsValue.trim()) {
        entries.push({ label: "Beats", value: beatsValue });
      }
    }
    if (pendingProfile.topic_keywords) {
      const topicsValue = Array.isArray(pendingProfile.topic_keywords)
        ? pendingProfile.topic_keywords.join(", ")
        : String(pendingProfile.topic_keywords);
      if (topicsValue.trim()) {
        entries.push({ label: "Topic keywords", value: topicsValue });
      }
    }
    if (pendingProfile.languages) {
      const languagesValue = Array.isArray(pendingProfile.languages)
        ? pendingProfile.languages.join(", ")
        : String(pendingProfile.languages);
      if (languagesValue.trim()) {
        entries.push({ label: "Languages", value: languagesValue });
      }
    }
    if (pendingProfile.coverage_regions) {
      const regionsValue = Array.isArray(pendingProfile.coverage_regions)
        ? pendingProfile.coverage_regions.join(", ")
        : String(pendingProfile.coverage_regions);
      if (regionsValue.trim()) {
        entries.push({ label: "Coverage regions", value: regionsValue });
      }
    }
    if (pendingProfile.location?.country) entries.push({ label: "Country", value: pendingProfile.location.country });
    if (pendingProfile.location?.city) entries.push({ label: "City", value: pendingProfile.location.city });
    if (pendingProfile.public_links?.author_page) entries.push({ label: "Author page", value: pendingProfile.public_links.author_page });
    if (pendingProfile.public_links?.twitter) entries.push({ label: "Twitter", value: pendingProfile.public_links.twitter });
    if (pendingProfile.public_links?.linkedin) entries.push({ label: "LinkedIn", value: pendingProfile.public_links.linkedin });
    if (pendingProfile.public_links?.other) {
      const otherLinksValue = Array.isArray(pendingProfile.public_links.other)
        ? pendingProfile.public_links.other.join(", ")
        : String(pendingProfile.public_links.other);
      if (otherLinksValue.trim()) {
        entries.push({ label: "Other links", value: otherLinksValue });
      }
    }
    if (pendingProfile.contacts?.emails?.length) {
      entries.push({ label: "Contact emails", value: pendingProfile.contacts.emails.join(", ") });
    }
    if (pendingProfile.contacts?.inferred_emails?.length) {
      entries.push({
        label: "Inferred emails",
        value: pendingProfile.contacts.inferred_emails.join(", "),
      });
    }
    if (pendingProfile.contacts?.phones?.length) {
      entries.push({ label: "Contact phones", value: pendingProfile.contacts.phones.join(", ") });
    }
    if (pendingProfile.bio_summary) entries.push({ label: "Bio summary", value: pendingProfile.bio_summary });
    if (pendingProfile.journey_summary) entries.push({ label: "Journey summary", value: pendingProfile.journey_summary });
    return entries;
  }, [pendingProfile]);
  const hasPendingContacts =
    pendingContactSummary.emails.length > 0
    || pendingContactSummary.inferred.length > 0
    || pendingContactSummary.phones.length > 0;

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
            {profileDetails.length > 0 && (
              <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-3 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <h2 className="text-sm font-semibold text-slate-700">Profile details</h2>
                <div className="space-y-2 text-xs text-slate-600">
                  {profileDetails.map((detail) => (
                    <div key={`${detail.label}-${detail.value}`} className="space-y-1">
                      <div className="flex items-center justify-between">
                        <span className="text-[10px] uppercase tracking-[0.2em] text-slate-500">
                          {detail.label}
                        </span>
                        {detail.pending && (
                          <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-700">
                            Pending
                          </span>
                        )}
                      </div>
                      <div className="rounded-lg border border-slate-200/70 bg-white px-3 py-2 text-xs text-slate-700">
                        {detail.value}
                      </div>
                    </div>
                  ))}
                </div>
              </div>
            )}
            {journalist && (
              <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-3 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <h2 className="text-sm font-semibold text-slate-700">Contact details</h2>
                {isAdmin && (
                  <div className="space-y-2 rounded-xl border border-slate-200/70 bg-white px-3 py-3 text-xs text-slate-600">
                    <div className="grid gap-2 md:grid-cols-2">
                      <input
                        className={contactFieldClass}
                        placeholder="Add email"
                        value={contactEmail}
                        onChange={(event) => setContactEmail(event.target.value)}
                        readOnly={!canEdit}
                      />
                      <input
                        className={contactFieldClass}
                        placeholder="Add phone"
                        value={contactPhone}
                        onChange={(event) => setContactPhone(event.target.value)}
                        readOnly={!canEdit}
                      />
                    </div>
                    <button
                      onClick={handleAddContact}
                      disabled={!canEdit || contactSaving}
                      className="w-full rounded-lg border border-slate-200 px-3 py-2 text-xs font-semibold text-slate-700 hover:border-cyan-300 hover:bg-slate-50 disabled:opacity-60"
                    >
                      {contactSaving ? "Saving..." : "Add contact"}
                    </button>
                    {!editMode && (
                      <p className="text-[11px] text-slate-500">
                        Click Edit in Admin updates to add or verify contact details.
                      </p>
                    )}
                    {contactNotice && (
                      <p className="text-[11px] text-emerald-600">{contactNotice}</p>
                    )}
                    {contactError && (
                      <p className="text-[11px] text-rose-600">{contactError}</p>
                    )}
                  </div>
                )}
                {hasPendingContacts && (
                  <div className="rounded-xl border border-amber-200/70 bg-amber-50/70 px-3 py-2 text-xs text-amber-800">
                    <p className="text-[10px] uppercase tracking-[0.2em] text-amber-700">
                      Pending from enrichment
                    </p>
                    <div className="mt-2 flex flex-wrap gap-2">
                      {pendingContactSummary.emails.map((email) => (
                        <span
                          key={`pending-email-${email}`}
                          className="rounded-full border border-amber-200 bg-white px-2 py-1 text-[11px]"
                        >
                          Email · {email}
                        </span>
                      ))}
                      {pendingContactSummary.inferred.map((email) => (
                        <span
                          key={`pending-inferred-${email}`}
                          className="rounded-full border border-amber-200 bg-white px-2 py-1 text-[11px]"
                        >
                          Inferred email · {email}
                        </span>
                      ))}
                      {pendingContactSummary.phones.map((phone) => (
                        <span
                          key={`pending-phone-${phone}`}
                          className="rounded-full border border-amber-200 bg-white px-2 py-1 text-[11px]"
                        >
                          Phone · {phone}
                        </span>
                      ))}
                    </div>
                  </div>
                )}
                {contactSummary.count === 0 ? (
                  <p className="text-xs text-slate-500">No contact details captured yet.</p>
                ) : (
                  <>
                    <p className="text-xs text-slate-500">
                      {contactSummary.emails} emails · {contactSummary.phones} phones
                    </p>
                    <div className="space-y-2 text-xs text-slate-600">
                      {journalist.contacts?.map((contact) => (
                        <div key={contact.id} className="rounded-xl border border-slate-200/70 bg-white px-3 py-2">
                          <div className="flex items-center justify-between text-[10px] uppercase tracking-[0.2em] text-slate-500">
                            <span>{contact.visibility.replace("_", " ")}</span>
                            <span>{contact.sourceType.replace("_", " ")}</span>
                          </div>
                          {contact.email && (
                            <div className="mt-1 flex flex-wrap items-center gap-2 text-sm text-slate-700">
                              <span>{contact.email}</span>
                              {contact.sourceType === "INFERRED" && (
                                <span className="rounded-full bg-amber-100 px-2 py-0.5 text-[10px] font-semibold text-amber-700">
                                  Inferred
                                </span>
                              )}
                            </div>
                          )}
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
                  </>
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
            {role === "ADMIN" && draft && (
              <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-6 space-y-4 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
                <div className="flex items-center justify-between">
                  <h2 className="text-sm font-semibold text-slate-700">Admin updates</h2>
                  <button
                    onClick={toggleEditMode}
                    className="rounded-full border border-slate-200 px-3 py-1 text-[11px] font-semibold text-slate-600 hover:border-cyan-300 hover:text-cyan-700"
                  >
                    {editMode ? "Cancel" : "Edit"}
                  </button>
                </div>
                {!editMode && (
                  <p className="text-xs text-slate-500">
                    Click Edit to unlock fields and apply changes.
                  </p>
                )}
                <div className="space-y-3 text-sm text-slate-600">
                  <div>
                    <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Full name</label>
                    <input
                      className={editFieldClass}
                      value={draft.fullName}
                      onChange={(event) => setDraft({ ...draft, fullName: event.target.value })}
                      readOnly={!canEdit}
                    />
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Publication</label>
                      <input
                        className={editFieldClass}
                        value={draft.publicationName}
                        onChange={(event) => setDraft({ ...draft, publicationName: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Publication domain</label>
                      <input
                        className={editFieldClass}
                        value={draft.publicationDomain}
                        onChange={(event) => setDraft({ ...draft, publicationDomain: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Designation</label>
                      <input
                        className={editFieldClass}
                        value={draft.designation}
                        onChange={(event) => setDraft({ ...draft, designation: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">LinkedIn</label>
                      <input
                        className={editFieldClass}
                        value={draft.linkedin}
                        onChange={(event) => setDraft({ ...draft, linkedin: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Twitter</label>
                      <input
                        className={editFieldClass}
                        value={draft.twitter}
                        onChange={(event) => setDraft({ ...draft, twitter: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Author page</label>
                      <input
                        className={editFieldClass}
                        value={draft.authorPageUrl}
                        onChange={(event) => setDraft({ ...draft, authorPageUrl: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div>
                    <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Beats (comma-separated)</label>
                    <input
                      className={editFieldClass}
                      value={draft.beats}
                      onChange={(event) => setDraft({ ...draft, beats: event.target.value })}
                      readOnly={!canEdit}
                    />
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Aliases (comma-separated)</label>
                      <input
                        className={editFieldClass}
                        value={draft.aliases}
                        onChange={(event) => setDraft({ ...draft, aliases: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Publication aliases</label>
                      <input
                        className={editFieldClass}
                        value={draft.publicationAliases}
                        onChange={(event) => setDraft({ ...draft, publicationAliases: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Topic keywords</label>
                      <input
                        className={editFieldClass}
                        value={draft.topicKeywords}
                        onChange={(event) => setDraft({ ...draft, topicKeywords: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Languages</label>
                      <input
                        className={editFieldClass}
                        value={draft.languages}
                        onChange={(event) => setDraft({ ...draft, languages: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Coverage regions</label>
                      <input
                        className={editFieldClass}
                        value={draft.coverageRegions}
                        onChange={(event) => setDraft({ ...draft, coverageRegions: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Other links</label>
                      <input
                        className={editFieldClass}
                        value={draft.otherLinks}
                        onChange={(event) => setDraft({ ...draft, otherLinks: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Country</label>
                      <input
                        className={editFieldClass}
                        value={draft.country}
                        onChange={(event) => setDraft({ ...draft, country: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">City</label>
                      <input
                        className={editFieldClass}
                        value={draft.city}
                        onChange={(event) => setDraft({ ...draft, city: event.target.value })}
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div className="grid gap-3 md:grid-cols-2">
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Verification</label>
                      <select
                        className={editFieldClass}
                        value={draft.verificationStatus}
                        onChange={(event) => setDraft({ ...draft, verificationStatus: event.target.value })}
                        disabled={!canEdit}
                      >
                        <option value="UNVERIFIED">Unverified</option>
                        <option value="VERIFIED">Verified</option>
                      </select>
                    </div>
                    <div>
                      <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Completeness</label>
                      <input
                        type="number"
                        className={editFieldClass}
                        value={draft.completenessScore}
                        onChange={(event) =>
                          setDraft({ ...draft, completenessScore: Number(event.target.value) })
                        }
                        readOnly={!canEdit}
                      />
                    </div>
                  </div>
                  <div>
                    <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Journey summary</label>
                    <textarea
                      rows={3}
                      className={editFieldClass}
                      value={draft.journeySummary}
                      onChange={(event) => setDraft({ ...draft, journeySummary: event.target.value })}
                      readOnly={!canEdit}
                    />
                  </div>
                  <div>
                    <label className="text-xs uppercase tracking-[0.2em] text-slate-500">Bio summary</label>
                    <textarea
                      rows={3}
                      className={editFieldClass}
                      value={draft.bioSummary}
                      onChange={(event) => setDraft({ ...draft, bioSummary: event.target.value })}
                      readOnly={!canEdit}
                    />
                  </div>
                  {saveNotice && (
                    <div className="rounded-xl border border-emerald-200 bg-emerald-50 px-3 py-2 text-xs text-emerald-700">
                      {saveNotice}
                    </div>
                  )}
                  <button
                    onClick={handleSave}
                    disabled={!canEdit || saving}
                    className="w-full rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 px-4 py-2 text-sm font-semibold text-slate-900 disabled:opacity-60"
                  >
                    <span className="inline-flex items-center gap-2">
                      {saving && (
                        <span className="h-3 w-3 animate-spin rounded-full border border-slate-200 border-t-transparent" />
                      )}
                      {saving ? "Saving..." : canEdit ? "Save updates" : "Edit to enable"}
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

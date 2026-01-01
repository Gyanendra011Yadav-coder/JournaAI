"use client";

import { useCallback, useEffect, useState } from "react";
import { useParams } from "next/navigation";
import Link from "next/link";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";
interface Article {
  id: number;
  title: string;
  authorRaw?: string | null;
  journalistId?: number | null;
  journalistName?: string | null;
  authorTaskStatus?: string | null;
  description: string | null;
  content: string | null;
  url: string;
  imageUrl: string | null;
  publishedAtUtc: string | null;
  sourceName: string | null;
  sourceUrl: string | null;
  beatName: string | null;
  category?: string | null;
  lensSource?: string;
  clientMatch?: boolean;
  providerType: string;
  status: string;
  internalPublishedAtUtc?: string | null;
}

interface AdminArticleForm {
  title: string;
  description: string;
  content: string;
  authorRaw: string;
  journalistId: string;
  journalistMatchConfidence: string;
  imageUrl: string;
  sourceName: string;
  sourceUrl: string;
  sourceCountry: string;
  category: string;
  publishedAt: string;
}

const DEFAULT_ADMIN_FORM: AdminArticleForm = {
  title: "",
  description: "",
  content: "",
  authorRaw: "",
  journalistId: "",
  journalistMatchConfidence: "100",
  imageUrl: "",
  sourceName: "",
  sourceUrl: "",
  sourceCountry: "",
  category: "",
  publishedAt: "",
};

function formatPublishedAtInput(value?: string | null): string {
  if (!value) {
    return "";
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return "";
  }
  const offsetMs = parsed.getTimezoneOffset() * 60000;
  const local = new Date(parsed.getTime() - offsetMs);
  return local.toISOString().slice(0, 16);
}

function toIsoString(value: string): string | undefined {
  if (!value) {
    return undefined;
  }
  const parsed = new Date(value);
  if (Number.isNaN(parsed.getTime())) {
    return undefined;
  }
  return parsed.toISOString();
}

function buildAdminForm(article: Article): AdminArticleForm {
  return {
    title: article.title ?? "",
    description: article.description ?? "",
    content: article.content ?? "",
    authorRaw: article.authorRaw ?? "",
    journalistId: article.journalistId ? String(article.journalistId) : "",
    journalistMatchConfidence: "100",
    imageUrl: article.imageUrl ?? "",
    sourceName: article.sourceName ?? "",
    sourceUrl: article.sourceUrl ?? "",
    sourceCountry: article.sourceCountry ?? "",
    category: article.category ?? "",
    publishedAt: formatPublishedAtInput(article.publishedAtUtc ?? article.internalPublishedAtUtc),
  };
}

function parseNumberInput(value: string): number | undefined {
  if (!value) {
    return undefined;
  }
  const parsed = Number(value);
  return Number.isFinite(parsed) ? parsed : undefined;
}

export default function ArticleDetailPage() {
  const params = useParams();
  const [article, setArticle] = useState<Article | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [pinning, setPinning] = useState(false);
  const [formState, setFormState] = useState<AdminArticleForm>(DEFAULT_ADMIN_FORM);
  const [userRole, setUserRole] = useState<string | null>(null);
  const [manualUpdating, setManualUpdating] = useState(false);
  const [manualError, setManualError] = useState<string | null>(null);
  const [manualStatus, setManualStatus] = useState<string | null>(null);

  const refreshArticle = useCallback(async () => {
    if (!params.id) {
      return;
    }
    try {
      const data = await apiFetch<Article>(`/api/articles/${params.id}`);
      setArticle(data);
      setError(null);
      setFormState(buildAdminForm(data));
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load article.");
    }
  }, [params.id]);

  useEffect(() => {
    refreshArticle();
  }, [refreshArticle]);

  useEffect(() => {
    let active = true;
    apiFetch<{ role: string }>("/api/auth/me")
      .then((data) => {
        if (active) {
          setUserRole(data.role);
        }
      })
      .catch(() => {
        if (active) {
          setUserRole(null);
        }
      });
    return () => {
      active = false;
    };
  }, []);

  const isAdmin = userRole === "ADMIN";
  const updateFormField = (field: keyof AdminArticleForm, value: string) => {
    setFormState((prev) => ({ ...prev, [field]: value }));
  };

  const handleManualUpdate = async () => {
    if (!article) {
      return;
    }
    setManualUpdating(true);
    setManualError(null);
    setManualStatus(null);
    const payload = {
      title: formState.title,
      description: formState.description,
      content: formState.content,
      imageUrl: formState.imageUrl,
      sourceName: formState.sourceName,
      sourceUrl: formState.sourceUrl,
      sourceCountry: formState.sourceCountry,
      category: formState.category,
      publishedAtUtc: toIsoString(formState.publishedAt),
      authorRaw: formState.authorRaw,
      journalistId: parseNumberInput(formState.journalistId),
      journalistMatchConfidence: parseNumberInput(formState.journalistMatchConfidence),
    };
    try {
      const updated = await apiFetch<Article>(`/api/admin/articles/${article.id}`, {
        method: "PUT",
        body: JSON.stringify(payload),
      });
      setArticle(updated);
      setFormState(buildAdminForm(updated));
      setManualStatus("Manual updates saved.");
      setError(null);
    } catch (err) {
      setManualError(err instanceof Error ? err.message : "Unable to apply manual updates.");
    } finally {
      setManualUpdating(false);
    }
  };

  if (!article) {
    return <div>Loading...</div>;
  }

  const externalUrl = article.url || article.sourceUrl;
  const authorLabel = article.journalistName ?? article.authorRaw;
  const authorStatus = article.authorTaskStatus;

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-semibold">{article.title}</h1>
        <p className="text-slate-600">
          {article.sourceName ?? "Unknown source"} ·{" "}
          {article.publishedAtUtc ? new Date(article.publishedAtUtc).toLocaleString() : "Unknown date"}
        </p>
        <p className="text-sm text-slate-600">
          {article.journalistId && authorLabel ? (
            <Link href={`/journalists/${article.journalistId}`} className="text-cyan-700 hover:underline">
              {authorLabel}
            </Link>
          ) : authorLabel ? (
            <span>{authorLabel}</span>
          ) : authorStatus === "PENDING" || authorStatus === "RUNNING" ? (
            <span className="inline-flex items-center gap-2 text-xs text-slate-500">
              <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />
              Extracting author...
            </span>
          ) : authorStatus === "FAILED" ? (
            <span className="text-xs text-rose-600">Author extraction failed</span>
          ) : authorStatus === "NEEDS_REVIEW" ? (
            <span className="text-xs text-amber-600">Author needs review</span>
          ) : (
            <span>Unknown author</span>
          )}
        </p>
        <p className="text-xs text-cyan-600">Provider: {article.providerType}</p>
      </header>
      <ErrorBanner message={error} />
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 space-y-4 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
        {article.imageUrl && (
          <img src={article.imageUrl} alt={article.title} className="w-full rounded-xl border border-slate-200" />
        )}
        <div>
          <p className="text-sm text-slate-600">Beat</p>
          <p className="text-slate-700 mt-2">{article.beatName}</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs">
          {article.category && (
            <span className="rounded-full bg-slate-100 px-2 py-1 text-slate-600">
              Category: {article.category}
            </span>
          )}
          {article.lensSource && (
            <span className="rounded-full bg-slate-100 px-2 py-1 text-slate-600">Lens: {article.lensSource}</span>
          )}
          {article.clientMatch && (
            <span className="rounded-full bg-cyan-50 px-2 py-1 text-cyan-700">Client match</span>
          )}
        </div>
        {article.description && (
          <div>
            <p className="text-sm text-slate-600">Description</p>
            <p className="text-slate-700 mt-2">{article.description}</p>
          </div>
        )}
        {article.content && (
          <div>
            <p className="text-sm text-slate-600">Content</p>
            <p className="text-slate-700 mt-2 whitespace-pre-line">{article.content}</p>
          </div>
        )}
        <div className="flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-slate-100 px-2 py-1 text-slate-600">Status: {article.status}</span>
          {article.internalPublishedAtUtc && (
            <span className="rounded-full bg-emerald-100 px-2 py-1 text-emerald-700">
              Published {new Date(article.internalPublishedAtUtc).toLocaleString()}
            </span>
          )}
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={async () => {
              setSaving(true);
              setError(null);
              try {
                await apiFetch(`/api/articles/${article.id}/save`, { method: "POST" });
              } catch (err) {
                setError(err instanceof Error ? err.message : "Unable to save article.");
              } finally {
                setSaving(false);
              }
            }}
            disabled={saving}
            className="rounded-lg border border-slate-200 px-4 py-2 text-sm text-slate-700 hover:border-cyan-500/60 disabled:opacity-60"
          >
            <span className="inline-flex items-center gap-2">
              {saving && <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />}
              Save
            </span>
          </button>
          <button
            onClick={async () => {
              setPinning(true);
              setError(null);
              try {
                await apiFetch(`/api/articles/${article.id}/pin`, { method: "POST" });
              } catch (err) {
                setError(err instanceof Error ? err.message : "Unable to pin article.");
              } finally {
                setPinning(false);
              }
            }}
            disabled={pinning}
            className="rounded-lg border border-emerald-300/70 bg-emerald-50 px-4 py-2 text-sm text-emerald-700 disabled:opacity-60"
          >
            <span className="inline-flex items-center gap-2">
              {pinning && (
                <span className="h-3 w-3 animate-spin rounded-full border border-emerald-500 border-t-transparent" />
              )}
              Pin
            </span>
          </button>
        </div>
        {externalUrl && (
          <a href={externalUrl} className="text-cyan-600 hover:underline" target="_blank" rel="noreferrer">
            View original article
          </a>
        )}
      </div>
      {isAdmin && (
        <section className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-xs uppercase tracking-[0.3em] text-slate-500">Manual enrichment</p>
              <h2 className="text-xl font-semibold text-slate-900">Review article details</h2>
            </div>
            <p className="text-sm text-slate-500">Fill any missing enrichment data before rerunning.</p>
          </div>
          {manualError && (
            <div className="mt-3">
              <ErrorBanner message={manualError} />
            </div>
          )}
          {manualStatus && <p className="mt-2 text-sm text-emerald-700">{manualStatus}</p>}
          <div className="mt-4 grid gap-4 md:grid-cols-2">
            <label className="space-y-1 text-xs text-slate-500">
              Title
              <input
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.title}
                onChange={(event) => updateFormField("title", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Author
              <input
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.authorRaw}
                onChange={(event) => updateFormField("authorRaw", event.target.value)}
              />
            </label>
            <label className="md:col-span-2 space-y-1 text-xs text-slate-500">
              Description
              <textarea
                rows={3}
                className="w-full rounded-2xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.description}
                onChange={(event) => updateFormField("description", event.target.value)}
              />
            </label>
            <label className="md:col-span-2 space-y-1 text-xs text-slate-500">
              Content
              <textarea
                rows={4}
                className="w-full rounded-2xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.content}
                onChange={(event) => updateFormField("content", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Journalist ID
              <input
                type="number"
                min={0}
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.journalistId}
                onChange={(event) => updateFormField("journalistId", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Confidence
              <input
                type="number"
                min={0}
                max={100}
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.journalistMatchConfidence}
                onChange={(event) => updateFormField("journalistMatchConfidence", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Source name
              <input
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.sourceName}
                onChange={(event) => updateFormField("sourceName", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Source URL
              <input
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.sourceUrl}
                onChange={(event) => updateFormField("sourceUrl", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Source country
              <input
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.sourceCountry}
                onChange={(event) => updateFormField("sourceCountry", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Category
              <input
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.category}
                onChange={(event) => updateFormField("category", event.target.value)}
              />
            </label>
            <label className="space-y-1 text-xs text-slate-500">
              Published at
              <input
                type="datetime-local"
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.publishedAt}
                onChange={(event) => updateFormField("publishedAt", event.target.value)}
              />
            </label>
            <label className="md:col-span-2 space-y-1 text-xs text-slate-500">
              Image URL
              <input
                className="w-full rounded-xl border border-slate-200/70 bg-white/80 px-3 py-2 text-sm text-slate-700 focus:border-cyan-500 focus:outline-none"
                value={formState.imageUrl}
                onChange={(event) => updateFormField("imageUrl", event.target.value)}
              />
            </label>
          </div>
          <div className="mt-6 flex justify-end">
            <button
              onClick={handleManualUpdate}
              disabled={manualUpdating}
              className="rounded-xl border border-cyan-300/70 bg-cyan-50 px-5 py-2 text-sm font-semibold text-cyan-700 hover:border-cyan-400 hover:bg-cyan-100 disabled:opacity-60"
            >
              {manualUpdating ? "Saving..." : "Apply manual updates"}
            </button>
          </div>
        </section>
      )}
    </div>
  );
}

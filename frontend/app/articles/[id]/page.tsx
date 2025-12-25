"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";
interface Article {
  id: number;
  title: string;
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

export default function ArticleDetailPage() {
  const params = useParams();
  const [article, setArticle] = useState<Article | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [pinning, setPinning] = useState(false);

  useEffect(() => {
    if (!params.id) return;
    apiFetch<Article>(`/api/articles/${params.id}`)
      .then((data) => {
        setArticle(data);
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load article."));
  }, [params.id]);

  if (!article) {
    return <div>Loading...</div>;
  }

  const externalUrl = article.url || article.sourceUrl;

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-3xl font-semibold">{article.title}</h1>
        <p className="text-slate-600">
          {article.sourceName ?? "Unknown source"} ·{" "}
          {article.publishedAtUtc ? new Date(article.publishedAtUtc).toLocaleString() : "Unknown date"}
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
    </div>
  );
}

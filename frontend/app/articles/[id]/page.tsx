"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { apiFetch } from "../../../lib/api";
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

  useEffect(() => {
    if (!params.id) return;
    apiFetch<Article>(`/api/articles/${params.id}`).then((data) => {
      setArticle(data);
    });
  }, [params.id]);

  if (!article) {
    return <div>Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">{article.title}</h1>
        <p className="text-slate-400">
          {article.sourceName ?? "Unknown source"} ·{" "}
          {article.publishedAtUtc ? new Date(article.publishedAtUtc).toLocaleString() : "Unknown date"}
        </p>
        <p className="text-xs text-cyan-300">Provider: {article.providerType}</p>
      </header>
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
        {article.imageUrl && (
          <img src={article.imageUrl} alt={article.title} className="w-full rounded-xl border border-slate-800" />
        )}
        <div>
          <p className="text-sm text-slate-400">Beat</p>
          <p className="text-slate-200 mt-2">{article.beatName}</p>
        </div>
        <div className="flex flex-wrap gap-2 text-xs">
          {article.category && <span className="rounded-full bg-slate-800 px-2 py-1">Category: {article.category}</span>}
          {article.lensSource && <span className="rounded-full bg-slate-800 px-2 py-1">Lens: {article.lensSource}</span>}
          {article.clientMatch && <span className="rounded-full bg-cyan-500/20 px-2 py-1 text-cyan-200">Client match</span>}
        </div>
        {article.description && (
          <div>
            <p className="text-sm text-slate-400">Description</p>
            <p className="text-slate-200 mt-2">{article.description}</p>
          </div>
        )}
        {article.content && (
          <div>
            <p className="text-sm text-slate-400">Content</p>
            <p className="text-slate-200 mt-2 whitespace-pre-line">{article.content}</p>
          </div>
        )}
        <div className="flex flex-wrap gap-2 text-xs">
          <span className="rounded-full bg-slate-800 px-2 py-1">Status: {article.status}</span>
          {article.internalPublishedAtUtc && (
            <span className="rounded-full bg-emerald-500/20 px-2 py-1 text-emerald-200">
              Published {new Date(article.internalPublishedAtUtc).toLocaleString()}
            </span>
          )}
        </div>
        <div className="flex flex-wrap gap-3">
          <button
            onClick={() => apiFetch(`/api/articles/${article.id}/save`, { method: "POST" })}
            className="rounded-lg border border-slate-700 px-4 py-2 text-sm text-slate-200 hover:border-cyan-500/60"
          >
            Save
          </button>
          <button
            onClick={() => apiFetch(`/api/articles/${article.id}/pin`, { method: "POST" })}
            className="rounded-lg border border-emerald-500/60 bg-emerald-500/10 px-4 py-2 text-sm text-emerald-100"
          >
            Pin
          </button>
        </div>
        <a href={article.sourceUrl ?? article.url} className="text-cyan-300 hover:underline" target="_blank">
          View original article
        </a>
      </div>
    </div>
  );
}

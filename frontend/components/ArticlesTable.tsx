"use client";

import Link from "next/link";
import { useState } from "react";
import { apiFetch } from "../lib/api";

interface Article {
  id: number;
  title: string;
  authorRaw?: string | null;
  journalistName?: string | null;
  authorTaskStatus?: string | null;
  sourceName: string | null;
  publishedAtUtc: string | null;
  status: string;
  beatName: string | null;
}

interface ArticlesTableProps {
  articles: Article[];
}

export function ArticlesTable({ articles }: ArticlesTableProps) {
  const [savingId, setSavingId] = useState<number | null>(null);

  const renderAuthor = (article: Article) => {
    if (article.journalistName) {
      return article.journalistName;
    }
    if (article.authorRaw) {
      return article.authorRaw;
    }
    if (article.authorTaskStatus === "PENDING" || article.authorTaskStatus === "RUNNING") {
      return (
        <span className="inline-flex items-center gap-2 text-xs text-slate-500">
          <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />
          Extracting...
        </span>
      );
    }
    if (article.authorTaskStatus === "FAILED") {
      return <span className="text-xs text-rose-600">Failed</span>;
    }
    if (article.authorTaskStatus === "NEEDS_REVIEW") {
      return <span className="text-xs text-amber-600">Needs review</span>;
    }
    return "—";
  };

  const handleSave = async (id: number) => {
    setSavingId(id);
    try {
      await apiFetch(`/api/articles/${id}/save`, { method: "POST" });
    } catch {
      // Errors are surfaced via the global API error banner.
    } finally {
      setSavingId(null);
    }
  };

  return (
    <div className="rounded-2xl border border-slate-200/70 bg-white/90 shadow-[0_18px_40px_-30px_rgba(15,23,42,0.35)] overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-slate-50 text-slate-600">
          <tr>
            <th className="text-left p-3">Headline</th>
            <th className="text-left p-3">Source</th>
            <th className="text-left p-3">Author</th>
            <th className="text-left p-3">Beat</th>
            <th className="text-left p-3">Published</th>
            <th className="text-left p-3">Status</th>
            <th className="text-left p-3">Save</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-200">
          {articles.map((article) => (
            <tr key={article.id} className="hover:bg-slate-50">
              <td className="p-3">
                <Link href={`/articles/${article.id}`} className="text-slate-900 hover:text-cyan-700 hover:underline">
                  {article.title}
                </Link>
              </td>
              <td className="p-3">{article.sourceName ?? "Unknown"}</td>
              <td className="p-3">{renderAuthor(article)}</td>
              <td className="p-3">{article.beatName ?? "—"}</td>
              <td className="p-3">
                {article.publishedAtUtc ? new Date(article.publishedAtUtc).toLocaleDateString() : "—"}
              </td>
              <td className="p-3">
                <span
                  className={`rounded-full px-2 py-1 text-xs ${
                    article.status === "PUBLISHED"
                      ? "bg-emerald-100 text-emerald-700"
                      : "bg-slate-100 text-slate-600"
                  }`}
                >
                  {article.status}
                </span>
              </td>
              <td className="p-3">
                <button
                  onClick={() => handleSave(article.id)}
                  disabled={savingId === article.id}
                  className="rounded-lg border border-slate-200 px-3 py-1 text-xs text-slate-700 hover:border-cyan-400/60 hover:text-cyan-700 disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {savingId === article.id && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />
                    )}
                    Save
                  </span>
                </button>
              </td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

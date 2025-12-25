"use client";

import Link from "next/link";
import { useState } from "react";
import { apiFetch } from "../lib/api";

interface Article {
  id: number;
  title: string;
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
    <div className="border border-slate-800 rounded-xl overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-slate-900 text-slate-300">
          <tr>
            <th className="text-left p-3">Headline</th>
            <th className="text-left p-3">Source</th>
            <th className="text-left p-3">Beat</th>
            <th className="text-left p-3">Published</th>
            <th className="text-left p-3">Status</th>
            <th className="text-left p-3">Save</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800">
          {articles.map((article) => (
            <tr key={article.id} className="hover:bg-slate-900/50">
              <td className="p-3">
                <Link href={`/articles/${article.id}`} className="text-cyan-300 hover:underline">
                  {article.title}
                </Link>
              </td>
              <td className="p-3">{article.sourceName ?? "Unknown"}</td>
              <td className="p-3">{article.beatName ?? "—"}</td>
              <td className="p-3">
                {article.publishedAtUtc ? new Date(article.publishedAtUtc).toLocaleDateString() : "—"}
              </td>
              <td className="p-3">
                <span
                  className={`rounded-full px-2 py-1 text-xs ${
                    article.status === "PUBLISHED"
                      ? "bg-emerald-500/20 text-emerald-200"
                      : "bg-slate-800 text-slate-300"
                  }`}
                >
                  {article.status}
                </span>
              </td>
              <td className="p-3">
                <button
                  onClick={() => handleSave(article.id)}
                  disabled={savingId === article.id}
                  className="rounded-lg border border-slate-700 px-3 py-1 text-xs text-slate-200 hover:border-cyan-500/60 disabled:opacity-60"
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

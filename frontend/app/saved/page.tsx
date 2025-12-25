"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";
import Link from "next/link";
import { ErrorBanner } from "../../components/ErrorBanner";

interface SavedArticle {
  articleId: number;
  pinned: boolean;
  savedAt?: string;
  note?: string | null;
  article: {
    id: number;
    title: string;
    sourceName?: string | null;
    publishedAtUtc?: string | null;
    beatName?: string | null;
    status?: string;
  };
}

export default function SavedPage() {
  const [items, setItems] = useState<SavedArticle[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<SavedArticle[]>("/api/saved-articles")
      .then((data) => {
        const sorted = [...data].sort((a, b) => Number(b.pinned) - Number(a.pinned));
        setItems(sorted);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load saved articles."));
  }, []);

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Saved Articles</p>
        <h1 className="text-2xl font-semibold">Pinned coverage</h1>
        <p className="text-slate-400">Review pinned highlights and saved clips.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        {items.length === 0 && <p className="text-sm text-slate-400">No saved articles yet.</p>}
        <div className="space-y-3">
          {items.map((item) => (
            <div
              key={item.articleId}
              className="flex flex-wrap items-center justify-between gap-4 rounded-xl border border-slate-800/80 bg-slate-950/60 p-4"
            >
              <div>
                <Link href={`/articles/${item.articleId}`} className="text-cyan-300 hover:underline">
                  {item.article.title}
                </Link>
                <p className="text-xs text-slate-400">
                  {item.article.beatName ?? "Trending"} ·{" "}
                  {item.article.publishedAtUtc
                    ? new Date(item.article.publishedAtUtc).toLocaleString()
                    : "Unknown date"}
                </p>
              </div>
              <div className="flex items-center gap-2 text-xs">
                {item.pinned && <span className="rounded-full bg-emerald-500/20 px-2 py-1 text-emerald-200">Pinned</span>}
                <button
                  onClick={() =>
                    apiFetch(`/api/articles/${item.articleId}/pin?pinned=${!item.pinned}`, { method: "POST" }).then(
                      () =>
                        setItems((prev) =>
                          prev.map((entry) =>
                            entry.articleId === item.articleId ? { ...entry, pinned: !item.pinned } : entry
                          )
                        )
                    )
                  }
                  className="rounded-full border border-slate-700 px-2 py-1 text-slate-200"
                >
                  {item.pinned ? "Unpin" : "Pin"}
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

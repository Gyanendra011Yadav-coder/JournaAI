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
    authorRaw?: string | null;
    authorTaskStatus?: string | null;
    journalistName?: string | null;
    journalistId?: number | null;
  };
}

export default function SavedPage() {
  const [items, setItems] = useState<SavedArticle[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [updatingId, setUpdatingId] = useState<number | null>(null);
  const [showPinnedOnly, setShowPinnedOnly] = useState(false);

  const renderAuthor = (article: SavedArticle["article"]) => {
    if (article.journalistName) {
      return article.journalistName;
    }
    if (article.authorRaw) {
      return article.authorRaw;
    }
    if (article.authorTaskStatus === "PENDING" || article.authorTaskStatus === "RUNNING") {
      return "Extracting...";
    }
    if (article.authorTaskStatus === "FAILED") {
      return "Failed";
    }
    if (article.authorTaskStatus === "NEEDS_REVIEW") {
      return "Needs review";
    }
    return "Unknown author";
  };

  useEffect(() => {
    apiFetch<SavedArticle[]>("/api/saved-articles")
      .then((data) => {
        const sorted = [...data].sort((a, b) => Number(b.pinned) - Number(a.pinned));
        setItems(sorted);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load saved articles."));
  }, []);

  const visibleItems = showPinnedOnly ? items.filter((item) => item.pinned) : items;

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Saved Articles</p>
        <h1 className="text-3xl font-semibold">Pinned coverage</h1>
        <p className="text-slate-600">Review pinned highlights and saved clips.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="flex flex-wrap gap-2">
        {[
          { key: "all", label: "All saved" },
          { key: "pinned", label: "Pinned only" },
        ].map((option) => (
          <button
            key={option.key}
            onClick={() => setShowPinnedOnly(option.key === "pinned")}
            className={`rounded-full border px-3 py-1 text-xs transition ${
              (option.key === "pinned") === showPinnedOnly
                ? "border-cyan-300/70 bg-cyan-50 text-cyan-700"
                : "border-slate-200 text-slate-600 hover:border-cyan-200 hover:text-slate-900"
            }`}
          >
            {option.label}
          </button>
        ))}
      </div>
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-8">
        {visibleItems.length === 0 && <p className="text-sm text-slate-600">No saved articles yet.</p>}
        <div className="space-y-3">
          {visibleItems.map((item) => (
            <div
              key={item.articleId}
              className="flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-200/70 bg-white p-4"
            >
              <div>
                <Link href={`/articles/${item.articleId}`} className="text-slate-900 hover:text-cyan-700 hover:underline">
                  {item.article.title}
                </Link>
                <p className="text-xs text-slate-600">
                  {renderAuthor(item.article)} ·{" "}
                  {item.article.beatName ?? "Trending"} ·{" "}
                  {item.article.publishedAtUtc
                    ? new Date(item.article.publishedAtUtc).toLocaleString()
                    : "Unknown date"}
                </p>
              </div>
              <div className="flex items-center gap-2 text-xs">
                {item.pinned && (
                  <span className="rounded-full bg-emerald-100 px-2 py-1 text-emerald-700">Pinned</span>
                )}
                <button
                  onClick={async () => {
                    setUpdatingId(item.articleId);
                    setError(null);
                    try {
                      await apiFetch(`/api/articles/${item.articleId}/pin?pinned=${!item.pinned}`, { method: "POST" });
                      if (item.pinned && showPinnedOnly) {
                        setItems((prev) => prev.filter((entry) => entry.articleId !== item.articleId));
                      } else {
                        setItems((prev) => {
                          const updated = prev.map((entry) =>
                            entry.articleId === item.articleId ? { ...entry, pinned: !item.pinned } : entry
                          );
                          return [...updated].sort((a, b) => Number(b.pinned) - Number(a.pinned));
                        });
                      }
                    } catch (err) {
                      setError(err instanceof Error ? err.message : "Unable to update pin.");
                    } finally {
                      setUpdatingId(null);
                    }
                  }}
                  disabled={updatingId === item.articleId}
                  className="rounded-full border border-slate-200 px-2 py-1 text-slate-700 hover:border-cyan-300 hover:text-slate-900 disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {updatingId === item.articleId && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-slate-400 border-t-transparent" />
                    )}
                    {item.pinned ? "Unpin" : "Pin"}
                  </span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </div>
    </div>
  );
}

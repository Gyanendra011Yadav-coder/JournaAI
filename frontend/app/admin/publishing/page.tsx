"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";

interface Article {
  id: number;
  title: string;
  beatName: string;
  sourceName: string | null;
  publishedAtUtc: string | null;
  status: string;
}

export default function AdminPublishingPage() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [view, setView] = useState<"INGESTED" | "PUBLISHED">("INGESTED");

  const load = async () => {
    const response = await apiFetch<{ items: Article[] }>(`/api/articles?status=${view}&page=0&size=50`);
    setArticles(response.items);
  };

  useEffect(() => {
    load();
  }, [view]);

  const handlePublishToggle = async (article: Article) => {
    const endpoint = article.status === "PUBLISHED" ? "unpublish" : "publish";
    await apiFetch(`/api/admin/articles/${article.id}/${endpoint}`, { method: "POST" });
    await load();
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">Publishing</h1>
        <p className="text-slate-400">Review ingested articles and control what is published.</p>
      </header>

      <div className="flex gap-3">
        {(["INGESTED", "PUBLISHED"] as const).map((item) => (
          <button
            key={item}
            onClick={() => setView(item)}
            className={`rounded-full px-4 py-2 text-sm border ${
              view === item ? "bg-cyan-500 text-slate-900 border-cyan-400" : "border-slate-700 text-slate-200"
            }`}
          >
            {item}
          </button>
        ))}
      </div>

      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <table className="w-full text-sm">
          <thead className="text-slate-400">
            <tr>
              <th className="text-left pb-3">Headline</th>
              <th className="text-left pb-3">Beat</th>
              <th className="text-left pb-3">Source</th>
              <th className="text-left pb-3">Published</th>
              <th className="text-left pb-3">Action</th>
            </tr>
          </thead>
          <tbody className="divide-y divide-slate-800">
            {articles.map((article) => (
              <tr key={article.id}>
                <td className="py-3">{article.title}</td>
                <td className="py-3">{article.beatName}</td>
                <td className="py-3">{article.sourceName ?? "Unknown"}</td>
                <td className="py-3">
                  {article.publishedAtUtc ? new Date(article.publishedAtUtc).toLocaleDateString() : "—"}
                </td>
                <td className="py-3">
                  <button
                    onClick={() => handlePublishToggle(article)}
                    className={`rounded-lg px-3 py-1 text-xs ${
                      article.status === "PUBLISHED"
                        ? "border border-amber-400/60 text-amber-200"
                        : "bg-emerald-500 text-slate-900"
                    }`}
                  >
                    {article.status === "PUBLISHED" ? "Unpublish" : "Publish"}
                  </button>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </div>
  );
}

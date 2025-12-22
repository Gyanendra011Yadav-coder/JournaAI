"use client";

import { useEffect, useState } from "react";
import { useParams } from "next/navigation";
import { apiFetch } from "../../../lib/api";
import { JournalistList } from "../../../components/JournalistList";

interface Article {
  id: number;
  headline: string;
  source: string;
  author: string;
  publishedAt: string;
  summary: string;
  url: string;
  canonicalUrl: string;
  beats: string[];
  provider: string;
  saved?: boolean;
}

interface Journalist {
  id: number;
  name: string;
  outlet: string;
  location: string;
  email: string;
  beats: string[];
}

export default function ArticleDetailPage() {
  const params = useParams();
  const [article, setArticle] = useState<Article | null>(null);
  const [journalists, setJournalists] = useState<Journalist[]>([]);
  const [saved, setSaved] = useState(false);
  const [filters, setFilters] = useState({ outlet: "", location: "" });

  useEffect(() => {
    if (!params.id) return;
    apiFetch<Article>(`/api/articles/${params.id}`).then((data) => {
      setArticle(data);
      setSaved(data.saved ?? false);
    });
  }, [params.id]);

  const handleFindJournalists = async () => {
    if (!article) return;
    const params = new URLSearchParams();
    if (article.beats?.[0]) {
      params.set("beat", article.beats[0]);
    }
    if (filters.outlet) {
      params.set("outlet", filters.outlet);
    }
    if (filters.location) {
      params.set("location", filters.location);
    }
    const result = await apiFetch<Journalist[]>(`/api/journalists/search?${params.toString()}`);
    setJournalists(result);
  };

  if (!article) {
    return <div>Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">{article.headline}</h1>
        <p className="text-slate-400">
          {article.source} · {article.author} · {new Date(article.publishedAt).toLocaleString()}
        </p>
        <p className="text-xs text-cyan-300">Provider: {article.provider}</p>
      </header>
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
        <div>
          <p className="text-sm text-slate-400">Summary</p>
          <p className="text-slate-200 mt-2">{article.summary}</p>
        </div>
        <div className="flex flex-wrap gap-2">
          {article.beats?.map((beat) => (
            <span key={beat} className="text-xs bg-slate-800 px-2 py-1 rounded-full">
              {beat}
            </span>
          ))}
        </div>
        <button
          onClick={async () => {
            await apiFetch<Article>(`/api/articles/${article.id}/save?saved=${!saved}`, { method: "POST" });
            setSaved(!saved);
          }}
          className="px-3 py-2 bg-slate-800 rounded-lg text-sm"
        >
          {saved ? "Remove from Saved" : "Save to Tracked"}
        </button>
        <a href={article.canonicalUrl ?? article.url} className="text-cyan-300 hover:underline" target="_blank">
          View original article
        </a>
      </div>
      <section className="space-y-4">
        <div className="flex flex-wrap items-center justify-between gap-3">
          <h2 className="text-lg font-semibold">Find journalists</h2>
          <button
            onClick={handleFindJournalists}
            className="px-3 py-2 bg-emerald-500 text-slate-900 rounded-lg"
          >
            Search
          </button>
        </div>
        <div className="grid gap-3 md:grid-cols-2">
          <input
            placeholder="Filter by outlet"
            value={filters.outlet}
            onChange={(event) => setFilters((prev) => ({ ...prev, outlet: event.target.value }))}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          />
          <input
            placeholder="Filter by location"
            value={filters.location}
            onChange={(event) => setFilters((prev) => ({ ...prev, location: event.target.value }))}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          />
        </div>
        <JournalistList journalists={journalists} articleId={article.id} />
      </section>
    </div>
  );
}

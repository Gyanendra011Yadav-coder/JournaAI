"use client";

import { useState } from "react";
import { BeatSelector } from "../../components/BeatSelector";
import { TimeframePicker } from "../../components/TimeframePicker";
import { ArticlesTable } from "../../components/ArticlesTable";
import { apiFetch } from "../../lib/api";

interface Article {
  id: number;
  headline: string;
  source: string;
  author: string;
  publishedAt: string;
  summary: string;
}

export default function SearchPage() {
  const [beat, setBeat] = useState("Taxation");
  const [timeframe, setTimeframe] = useState("24h");
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(false);

  const handleSearch = async () => {
    setLoading(true);
    try {
      const result = await apiFetch<Article[]>("/api/articles/search", {
        method: "POST",
        body: JSON.stringify({ beat, timeframe, page: 1 }),
      });
      setArticles(result);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Discovery</p>
        <h1 className="text-2xl font-semibold">News Search</h1>
        <p className="text-slate-400">Track news by beat and timeframe.</p>
      </header>
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4 shadow-[0_0_0_1px_rgba(59,130,246,0.1)]">
        <div>
          <p className="text-sm text-slate-400 mb-2">Select beat</p>
          <BeatSelector value={beat} onChange={setBeat} />
        </div>
        <div>
          <p className="text-sm text-slate-400 mb-2">Timeframe</p>
          <TimeframePicker value={timeframe} onChange={setTimeframe} />
        </div>
        <button
          onClick={handleSearch}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 text-slate-900 font-semibold shadow-lg shadow-cyan-500/20"
        >
          {loading ? "Searching..." : "Search"}
        </button>
      </div>
      <ArticlesTable articles={articles} />
    </div>
  );
}

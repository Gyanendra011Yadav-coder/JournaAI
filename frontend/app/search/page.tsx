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
      <header>
        <h1 className="text-2xl font-semibold">News Search</h1>
        <p className="text-slate-400">Track news by beat and timeframe.</p>
      </header>
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
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
          className="px-4 py-2 bg-cyan-500 text-slate-900 rounded-lg font-semibold"
        >
          {loading ? "Searching..." : "Search"}
        </button>
      </div>
      <ArticlesTable articles={articles} />
    </div>
  );
}

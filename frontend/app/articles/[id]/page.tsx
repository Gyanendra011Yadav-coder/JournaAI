"use client";

import { useEffect, useState } from "react";
import { useParams, useRouter } from "next/navigation";
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
  saved?: boolean;
}

interface Journalist {
  id: number;
  name: string;
  outlet: string;
  location: string;
  email: string;
}

export default function ArticleDetailPage() {
  const params = useParams();
  const router = useRouter();
  const [article, setArticle] = useState<Article | null>(null);
  const [journalists, setJournalists] = useState<Journalist[]>([]);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    if (!params.id) return;
    apiFetch<Article>(`/api/articles/${params.id}`).then((data) => {
      setArticle(data);
      setSaved(data.saved ?? false);
    });
  }, [params.id]);

  const handleFindJournalists = async () => {
    const result = await apiFetch<Journalist[]>(`/api/journalists/search?beat=Taxation`);
    setJournalists(result);
  };

  if (!article) {
    return <div>Loading...</div>;
  }

  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">{article.headline}</h1>
        <p className="text-slate-400">{article.source} · {article.author} · {new Date(article.publishedAt).toLocaleString()}</p>
      </header>
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
        <div>
          <p className="text-sm text-slate-400">Summary</p>
          <p className="text-slate-200 mt-2">{article.summary}</p>
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
        <a href={article.url} className="text-cyan-300 hover:underline" target="_blank">View original article</a>
      </div>
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Find journalists</h2>
          <button
            onClick={handleFindJournalists}
            className="px-3 py-2 bg-emerald-500 text-slate-900 rounded-lg"
          >
            Search database
          </button>
        </div>
        <JournalistList journalists={journalists} />
        {journalists.map((journalist) => (
          <button
            key={journalist.id}
            className="text-sm text-cyan-300"
            onClick={() => router.push(`/outreach/compose?articleId=${article.id}&journalistId=${journalist.id}`)}
          >
            Compose outreach to {journalist.name}
          </button>
        ))}
      </section>
    </div>
  );
}

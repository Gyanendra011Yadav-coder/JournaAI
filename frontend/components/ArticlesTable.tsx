"use client";

import Link from "next/link";

interface Article {
  id: number;
  headline: string;
  source: string;
  author: string;
  publishedAt: string;
  summary: string;
}

interface ArticlesTableProps {
  articles: Article[];
}

export function ArticlesTable({ articles }: ArticlesTableProps) {
  return (
    <div className="border border-slate-800 rounded-xl overflow-hidden">
      <table className="w-full text-sm">
        <thead className="bg-slate-900 text-slate-300">
          <tr>
            <th className="text-left p-3">Headline</th>
            <th className="text-left p-3">Source</th>
            <th className="text-left p-3">Author</th>
            <th className="text-left p-3">Date</th>
            <th className="text-left p-3">Summary</th>
          </tr>
        </thead>
        <tbody className="divide-y divide-slate-800">
          {articles.map((article) => (
            <tr key={article.id} className="hover:bg-slate-900/50">
              <td className="p-3">
                <Link href={`/articles/${article.id}`} className="text-cyan-300 hover:underline">
                  {article.headline}
                </Link>
              </td>
              <td className="p-3">{article.source}</td>
              <td className="p-3">{article.author}</td>
              <td className="p-3">{new Date(article.publishedAt).toLocaleDateString()}</td>
              <td className="p-3 text-slate-400">{article.summary}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface Article {
  id: number;
  title: string;
  beatName: string;
  sourceName: string | null;
  publishedAtUtc: string | null;
  status: string;
}

interface Beat {
  id: number;
  name: string;
}

export default function AdminPublishingPage() {
  const [articles, setArticles] = useState<Article[]>([]);
  const [view, setView] = useState<"INGESTED" | "PUBLISHED">("INGESTED");
  const [beats, setBeats] = useState<Beat[]>([]);
  const [manualBeatId, setManualBeatId] = useState<number | null>(null);
  const [manualTitle, setManualTitle] = useState("");
  const [manualUrl, setManualUrl] = useState("");
  const [manualSource, setManualSource] = useState("");
  const [manualAuthor, setManualAuthor] = useState("");
  const [manualSummary, setManualSummary] = useState("");
  const [manualPublishedAt, setManualPublishedAt] = useState("");
  const [manualStatus, setManualStatus] = useState<string | null>(null);
  const [manualError, setManualError] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [manualSubmitting, setManualSubmitting] = useState(false);
  const [publishingId, setPublishingId] = useState<number | null>(null);

  const load = async () => {
    try {
      const response = await apiFetch<{ items: Article[] }>(`/api/articles?status=${view}&page=0&size=50`);
      setArticles(response.items);
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load articles.");
    }
  };

  useEffect(() => {
    load();
  }, [view]);

  useEffect(() => {
    apiFetch<Beat[]>("/api/beats")
      .then((data) => {
        setBeats(data);
        if (data.length > 0) {
          setManualBeatId(data[0].id);
        }
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load beats."));
  }, []);

  const handlePublishToggle = async (article: Article) => {
    const endpoint = article.status === "PUBLISHED" ? "unpublish" : "publish";
    setPublishingId(article.id);
    try {
      await apiFetch(`/api/admin/articles/${article.id}/${endpoint}`, { method: "POST" });
      await load();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to update article.");
    } finally {
      setPublishingId(null);
    }
  };

  const handleManualSubmit = async () => {
    if (!manualBeatId) {
      setManualError("Select a beat before adding an article.");
      return;
    }
    if (!manualTitle.trim() || !manualUrl.trim()) {
      setManualError("Headline and URL are required.");
      return;
    }
    setManualError(null);
    setManualSubmitting(true);
    try {
      await apiFetch("/api/admin/articles/manual", {
        method: "POST",
        body: JSON.stringify({
          beatId: manualBeatId,
          title: manualTitle,
          url: manualUrl,
          sourceName: manualSource || null,
          author: manualAuthor || null,
          summary: manualSummary || null,
          publishedAtUtc: manualPublishedAt ? new Date(manualPublishedAt).toISOString() : null,
        }),
      });
      setManualStatus("Manual article added.");
      setManualTitle("");
      setManualUrl("");
      setManualSource("");
      setManualAuthor("");
      setManualSummary("");
      setManualPublishedAt("");
      await load();
    } catch (err) {
      setManualError(err instanceof Error ? err.message : "Unable to add article.");
    } finally {
      setManualSubmitting(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">Publishing</h1>
        <p className="text-slate-400">Review ingested articles and control what is published.</p>
      </header>
      <ErrorBanner message={error} />

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Manual feed</h2>
          <span className="text-xs uppercase tracking-[0.2em] text-slate-400">Admin only</span>
        </div>
        <ErrorBanner message={manualError} />
        {manualStatus && (
          <div className="rounded-2xl border border-emerald-500/40 bg-emerald-500/10 p-4 text-sm text-emerald-100">
            {manualStatus}
          </div>
        )}
        <div className="grid gap-4 md:grid-cols-2">
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Beat</label>
            <select
              value={manualBeatId ?? ""}
              onChange={(event) => setManualBeatId(Number(event.target.value))}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            >
              {beats.map((beat) => (
                <option key={beat.id} value={beat.id}>
                  {beat.name}
                </option>
              ))}
            </select>
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Headline</label>
            <input
              value={manualTitle}
              onChange={(event) => setManualTitle(event.target.value)}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">URL</label>
            <input
              value={manualUrl}
              onChange={(event) => setManualUrl(event.target.value)}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Source</label>
            <input
              value={manualSource}
              onChange={(event) => setManualSource(event.target.value)}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Author</label>
            <input
              value={manualAuthor}
              onChange={(event) => setManualAuthor(event.target.value)}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
          </div>
          <div>
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Published at</label>
            <input
              type="datetime-local"
              value={manualPublishedAt}
              onChange={(event) => setManualPublishedAt(event.target.value)}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 text-slate-200"
              style={{ colorScheme: "dark" }}
            />
          </div>
          <div className="md:col-span-2">
            <label className="text-xs uppercase tracking-[0.2em] text-slate-400">Summary</label>
            <textarea
              value={manualSummary}
              onChange={(event) => setManualSummary(event.target.value)}
              className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              rows={3}
            />
          </div>
        </div>
        <button
          onClick={handleManualSubmit}
          disabled={manualSubmitting}
          className="rounded-xl bg-cyan-500 text-slate-900 px-4 py-2 font-semibold disabled:opacity-60"
        >
          <span className="inline-flex items-center gap-2">
            {manualSubmitting && (
              <span className="h-3 w-3 animate-spin rounded-full border border-slate-900 border-t-transparent" />
            )}
            Add manual article
          </span>
        </button>
      </section>

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
                    disabled={publishingId === article.id}
                    className={`rounded-lg px-3 py-1 text-xs disabled:opacity-60 ${
                      article.status === "PUBLISHED"
                        ? "border border-amber-400/60 text-amber-200"
                        : "bg-emerald-500 text-slate-900"
                    }`}
                  >
                    <span className="inline-flex items-center gap-2">
                      {publishingId === article.id && (
                        <span className="h-3 w-3 animate-spin rounded-full border border-slate-200 border-t-transparent" />
                      )}
                      {article.status === "PUBLISHED" ? "Unpublish" : "Publish"}
                    </span>
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

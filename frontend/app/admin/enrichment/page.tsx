"use client";

import { useEffect, useMemo, useState } from "react";
import Link from "next/link";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface EnrichmentTask {
  id: number;
  taskType: string;
  articleId?: number;
  articleTitle?: string;
  articleUrl?: string;
  journalistId?: number;
  journalistName?: string;
  status: string;
  priority: number;
  attempts: number;
  nextRunAt?: string;
  notes?: string;
}

export default function AdminEnrichmentPage() {
  const [tasks, setTasks] = useState<EnrichmentTask[]>([]);
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [error, setError] = useState<string | null>(null);
  const [running, setRunning] = useState(false);
  const [runningTaskId, setRunningTaskId] = useState<number | null>(null);

  const loadTasks = () => {
    const query = statusFilter ? `?status=${statusFilter}` : "";
    apiFetch<EnrichmentTask[]>(`/api/admin/enrichment/tasks${query}`)
      .then((data) => {
        setTasks(data);
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load enrichment tasks."));
  };

  useEffect(() => {
    loadTasks();
  }, [statusFilter]);

  const handleRunQueue = async () => {
    setRunning(true);
    try {
      await apiFetch("/api/admin/enrichment/run", { method: "POST" });
      loadTasks();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to run enrichment tasks.");
    } finally {
      setRunning(false);
    }
  };

  const handleRunTask = async (task: EnrichmentTask) => {
    setRunningTaskId(task.id);
    try {
      if (task.articleId) {
        await apiFetch(`/api/admin/enrichment/run?articleId=${task.articleId}`, { method: "POST" });
      } else if (task.journalistId) {
        await apiFetch(`/api/admin/enrichment/run?journalistId=${task.journalistId}`, { method: "POST" });
      } else {
        await apiFetch("/api/admin/enrichment/run", { method: "POST" });
      }
      loadTasks();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to run enrichment task.");
    } finally {
      setRunningTaskId(null);
    }
  };

  const actionHint = useMemo(
    () => (task: EnrichmentTask) => {
      if (task.status === "NEEDS_REVIEW") {
        return "Review the article author match and confirm the journalist.";
      }
      if (task.status === "FAILED") {
        return "Check the task notes and retry enrichment.";
      }
      if (task.status === "RUNNING") {
        return "Enrichment is running.";
      }
      if (task.status === "DONE") {
        return "Enrichment completed.";
      }
      if (task.status === "SKIPPED") {
        return "Task skipped; no action required.";
      }
      return "Queued for processing.";
    },
    []
  );

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Admin</p>
        <h1 className="text-3xl font-semibold">Article Enrichment</h1>
        <p className="text-slate-600">Track author extraction, journalist resolution, and profile enrichment.</p>
      </header>
      <div className="rounded-3xl border border-slate-200/70 bg-slate-50 p-6 text-sm text-slate-700 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.12)]">
        <p className="font-semibold text-slate-800">Tip: status guide</p>
        <div className="mt-3 grid gap-2 md:grid-cols-2">
          <div className="flex items-start gap-2">
            <span className="mt-1 h-2 w-2 rounded-full bg-amber-400" />
            <span><strong>PENDING</strong> queues work for the next run.</span>
          </div>
          <div className="flex items-start gap-2">
            <span className="mt-1 h-2 w-2 rounded-full bg-slate-400" />
            <span><strong>RUNNING</strong> means the job is processing.</span>
          </div>
          <div className="flex items-start gap-2">
            <span className="mt-1 h-2 w-2 rounded-full bg-indigo-400" />
            <span><strong>NEEDS_REVIEW</strong> expects an admin review on the linked article/journalist.</span>
          </div>
          <div className="flex items-start gap-2">
            <span className="mt-1 h-2 w-2 rounded-full bg-emerald-500" />
            <span><strong>DONE</strong> is complete; no action needed.</span>
          </div>
          <div className="flex items-start gap-2">
            <span className="mt-1 h-2 w-2 rounded-full bg-rose-500" />
            <span><strong>FAILED</strong> needs a retry after reviewing notes.</span>
          </div>
          <div className="flex items-start gap-2">
            <span className="mt-1 h-2 w-2 rounded-full bg-slate-300" />
            <span><strong>SKIPPED</strong> is safe to ignore.</span>
          </div>
        </div>
      </div>
      <ErrorBanner message={error} />
      <div className="flex flex-wrap items-center gap-3">
        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          className="rounded-xl border border-slate-200 bg-white/80 px-3 py-2 text-sm text-slate-700"
        >
          {["PENDING", "RUNNING", "NEEDS_REVIEW", "DONE", "FAILED", "SKIPPED"].map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
        <button
          onClick={handleRunQueue}
          disabled={running}
          className="rounded-xl border border-emerald-300/70 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-60"
        >
          {running ? "Running..." : "Run enrichment"}
        </button>
      </div>
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
        {tasks.length === 0 ? (
          <p className="text-sm text-slate-600">No tasks for this filter.</p>
        ) : (
          <div className="space-y-4">
            {tasks.map((task) => (
              <div key={task.id} className="rounded-2xl border border-slate-200/70 bg-white p-5">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <p className="text-sm font-semibold text-slate-900">{task.taskType}</p>
                    <div className="text-xs text-slate-500 space-y-1">
                      {task.articleId && (
                        <p>
                          Article{" "}
                          <Link href={`/articles/${task.articleId}`} className="text-cyan-700 hover:underline">
                            {task.articleTitle ?? `#${task.articleId}`}
                          </Link>
                          {task.articleUrl && (
                            <a
                              href={task.articleUrl}
                              target="_blank"
                              rel="noreferrer"
                              className="ml-2 text-slate-500 hover:text-cyan-700"
                            >
                              Source
                            </a>
                          )}
                        </p>
                      )}
                      {task.journalistId && (
                        <p>
                          Journalist{" "}
                          <Link href={`/journalists/${task.journalistId}`} className="text-cyan-700 hover:underline">
                            {task.journalistName ?? `#${task.journalistId}`}
                          </Link>
                        </p>
                      )}
                    </div>
                  </div>
                  <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
                    {task.status}
                  </span>
                </div>
                <div className="mt-3 flex flex-wrap items-center gap-3 text-xs text-slate-600">
                  <span>Attempts: {task.attempts}</span>
                  {task.nextRunAt && <span>Next run: {new Date(task.nextRunAt).toLocaleString()}</span>}
                </div>
                <p className="mt-3 text-sm text-slate-600">{actionHint(task)}</p>
                {task.notes && <p className="mt-2 text-xs text-slate-500">Notes: {task.notes}</p>}
                <div className="mt-4 flex flex-wrap items-center gap-2">
                  {(task.articleId || task.journalistId) && (
                    <button
                      onClick={() => handleRunTask(task)}
                      disabled={runningTaskId === task.id}
                      className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-700 hover:border-cyan-300 hover:text-slate-900 disabled:opacity-60"
                    >
                      {runningTaskId === task.id ? "Running..." : "Run now"}
                    </button>
                  )}
                  {task.articleId && (
                    <Link
                      href={`/articles/${task.articleId}`}
                      className="rounded-xl border border-cyan-300/70 bg-cyan-50 px-3 py-1.5 text-xs text-cyan-700 hover:bg-cyan-100"
                    >
                      Review article
                    </Link>
                  )}
                  {task.journalistId && (
                    <Link
                      href={`/journalists/${task.journalistId}`}
                      className="rounded-xl border border-emerald-300/70 bg-emerald-50 px-3 py-1.5 text-xs text-emerald-700 hover:bg-emerald-100"
                    >
                      Review journalist
                    </Link>
                  )}
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

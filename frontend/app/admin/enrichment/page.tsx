"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface EnrichmentTask {
  id: number;
  taskType: string;
  articleId?: number;
  journalistId?: number;
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

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">Enrichment Queue</h1>
        <p className="text-slate-400">Track author extraction, journalist resolution, and profile enrichment.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="flex flex-wrap items-center gap-3">
        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          className="rounded-xl border border-slate-700/80 bg-slate-900/60 px-3 py-2 text-sm"
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
          className="rounded-xl border border-emerald-500/60 bg-emerald-500/10 px-4 py-2 text-sm font-semibold text-emerald-100 hover:bg-emerald-500/20 disabled:opacity-60"
        >
          {running ? "Running..." : "Run enrichment"}
        </button>
      </div>
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        {tasks.length === 0 ? (
          <p className="text-sm text-slate-400">No tasks for this filter.</p>
        ) : (
          <div className="space-y-4">
            {tasks.map((task) => (
              <div key={task.id} className="rounded-xl border border-slate-800/80 bg-slate-950/60 p-4">
                <div className="flex flex-wrap items-center justify-between gap-2">
                  <div>
                    <p className="text-sm font-semibold text-slate-100">{task.taskType}</p>
                    <p className="text-xs text-slate-400">
                      Article {task.articleId ?? "-"} · Journalist {task.journalistId ?? "-"}
                    </p>
                  </div>
                  <span className="rounded-full bg-slate-800/80 px-3 py-1 text-xs text-slate-200">
                    {task.status}
                  </span>
                </div>
                {task.notes && <p className="mt-2 text-xs text-slate-400">{task.notes}</p>}
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
}

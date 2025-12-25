"use client";

import { useState } from "react";
import { apiFetch } from "../../../../lib/api";
import { ErrorBanner } from "../../../../components/ErrorBanner";

interface ImportRow {
  id: number;
  rowNumber: number;
  status: string;
  message?: string;
  journalistId?: number;
}

interface ImportJobResponse {
  id: number;
  status: string;
  dryRun: boolean;
  createdAt?: string;
  sourceName?: string;
  summaryJsonb?: string;
  rows: ImportRow[];
}

export default function AdminJournalistImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const [dryRun, setDryRun] = useState(true);
  const [job, setJob] = useState<ImportJobResponse | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);

  const handleSubmit = async () => {
    if (!file) return;
    setLoading(true);
    setError(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const response = await apiFetch<ImportJobResponse>(`/api/admin/journalists/import-csv?dryRun=${dryRun}`, {
        method: "POST",
        body: form,
      });
      setJob(response);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to import CSV.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">CSV Import</h1>
        <p className="text-slate-400">Upload verified journalist details and review changes before applying.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
        <input
          type="file"
          accept=".csv"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          className="block w-full text-sm text-slate-300"
        />
        <label className="flex items-center gap-2 text-sm text-slate-300">
          <input type="checkbox" checked={dryRun} onChange={(event) => setDryRun(event.target.checked)} />
          Dry run (validation only)
        </label>
        <button
          onClick={handleSubmit}
          disabled={!file || loading}
          className="rounded-xl border border-emerald-500/60 bg-emerald-500/10 px-4 py-2 text-sm font-semibold text-emerald-100 hover:bg-emerald-500/20 disabled:opacity-60"
        >
          {loading ? "Uploading..." : "Upload CSV"}
        </button>
      </div>
      {job && (
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-slate-100">Job #{job.id}</p>
              <p className="text-xs text-slate-400">{job.sourceName ?? "CSV"} · {job.status}</p>
            </div>
            <span className="rounded-full bg-slate-800/80 px-3 py-1 text-xs text-slate-200">
              {dryRun ? "Dry run" : "Applied"}
            </span>
          </div>
          <div className="space-y-2">
            {job.rows.map((row) => (
              <div key={row.id} className="rounded-xl border border-slate-800/80 bg-slate-950/60 p-3 text-xs text-slate-300">
                Row {row.rowNumber}: {row.status} {row.message ? `- ${row.message}` : ""}
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
}

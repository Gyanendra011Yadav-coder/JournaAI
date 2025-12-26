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
  payloadJsonb?: string;
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
  const [applying, setApplying] = useState(false);

  const sampleCsv = `full_name,publication_name,publication_domain,email,designation,beats,country,city,linkedin,phone,source_type
Jane Doe,TechWire,techwire.com,jane@techwire.com,Senior Reporter,Technology/AI;Startups/VC,US,San Francisco,https://linkedin.com/in/janedoe,+1-415-555-0123,manual
Arun Kapoor,FinNews,finnews.in,arun@finnews.in,Editor,Finance/BFSI,IN,Mumbai,,,+`;

  const parsePayload = (payload?: string) => {
    if (!payload) return null;
    try {
      return JSON.parse(payload) as Record<string, string>;
    } catch {
      return null;
    }
  };

  const previewRows = job?.rows.map((row) => ({
    ...row,
    payload: parsePayload(row.payloadJsonb),
  })) ?? [];

  const previewColumns = Array.from(
    new Set(
      previewRows
        .flatMap((row) => (row.payload ? Object.keys(row.payload) : []))
        .filter((key) => key && key.length > 0)
    )
  );

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

  const handleApply = async () => {
    if (!file) return;
    setApplying(true);
    setError(null);
    try {
      const form = new FormData();
      form.append("file", file);
      const response = await apiFetch<ImportJobResponse>("/api/admin/journalists/import-csv?dryRun=false", {
        method: "POST",
        body: form,
      });
      setJob(response);
      setDryRun(false);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to apply CSV import.");
    } finally {
      setApplying(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Admin</p>
        <h1 className="text-3xl font-semibold">CSV Import</h1>
        <p className="text-slate-600">Upload verified journalist details and review changes before applying.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 space-y-4 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
        <div className="rounded-2xl border border-slate-200/70 bg-slate-50 p-4 text-sm text-slate-700 space-y-2">
          <p className="font-semibold">Expected columns</p>
          <p>Required: <span className="font-medium">full_name</span></p>
          <p>
            Optional: email, publication_name, publication_domain, designation, beats (semicolon-separated), country,
            city, linkedin, phone, source_type.
          </p>
          <a
            href={`data:text/csv;charset=utf-8,${encodeURIComponent(sampleCsv)}`}
            download="journalist_import_sample.csv"
            className="inline-flex items-center rounded-lg border border-cyan-300/70 bg-cyan-50 px-3 py-1 text-xs font-semibold text-cyan-700 hover:bg-cyan-100"
          >
            Download sample CSV
          </a>
        </div>
        <input
          type="file"
          accept=".csv"
          onChange={(event) => setFile(event.target.files?.[0] ?? null)}
          className="block w-full text-sm text-slate-600"
        />
        <label className="flex items-center gap-2 text-sm text-slate-600">
          <input type="checkbox" checked={dryRun} onChange={(event) => setDryRun(event.target.checked)} />
          Dry run (validation only)
        </label>
        <button
          onClick={handleSubmit}
          disabled={!file || loading}
          className="rounded-xl border border-emerald-300/70 bg-emerald-50 px-4 py-2 text-sm font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-60"
        >
          {loading ? "Uploading..." : "Upload CSV"}
        </button>
        {job?.dryRun && (
          <button
            onClick={handleApply}
            disabled={!file || applying}
            className="rounded-xl border border-cyan-300/70 bg-cyan-50 px-4 py-2 text-sm font-semibold text-cyan-700 hover:bg-cyan-100 disabled:opacity-60"
          >
            {applying ? "Applying..." : "Apply import"}
          </button>
        )}
      </div>
      {job && (
        <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 space-y-4 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="text-sm font-semibold text-slate-900">Job #{job.id}</p>
              <p className="text-xs text-slate-600">{job.sourceName ?? "CSV"} · {job.status}</p>
            </div>
            <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
              {dryRun ? "Dry run" : "Applied"}
            </span>
          </div>
          {previewColumns.length > 0 && (
            <div className="overflow-x-auto rounded-2xl border border-slate-200/70">
              <table className="w-full text-xs">
                <thead className="bg-slate-50 text-slate-600">
                  <tr>
                    <th className="p-2 text-left">Row</th>
                    <th className="p-2 text-left">Status</th>
                    {previewColumns.map((column) => (
                      <th key={column} className="p-2 text-left">
                        {column}
                      </th>
                    ))}
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-200">
                  {previewRows.slice(0, 10).map((row) => (
                    <tr key={row.id}>
                      <td className="p-2 text-slate-700">{row.rowNumber}</td>
                      <td className="p-2 text-slate-600">
                        {row.status} {row.message ? `- ${row.message}` : ""}
                      </td>
                      {previewColumns.map((column) => (
                        <td key={`${row.id}-${column}`} className="p-2 text-slate-600">
                          {row.payload?.[column] ?? "—"}
                        </td>
                      ))}
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
          {previewColumns.length === 0 && (
            <div className="space-y-2">
              {job.rows.map((row) => (
                <div key={row.id} className="rounded-xl border border-slate-200/70 bg-white p-3 text-xs text-slate-600">
                  Row {row.rowNumber}: {row.status} {row.message ? `- ${row.message}` : ""}
                </div>
              ))}
            </div>
          )}
        </div>
      )}
    </div>
  );
}

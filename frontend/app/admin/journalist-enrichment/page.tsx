"use client";

import { useEffect, useState } from "react";
import Link from "next/link";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface EnrichmentReview {
  id: number;
  journalistId: number | null;
  journalistName?: string | null;
  status: string;
  currentJsonb?: string | null;
  proposedJsonb?: string | null;
  diffJsonb?: string | null;
  createdAt?: string | null;
}

type DiffMap = Record<string, { current?: string; proposed?: string }>;

const parseDiff = (json?: string | null): DiffMap => {
  if (!json) {
    return {};
  }
  try {
    return JSON.parse(json) as DiffMap;
  } catch {
    return {};
  }
};

export default function JournalistEnrichmentPage() {
  const [reviews, setReviews] = useState<EnrichmentReview[]>([]);
  const [statusFilter, setStatusFilter] = useState("PENDING");
  const [error, setError] = useState<string | null>(null);
  const [processingId, setProcessingId] = useState<number | null>(null);

  const loadReviews = () => {
    apiFetch<EnrichmentReview[]>(`/api/admin/journalist-enrichment?status=${statusFilter}`)
      .then((data) => {
        setReviews(data);
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load journalist enrichment reviews."));
  };

  useEffect(() => {
    loadReviews();
  }, [statusFilter]);

  const handleAction = async (id: number, action: "apply" | "dismiss") => {
    setProcessingId(id);
    try {
      await apiFetch(`/api/admin/journalist-enrichment/${id}/${action}`, { method: "POST" });
      loadReviews();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to update review.");
    } finally {
      setProcessingId(null);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-emerald-600">Admin</p>
        <h1 className="text-3xl font-semibold">Journalist Enrichment</h1>
        <p className="text-slate-600">Review LLM suggestions before updating journalist profiles.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="flex flex-wrap items-center gap-3">
        <select
          value={statusFilter}
          onChange={(event) => setStatusFilter(event.target.value)}
          className="rounded-xl border border-slate-200 bg-white/80 px-3 py-2 text-sm text-slate-700"
        >
          {["PENDING", "APPLIED", "DISMISSED"].map((status) => (
            <option key={status} value={status}>
              {status}
            </option>
          ))}
        </select>
      </div>
      <div className="rounded-3xl border border-slate-200/70 bg-white/90 p-6 shadow-[0_12px_40px_-32px_rgba(15,23,42,0.2)]">
        {reviews.length === 0 ? (
          <p className="text-sm text-slate-600">No reviews for this filter.</p>
        ) : (
          <div className="space-y-4">
            {reviews.map((review) => {
              const diff = parseDiff(review.diffJsonb);
              const diffEntries = Object.entries(diff);
              return (
                <div key={review.id} className="rounded-2xl border border-slate-200/70 bg-white p-5">
                  <div className="flex flex-wrap items-center justify-between gap-2">
                    <div>
                      <p className="text-sm font-semibold text-slate-900">
                        {review.journalistName ?? "Unknown journalist"}
                      </p>
                      {review.journalistId && (
                        <Link
                          href={`/journalists/${review.journalistId}`}
                          className="text-xs text-cyan-700 hover:underline"
                        >
                          Open profile
                        </Link>
                      )}
                    </div>
                    <span className="rounded-full bg-slate-100 px-3 py-1 text-xs text-slate-600">
                      {review.status}
                    </span>
                  </div>
                  <div className="mt-4 space-y-3">
                    {diffEntries.length === 0 ? (
                      <p className="text-xs text-slate-500">No differences captured.</p>
                    ) : (
                      diffEntries.map(([field, values]) => (
                        <div key={field} className="rounded-xl border border-slate-200/70 bg-slate-50 px-3 py-2">
                          <p className="text-xs uppercase tracking-[0.2em] text-slate-500">{field}</p>
                          <div className="mt-1 grid gap-2 text-xs text-slate-700 md:grid-cols-2">
                            <div>
                              <p className="text-[11px] text-slate-500">Saved</p>
                              <p>{values.current ?? "—"}</p>
                            </div>
                            <div>
                              <p className="text-[11px] text-emerald-600">New</p>
                              <p>{values.proposed ?? "—"}</p>
                            </div>
                          </div>
                        </div>
                      ))
                    )}
                  </div>
                  {review.status === "PENDING" && (
                    <div className="mt-4 flex flex-wrap gap-2">
                      <button
                        onClick={() => handleAction(review.id, "apply")}
                        disabled={processingId === review.id}
                        className="rounded-xl border border-emerald-300/70 bg-emerald-50 px-3 py-1.5 text-xs font-semibold text-emerald-700 hover:bg-emerald-100 disabled:opacity-60"
                      >
                        Apply changes
                      </button>
                      <button
                        onClick={() => handleAction(review.id, "dismiss")}
                        disabled={processingId === review.id}
                        className="rounded-xl border border-slate-200 px-3 py-1.5 text-xs text-slate-700 hover:border-rose-300 hover:text-rose-700 disabled:opacity-60"
                      >
                        Dismiss
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>
    </div>
  );
}

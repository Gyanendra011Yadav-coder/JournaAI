"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";
import { AuditTimeline } from "../../../components/AuditTimeline";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface AuditResponse {
  id: number;
  action: string;
  entityType: string;
  entityId?: string;
  createdAt?: string;
  actorEmail?: string;
}

export default function AdminAuditPage() {
  const [events, setEvents] = useState<AuditResponse[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<AuditResponse[]>("/api/admin/audit")
      .then((data) => {
        setEvents(data);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unable to load audit log.");
      });
  }, []);

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">Audit Log</h1>
        <p className="text-slate-400">Track searches, refreshes, publishes, and outreach actions.</p>
      </header>
      <ErrorBanner message={error} />
      {events.length === 0 && !error && (
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 text-sm text-slate-400">
          No audit events recorded yet.
        </div>
      )}
      {events.length > 0 && (
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
          <AuditTimeline
            events={events.map((event) => ({
              id: event.id,
              action: event.action,
              entity: event.entityId ? `${event.entityType}:${event.entityId}` : event.entityType,
              timestamp: event.createdAt ?? new Date().toISOString(),
            }))}
          />
        </div>
      )}
    </div>
  );
}

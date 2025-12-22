"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";
import { AuditTimeline } from "../../components/AuditTimeline";

interface AuditEvent {
  id: number;
  action: string;
  entity: string;
  timestamp: string;
}

export default function DashboardPage() {
  const [events, setEvents] = useState<AuditEvent[]>([]);

  useEffect(() => {
    apiFetch<AuditEvent[]>("/api/audit").then(setEvents).catch(() => setEvents([]));
  }, []);

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Dashboard</p>
            <h1 className="text-2xl font-semibold">Command your PR narrative</h1>
            <p className="text-slate-400">A clear snapshot of activity, outreach, and editorial momentum.</p>
          </div>
          <div className="rounded-xl border border-cyan-500/40 bg-cyan-500/10 px-4 py-2 text-sm text-cyan-100">
            Weekly pulse • {events.length} updates
          </div>
        </div>
      </header>
      <div className="grid gap-4 md:grid-cols-3">
        {[
          { label: "Searches", value: events.filter((e) => e.action === "SEARCH").length },
          { label: "Saved Articles", value: events.filter((e) => e.action === "SAVE").length },
          { label: "Emails Sent", value: events.filter((e) => e.action === "SEND").length },
        ].map((metric) => (
          <div
            key={metric.label}
            className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-5 shadow-[0_0_0_1px_rgba(59,130,246,0.1)]"
          >
            <p className="text-xs uppercase tracking-[0.2em] text-slate-400">{metric.label}</p>
            <p className="mt-3 text-3xl font-semibold">{metric.value}</p>
            <div className="mt-4 h-1 rounded-full bg-gradient-to-r from-cyan-500/40 via-indigo-500/40 to-transparent" />
          </div>
        ))}
      </div>
      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-semibold">Audit Trail</h2>
          <span className="text-xs uppercase tracking-[0.2em] text-slate-400">Latest activity</span>
        </div>
        <div className="mt-4">
          <AuditTimeline events={events} />
        </div>
      </section>
    </div>
  );
}

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
      <header>
        <h1 className="text-2xl font-semibold">Dashboard</h1>
        <p className="text-slate-400">Overview of recent activity and saved work.</p>
      </header>
      <div className="grid md:grid-cols-3 gap-4">
        {[
          { label: "Searches", value: events.filter((e) => e.action === "SEARCH").length },
          { label: "Saved Articles", value: events.filter((e) => e.action === "SAVE").length },
          { label: "Emails Sent", value: events.filter((e) => e.action === "SEND").length },
        ].map((metric) => (
          <div key={metric.label} className="p-4 border border-slate-800 rounded-xl">
            <p className="text-slate-400 text-sm">{metric.label}</p>
            <p className="text-2xl font-semibold mt-2">{metric.value}</p>
          </div>
        ))}
      </div>
      <section>
        <h2 className="text-lg font-semibold mb-3">Audit Trail</h2>
        <AuditTimeline events={events} />
      </section>
    </div>
  );
}

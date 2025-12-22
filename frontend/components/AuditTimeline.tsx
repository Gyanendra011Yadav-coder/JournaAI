interface AuditEvent {
  id: number;
  action: string;
  entity: string;
  timestamp: string;
}

interface AuditTimelineProps {
  events: AuditEvent[];
}

export function AuditTimeline({ events }: AuditTimelineProps) {
  return (
    <div className="space-y-3">
      {events.map((event) => (
        <div key={event.id} className="p-3 border border-slate-800 rounded-lg">
          <div className="flex items-center justify-between text-sm">
            <span className="font-semibold">{event.action}</span>
            <span className="text-slate-400">{new Date(event.timestamp).toLocaleString()}</span>
          </div>
          <div className="text-xs text-slate-500">{event.entity}</div>
        </div>
      ))}
    </div>
  );
}

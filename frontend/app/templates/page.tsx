"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";

interface Template {
  id: number;
  name: string;
  subject: string;
  body: string;
}

export default function TemplatesPage() {
  const [templates, setTemplates] = useState<Template[]>([]);

  useEffect(() => {
    apiFetch<Template[]>("/api/templates").then(setTemplates);
  }, []);

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Outreach Studio</p>
        <h1 className="text-2xl font-semibold">Outreach Templates</h1>
        <p className="text-slate-400">
          Seeded templates with variables like {"{{journalist_name}}, {{outlet}}, {{beat}}"}.
        </p>
      </header>
      <div className="space-y-3">
        {templates.map((template) => (
          <div key={template.id} className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4">
            <h3 className="font-semibold">{template.name}</h3>
            <p className="text-sm text-slate-400">{template.subject}</p>
            <p className="text-xs text-slate-500 mt-2 whitespace-pre-wrap">{template.body}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

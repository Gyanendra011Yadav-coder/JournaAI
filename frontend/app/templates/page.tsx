"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../lib/api";
import { ErrorBanner } from "../../components/ErrorBanner";

interface Template {
  id: number;
  name: string;
  subject: string;
  body: string;
}

export default function TemplatesPage() {
  const [templates, setTemplates] = useState<Template[]>([]);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Template[]>("/api/templates")
      .then((data) => {
        setTemplates(data);
        setError(null);
      })
      .catch((err) => {
        setError(err instanceof Error ? err.message : "Unable to load templates.");
      });
  }, []);

  return (
    <div className="space-y-6">
      <header className="rounded-3xl border border-slate-200/70 bg-white/90 p-8 shadow-[0_20px_60px_-45px_rgba(15,23,42,0.25)]">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-600">Outreach Studio</p>
        <h1 className="text-3xl font-semibold">Outreach Templates</h1>
        <p className="text-slate-600">
          Seeded templates with variables like {"{{journalist_name}}, {{outlet}}, {{beat}}"}.
        </p>
      </header>
      <ErrorBanner message={error} />
      <div className="space-y-3">
        {templates.map((template) => (
          <div key={template.id} className="rounded-2xl border border-slate-200/70 bg-white/80 p-4">
            <h3 className="font-semibold">{template.name}</h3>
            <p className="text-sm text-slate-600">{template.subject}</p>
            <p className="text-xs text-slate-500 mt-2 whitespace-pre-wrap">{template.body}</p>
          </div>
        ))}
      </div>
    </div>
  );
}

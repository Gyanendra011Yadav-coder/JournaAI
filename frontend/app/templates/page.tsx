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
  const [name, setName] = useState("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");

  const loadTemplates = () => {
    apiFetch<Template[]>("/api/templates").then(setTemplates);
  };

  useEffect(() => {
    loadTemplates();
  }, []);

  const handleCreate = async () => {
    await apiFetch<Template>("/api/admin/templates", {
      method: "POST",
      body: JSON.stringify({ name, subject, body }),
    });
    setName("");
    setSubject("");
    setBody("");
    loadTemplates();
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Outreach Studio</p>
        <h1 className="text-2xl font-semibold">Outreach Templates</h1>
        <p className="text-slate-400">Admin-only template management.</p>
      </header>
      <div className="grid lg:grid-cols-2 gap-6">
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4 shadow-[0_0_0_1px_rgba(16,185,129,0.12)]">
          <h2 className="text-lg font-semibold">Create template</h2>
          <input
            placeholder="Template name"
            className="w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 focus:border-emerald-400/60 focus:outline-none"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <input
            placeholder="Subject"
            className="w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 focus:border-emerald-400/60 focus:outline-none"
            value={subject}
            onChange={(event) => setSubject(event.target.value)}
          />
          <textarea
            placeholder="Body"
            className="w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 focus:border-emerald-400/60 focus:outline-none"
            value={body}
            onChange={(event) => setBody(event.target.value)}
            rows={6}
          />
          <button
            onClick={handleCreate}
            className="px-4 py-2 rounded-xl bg-gradient-to-r from-emerald-400 via-emerald-500 to-teal-500 text-slate-900 font-semibold shadow-lg shadow-emerald-500/20"
          >
            Save Template
          </button>
        </div>
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
    </div>
  );
}

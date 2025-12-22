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
      <header>
        <h1 className="text-2xl font-semibold">Outreach Templates</h1>
        <p className="text-slate-400">Admin-only template management.</p>
      </header>
      <div className="grid lg:grid-cols-2 gap-6">
        <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
          <h2 className="text-lg font-semibold">Create template</h2>
          <input
            placeholder="Template name"
            className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2"
            value={name}
            onChange={(event) => setName(event.target.value)}
          />
          <input
            placeholder="Subject"
            className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2"
            value={subject}
            onChange={(event) => setSubject(event.target.value)}
          />
          <textarea
            placeholder="Body"
            className="w-full bg-slate-950 border border-slate-700 rounded-lg p-2"
            value={body}
            onChange={(event) => setBody(event.target.value)}
            rows={6}
          />
          <button
            onClick={handleCreate}
            className="px-4 py-2 bg-emerald-500 text-slate-900 rounded-lg"
          >
            Save Template
          </button>
        </div>
        <div className="space-y-3">
          {templates.map((template) => (
            <div key={template.id} className="border border-slate-800 rounded-xl p-4">
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

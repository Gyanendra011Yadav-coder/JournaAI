"use client";

import { useEffect, useState } from "react";
import { useSearchParams } from "next/navigation";
import { apiFetch } from "../../../lib/api";
import { EmailComposer } from "../../../components/EmailComposer";

interface Template {
  id: number;
  name: string;
  subject: string;
  body: string;
}

export default function OutreachComposePage() {
  const searchParams = useSearchParams();
  const articleId = searchParams.get("articleId");
  const journalistId = searchParams.get("journalistId");
  const [templates, setTemplates] = useState<Template[]>([]);
  const [templateId, setTemplateId] = useState<string>("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [status, setStatus] = useState<string | null>(null);

  useEffect(() => {
    apiFetch<Template[]>("/api/templates").then((data) => {
      setTemplates(data);
      if (data.length > 0) {
        setTemplateId(String(data[0].id));
        setSubject(data[0].subject);
        setBody(data[0].body);
      }
    });
  }, []);

  const handleSend = async () => {
    if (!articleId || !journalistId || !templateId) return;
    const response = await apiFetch<{ status: string }>("/api/outreach/send", {
      method: "POST",
      body: JSON.stringify({
        articleId: Number(articleId),
        journalistId: Number(journalistId),
        templateId: Number(templateId),
        finalSubject: subject,
        finalBody: body,
      }),
    });
    setStatus(response.status);
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Outreach</p>
        <h1 className="text-2xl font-semibold">Compose Outreach</h1>
        <p className="text-slate-400">Draft an email and send from within the platform.</p>
      </header>
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4 shadow-[0_0_0_1px_rgba(59,130,246,0.1)]">
        <div>
          <label className="text-sm text-slate-300">Template</label>
          <select
            value={templateId}
            onChange={(event) => {
              const selected = templates.find((template) => template.id === Number(event.target.value));
              setTemplateId(event.target.value);
              if (selected) {
                setSubject(selected.subject);
                setBody(selected.body);
              }
            }}
            className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 focus:border-cyan-500/60 focus:outline-none"
          >
            {templates.map((template) => (
              <option key={template.id} value={template.id}>
                {template.name}
              </option>
            ))}
          </select>
        </div>
        <EmailComposer
          subject={subject}
          body={body}
          onSubjectChange={setSubject}
          onBodyChange={setBody}
        />
        <button
          onClick={handleSend}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 text-slate-900 font-semibold shadow-lg shadow-cyan-500/20"
        >
          Send Outreach
        </button>
        {status && <p className="text-emerald-400">Status: {status}</p>}
      </div>
    </div>
  );
}

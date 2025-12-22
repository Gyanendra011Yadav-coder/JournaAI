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
      <header>
        <h1 className="text-2xl font-semibold">Compose Outreach</h1>
        <p className="text-slate-400">Draft an email and send from within the platform.</p>
      </header>
      <div className="bg-slate-900 border border-slate-800 rounded-xl p-6 space-y-4">
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
            className="mt-1 w-full bg-slate-950 border border-slate-700 rounded-lg p-2"
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
          className="px-4 py-2 bg-cyan-500 text-slate-900 rounded-lg font-semibold"
        >
          Send Outreach
        </button>
        {status && <p className="text-emerald-400">Status: {status}</p>}
      </div>
    </div>
  );
}

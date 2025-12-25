"use client";

import { Suspense, useEffect, useMemo, useState } from "react";
import { useSearchParams } from "next/navigation";
import { apiFetch } from "../../../lib/api";
import { EmailComposer } from "../../../components/EmailComposer";
import { ErrorBanner } from "../../../components/ErrorBanner";

interface Template {
  id: number;
  name: string;
  subject: string;
  body: string;
}

interface Article {
  id: number;
  headline: string;
  beats: string[];
  canonicalUrl: string;
}

interface Journalist {
  id: number;
  fullName: string;
  publicationName?: string;
}

function OutreachComposeInner() {
  const searchParams = useSearchParams();
  const articleId = searchParams.get("articleId");
  const journalistId = searchParams.get("journalistId");
  const [templates, setTemplates] = useState<Template[]>([]);
  const [templateId, setTemplateId] = useState<string>("");
  const [subject, setSubject] = useState("");
  const [body, setBody] = useState("");
  const [status, setStatus] = useState<string | null>(null);
  const [article, setArticle] = useState<Article | null>(null);
  const [journalist, setJournalist] = useState<Journalist | null>(null);
  const [clientQuote, setClientQuote] = useState("We can share fresh data and executive commentary.");
  const [error, setError] = useState<string | null>(null);
  const [sending, setSending] = useState(false);

  useEffect(() => {
    apiFetch<Template[]>("/api/templates")
      .then((data) => {
        setTemplates(data);
        if (data.length > 0) {
          setTemplateId(String(data[0].id));
          setSubject(data[0].subject);
          setBody(data[0].body);
        }
        setError(null);
      })
      .catch((err) => setError(err instanceof Error ? err.message : "Unable to load templates."));
  }, []);

  useEffect(() => {
    if (articleId) {
      apiFetch<Article>(`/api/articles/${articleId}`)
        .then(setArticle)
        .catch((err) => setError(err instanceof Error ? err.message : "Unable to load article."));
    }
    if (journalistId) {
      apiFetch<Journalist>(`/api/journalists/${journalistId}`)
        .then(setJournalist)
        .catch((err) => setError(err instanceof Error ? err.message : "Unable to load journalist."));
    }
  }, [articleId, journalistId]);

  const context = useMemo(() => {
    return {
      journalist_name: journalist?.fullName ?? "",
      outlet: journalist?.publicationName ?? "",
      beat: article?.beats?.[0] ?? "",
      article_link: article?.canonicalUrl ?? "",
      headline: article?.headline ?? "",
      client_quote: clientQuote,
    };
  }, [article, clientQuote, journalist]);

  const applyTemplate = (template: Template) => {
    const render = (text: string) =>
      text.replace(/\{\{(\w+)\}\}/g, (_match, key) => {
        const value = (context as Record<string, string>)[key];
        return value ?? "";
      });
    setSubject(render(template.subject));
    setBody(render(template.body));
  };

  const handleApplyVariables = () => {
    const selected = templates.find((template) => template.id === Number(templateId));
    if (selected) {
      applyTemplate(selected);
    }
  };

  const handleSend = async () => {
    if (!articleId || !journalistId || !templateId) return;
    setSending(true);
    setError(null);
    try {
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
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to send outreach email.");
    } finally {
      setSending(false);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Outreach</p>
        <h1 className="text-2xl font-semibold">Compose Outreach</h1>
        <p className="text-slate-400">Draft an email and send from within the platform.</p>
      </header>
      <ErrorBanner message={error} />
      <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4 shadow-[0_0_0_1px_rgba(59,130,246,0.1)]">
        <div>
          <label className="text-sm text-slate-300">Template</label>
          <select
            value={templateId}
            onChange={(event) => {
              const selected = templates.find((template) => template.id === Number(event.target.value));
              setTemplateId(event.target.value);
              if (selected) {
                applyTemplate(selected);
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
        <div>
          <label className="text-sm text-slate-300">Client quote</label>
          <input
            value={clientQuote}
            onChange={(event) => setClientQuote(event.target.value)}
            className="mt-2 w-full rounded-xl bg-slate-900/60 border border-slate-700/80 p-3 focus:border-cyan-500/60 focus:outline-none"
          />
        </div>
        <button
          onClick={handleApplyVariables}
          className="px-3 py-2 rounded-xl border border-slate-700 text-slate-200 text-sm"
        >
          Apply template variables
        </button>
        <EmailComposer
          subject={subject}
          body={body}
          onSubjectChange={setSubject}
          onBodyChange={setBody}
        />
        <button
          onClick={handleSend}
          disabled={sending}
          className="px-4 py-2 rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 text-slate-900 font-semibold shadow-lg shadow-cyan-500/20 disabled:opacity-60"
        >
          <span className="inline-flex items-center gap-2">
            {sending && (
              <span className="h-3 w-3 animate-spin rounded-full border border-slate-700 border-t-transparent" />
            )}
            Send Outreach
          </span>
        </button>
        {status && <p className="text-emerald-400">Status: {status}</p>}
      </div>
    </div>
  );
}

export default function OutreachComposePage() {
  return (
    <Suspense fallback={<div>Loading outreach composer...</div>}>
      <OutreachComposeInner />
    </Suspense>
  );
}

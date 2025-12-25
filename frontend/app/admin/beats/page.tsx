"use client";

import { useEffect, useMemo, useState } from "react";
import { apiFetch } from "../../../lib/api";
import { ErrorBanner } from "../../../components/ErrorBanner";
import { getCountryOptions, getLanguageOptions } from "../../../lib/locale";

interface Beat {
  id: number;
  name: string;
  slug: string;
  active: boolean;
}

interface Template {
  id: number;
  beatId: number;
  endpointType: "SEARCH" | "TOP_HEADLINES";
  category?: string;
  beatTerms?: string[];
  langDefault?: string;
  countryDefault?: string;
  inDefault?: string;
  nullableFields?: string;
  maxDefault?: number;
  sortbyDefault?: string;
}

export default function AdminBeatsPage() {
  const [beats, setBeats] = useState<Beat[]>([]);
  const [templates, setTemplates] = useState<Template[]>([]);
  const [beatForm, setBeatForm] = useState({ name: "", slug: "", active: true });
  const [templateForm, setTemplateForm] = useState<Template>({
    id: 0,
    beatId: 0,
    endpointType: "SEARCH",
    category: "",
    beatTerms: [],
    langDefault: "en",
    countryDefault: "us",
    maxDefault: 25,
    sortbyDefault: "publishedAt",
  });
  const [error, setError] = useState<string | null>(null);
  const [creatingBeat, setCreatingBeat] = useState(false);
  const [updatingBeatId, setUpdatingBeatId] = useState<number | null>(null);
  const [deletingBeatId, setDeletingBeatId] = useState<number | null>(null);
  const [creatingTemplate, setCreatingTemplate] = useState(false);
  const [updatingTemplateId, setUpdatingTemplateId] = useState<number | null>(null);
  const [deletingTemplateId, setDeletingTemplateId] = useState<number | null>(null);
  const countryOptions = useMemo(() => getCountryOptions(), []);
  const languageOptions = useMemo(() => getLanguageOptions(), []);

  const loadData = async () => {
    try {
      const beatsData = await apiFetch<Beat[]>("/api/admin/beats");
      const templatesData = await apiFetch<Template[]>("/api/admin/beat-query-templates");
      setBeats(beatsData);
      setTemplates(templatesData);
      if (beatsData.length && templateForm.beatId === 0) {
        setTemplateForm((prev) => ({ ...prev, beatId: beatsData[0].id }));
      }
      setError(null);
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to load admin beats.");
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateBeat = async () => {
    setCreatingBeat(true);
    setError(null);
    try {
      await apiFetch("/api/admin/beats", {
        method: "POST",
        body: JSON.stringify(beatForm),
      });
      setBeatForm({ name: "", slug: "", active: true });
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to create beat.");
    } finally {
      setCreatingBeat(false);
    }
  };

  const handleUpdateBeat = async (beat: Beat) => {
    setUpdatingBeatId(beat.id);
    setError(null);
    try {
      await apiFetch(`/api/admin/beats/${beat.id}`, {
        method: "PUT",
        body: JSON.stringify({ name: beat.name, slug: beat.slug, active: beat.active }),
      });
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to update beat.");
    } finally {
      setUpdatingBeatId(null);
    }
  };

  const handleDeleteBeat = async (id: number) => {
    setDeletingBeatId(id);
    setError(null);
    try {
      await apiFetch(`/api/admin/beats/${id}`, { method: "DELETE" });
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to delete beat.");
    } finally {
      setDeletingBeatId(null);
    }
  };

  const handleCreateTemplate = async () => {
    setCreatingTemplate(true);
    setError(null);
    try {
      await apiFetch("/api/admin/beat-query-templates", {
        method: "POST",
        body: JSON.stringify({
          beatId: templateForm.beatId,
          endpointType: templateForm.endpointType,
          category: templateForm.category,
          beatTerms: templateForm.beatTerms,
          langDefault: templateForm.langDefault,
          countryDefault: templateForm.countryDefault,
          inDefault: templateForm.inDefault,
          nullableFields: templateForm.nullableFields,
          maxDefault: templateForm.maxDefault,
          sortbyDefault: templateForm.sortbyDefault,
        }),
      });
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to create template.");
    } finally {
      setCreatingTemplate(false);
    }
  };

  const handleUpdateTemplate = async (template: Template) => {
    setUpdatingTemplateId(template.id);
    setError(null);
    try {
      await apiFetch(`/api/admin/beat-query-templates/${template.id}`, {
        method: "PUT",
        body: JSON.stringify(template),
      });
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to update template.");
    } finally {
      setUpdatingTemplateId(null);
    }
  };

  const handleDeleteTemplate = async (id: number) => {
    setDeletingTemplateId(id);
    setError(null);
    try {
      await apiFetch(`/api/admin/beat-query-templates/${id}`, { method: "DELETE" });
      await loadData();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Unable to delete template.");
    } finally {
      setDeletingTemplateId(null);
    }
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">Beat Management</h1>
        <p className="text-slate-400">Define beats and their query templates for GNews ingestion.</p>
      </header>
      <ErrorBanner message={error} />

      <section className="grid gap-6 lg:grid-cols-2">
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <h2 className="text-lg font-semibold">Create beat</h2>
          <input
            placeholder="Beat name"
            value={beatForm.name}
            onChange={(event) => setBeatForm({ ...beatForm, name: event.target.value })}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          />
          <input
            placeholder="Slug"
            value={beatForm.slug}
            onChange={(event) => setBeatForm({ ...beatForm, slug: event.target.value })}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          />
          <label className="flex items-center gap-2 text-sm">
            <input
              type="checkbox"
              checked={beatForm.active}
              onChange={(event) => setBeatForm({ ...beatForm, active: event.target.checked })}
            />
            Active
          </label>
          <button
            onClick={handleCreateBeat}
            disabled={creatingBeat}
            className="rounded-xl bg-cyan-500 px-4 py-2 font-semibold text-slate-900 disabled:opacity-60"
          >
            <span className="inline-flex items-center gap-2">
              {creatingBeat && (
                <span className="h-3 w-3 animate-spin rounded-full border border-slate-900 border-t-transparent" />
              )}
              Add beat
            </span>
          </button>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <h2 className="text-lg font-semibold">Create template</h2>
          <select
            value={templateForm.beatId}
            onChange={(event) => setTemplateForm({ ...templateForm, beatId: Number(event.target.value) })}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          >
            {beats.map((beat) => (
              <option key={beat.id} value={beat.id}>
                {beat.name}
              </option>
            ))}
          </select>
          <select
            value={templateForm.endpointType}
            onChange={(event) =>
              setTemplateForm({ ...templateForm, endpointType: event.target.value as Template["endpointType"] })
            }
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          >
            <option value="SEARCH">Search</option>
            <option value="TOP_HEADLINES">Top headlines</option>
          </select>
          <input
            placeholder="Beat terms (comma-separated)"
            value={templateForm.beatTerms?.join(", ") ?? ""}
            onChange={(event) =>
              setTemplateForm({ ...templateForm, beatTerms: event.target.value.split(",").map((term) => term.trim()) })
            }
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          />
          <div className="grid gap-3 md:grid-cols-2">
            <input
              placeholder="Category"
              value={templateForm.category}
              onChange={(event) => setTemplateForm({ ...templateForm, category: event.target.value })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
            <input
              placeholder="Lang"
              value={templateForm.langDefault}
              onChange={(event) => setTemplateForm({ ...templateForm, langDefault: event.target.value })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              list="lang-options"
            />
            <input
              placeholder="Country"
              value={templateForm.countryDefault}
              onChange={(event) => setTemplateForm({ ...templateForm, countryDefault: event.target.value })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
              list="country-options"
            />
            <input
              placeholder="Max"
              type="number"
              value={templateForm.maxDefault ?? 0}
              onChange={(event) => setTemplateForm({ ...templateForm, maxDefault: Number(event.target.value) })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
          </div>
          <button
            onClick={handleCreateTemplate}
            disabled={creatingTemplate}
            className="rounded-xl bg-emerald-500 px-4 py-2 font-semibold text-slate-900 disabled:opacity-60"
          >
            <span className="inline-flex items-center gap-2">
              {creatingTemplate && (
                <span className="h-3 w-3 animate-spin rounded-full border border-slate-900 border-t-transparent" />
              )}
              Add template
            </span>
          </button>
        </div>
      </section>

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <h2 className="text-lg font-semibold">Beats</h2>
        <div className="mt-4 space-y-3">
          {beats.map((beat) => (
            <div key={beat.id} className="flex flex-wrap items-center gap-3 rounded-xl border border-slate-800/80 p-3">
              <input
                value={beat.name}
                onChange={(event) =>
                  setBeats((prev) => prev.map((item) => (item.id === beat.id ? { ...item, name: event.target.value } : item)))
                }
                className="flex-1 rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              />
              <input
                value={beat.slug}
                onChange={(event) =>
                  setBeats((prev) => prev.map((item) => (item.id === beat.id ? { ...item, slug: event.target.value } : item)))
                }
                className="flex-1 rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              />
              <label className="flex items-center gap-2 text-xs">
                <input
                  type="checkbox"
                  checked={beat.active}
                  onChange={(event) =>
                    setBeats((prev) => prev.map((item) => (item.id === beat.id ? { ...item, active: event.target.checked } : item)))
                  }
                />
                Active
              </label>
              <button
                onClick={() => handleUpdateBeat(beat)}
                disabled={updatingBeatId === beat.id}
                className="rounded-lg bg-cyan-500 px-3 py-1 text-xs text-slate-900 disabled:opacity-60"
              >
                <span className="inline-flex items-center gap-2">
                  {updatingBeatId === beat.id && (
                    <span className="h-3 w-3 animate-spin rounded-full border border-slate-900 border-t-transparent" />
                  )}
                  Update
                </span>
              </button>
              <button
                onClick={() => handleDeleteBeat(beat.id)}
                disabled={deletingBeatId === beat.id}
                className="rounded-lg border border-red-500/60 px-3 py-1 text-xs text-red-200 disabled:opacity-60"
              >
                <span className="inline-flex items-center gap-2">
                  {deletingBeatId === beat.id && (
                    <span className="h-3 w-3 animate-spin rounded-full border border-red-200 border-t-transparent" />
                  )}
                  Delete
                </span>
              </button>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <h2 className="text-lg font-semibold">Query templates</h2>
        <div className="mt-4 space-y-3">
          {templates.map((template) => (
            <div key={template.id} className="grid gap-2 rounded-xl border border-slate-800/80 p-3 md:grid-cols-6">
              <select
                value={template.beatId}
                onChange={(event) =>
                  setTemplates((prev) =>
                    prev.map((item) =>
                      item.id === template.id ? { ...item, beatId: Number(event.target.value) } : item
                    )
                  )
                }
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              >
                {beats.map((beat) => (
                  <option key={beat.id} value={beat.id}>
                    {beat.name}
                  </option>
                ))}
              </select>
              <select
                value={template.endpointType}
                onChange={(event) =>
                  setTemplates((prev) =>
                    prev.map((item) =>
                      item.id === template.id ? { ...item, endpointType: event.target.value as Template["endpointType"] } : item
                    )
                  )
                }
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              >
                <option value="SEARCH">Search</option>
                <option value="TOP_HEADLINES">Top</option>
              </select>
              <input
                value={template.beatTerms?.join(", ") ?? ""}
                onChange={(event) =>
                  setTemplates((prev) =>
                    prev.map((item) =>
                      item.id === template.id
                        ? { ...item, beatTerms: event.target.value.split(",").map((term) => term.trim()) }
                        : item
                    )
                  )
                }
                placeholder="Beat terms"
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              />
              <input
                value={template.langDefault ?? ""}
                onChange={(event) =>
                  setTemplates((prev) =>
                    prev.map((item) =>
                      item.id === template.id ? { ...item, langDefault: event.target.value } : item
                    )
                  )
                }
                placeholder="Lang"
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
                list="lang-options"
              />
              <input
                value={template.countryDefault ?? ""}
                onChange={(event) =>
                  setTemplates((prev) =>
                    prev.map((item) =>
                      item.id === template.id ? { ...item, countryDefault: event.target.value } : item
                    )
                  )
                }
                placeholder="Country"
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
                list="country-options"
              />
              <div className="flex items-center gap-2">
                <button
                  onClick={() => handleUpdateTemplate(template)}
                  disabled={updatingTemplateId === template.id}
                  className="rounded-lg bg-emerald-500 px-3 py-1 text-xs text-slate-900 disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {updatingTemplateId === template.id && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-slate-900 border-t-transparent" />
                    )}
                    Update
                  </span>
                </button>
                <button
                  onClick={() => handleDeleteTemplate(template.id)}
                  disabled={deletingTemplateId === template.id}
                  className="rounded-lg border border-red-500/60 px-3 py-1 text-xs text-red-200 disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {deletingTemplateId === template.id && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-red-200 border-t-transparent" />
                    )}
                    Delete
                  </span>
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
      <datalist id="country-options">
        {countryOptions.map((option) => (
          <option key={option.code} value={option.code}>
            {option.label}
          </option>
        ))}
      </datalist>
      <datalist id="lang-options">
        {languageOptions.map((option) => (
          <option key={option.code} value={option.code}>
            {option.label}
          </option>
        ))}
      </datalist>
    </div>
  );
}

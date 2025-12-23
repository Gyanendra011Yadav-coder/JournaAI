"use client";

import { useEffect, useState } from "react";
import { apiFetch } from "../../../lib/api";

interface Beat {
  id: number;
  name: string;
  slug: string;
  active: boolean;
}

interface Recipe {
  id: number;
  beatId: number;
  endpointType: "SEARCH" | "TOP_HEADLINES";
  query?: string;
  category?: string;
  lang?: string;
  country?: string;
  inFields?: string;
  nullableFields?: string;
  max?: number;
  sort?: string;
}

export default function AdminBeatsPage() {
  const [beats, setBeats] = useState<Beat[]>([]);
  const [recipes, setRecipes] = useState<Recipe[]>([]);
  const [beatForm, setBeatForm] = useState({ name: "", slug: "", active: true });
  const [recipeForm, setRecipeForm] = useState<Recipe>({
    id: 0,
    beatId: 0,
    endpointType: "SEARCH",
    query: "",
    category: "",
    lang: "en",
    country: "us",
    max: 25,
    sort: "publishedAt",
  });

  const loadData = async () => {
    const beatsData = await apiFetch<Beat[]>("/api/admin/beats");
    const recipesData = await apiFetch<Recipe[]>("/api/admin/beat-query-recipes");
    setBeats(beatsData);
    setRecipes(recipesData);
    if (beatsData.length && recipeForm.beatId === 0) {
      setRecipeForm((prev) => ({ ...prev, beatId: beatsData[0].id }));
    }
  };

  useEffect(() => {
    loadData();
  }, []);

  const handleCreateBeat = async () => {
    await apiFetch("/api/admin/beats", {
      method: "POST",
      body: JSON.stringify(beatForm),
    });
    setBeatForm({ name: "", slug: "", active: true });
    await loadData();
  };

  const handleUpdateBeat = async (beat: Beat) => {
    await apiFetch(`/api/admin/beats/${beat.id}`, {
      method: "PUT",
      body: JSON.stringify({ name: beat.name, slug: beat.slug, active: beat.active }),
    });
    await loadData();
  };

  const handleDeleteBeat = async (id: number) => {
    await apiFetch(`/api/admin/beats/${id}`, { method: "DELETE" });
    await loadData();
  };

  const handleCreateRecipe = async () => {
    await apiFetch("/api/admin/beat-query-recipes", {
      method: "POST",
      body: JSON.stringify({
        beatId: recipeForm.beatId,
        endpointType: recipeForm.endpointType,
        query: recipeForm.query,
        category: recipeForm.category,
        lang: recipeForm.lang,
        country: recipeForm.country,
        inFields: recipeForm.inFields,
        nullableFields: recipeForm.nullableFields,
        max: recipeForm.max,
        sort: recipeForm.sort,
      }),
    });
    await loadData();
  };

  const handleUpdateRecipe = async (recipe: Recipe) => {
    await apiFetch(`/api/admin/beat-query-recipes/${recipe.id}`, {
      method: "PUT",
      body: JSON.stringify(recipe),
    });
    await loadData();
  };

  const handleDeleteRecipe = async (id: number) => {
    await apiFetch(`/api/admin/beat-query-recipes/${id}`, { method: "DELETE" });
    await loadData();
  };

  return (
    <div className="space-y-6">
      <header className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <p className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Admin</p>
        <h1 className="text-2xl font-semibold">Beat Management</h1>
        <p className="text-slate-400">Define beats and their query recipes for GNews ingestion.</p>
      </header>

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
          <button onClick={handleCreateBeat} className="rounded-xl bg-cyan-500 px-4 py-2 font-semibold text-slate-900">
            Add beat
          </button>
        </div>
        <div className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6 space-y-4">
          <h2 className="text-lg font-semibold">Create recipe</h2>
          <select
            value={recipeForm.beatId}
            onChange={(event) => setRecipeForm({ ...recipeForm, beatId: Number(event.target.value) })}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          >
            {beats.map((beat) => (
              <option key={beat.id} value={beat.id}>
                {beat.name}
              </option>
            ))}
          </select>
          <select
            value={recipeForm.endpointType}
            onChange={(event) =>
              setRecipeForm({ ...recipeForm, endpointType: event.target.value as Recipe["endpointType"] })
            }
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          >
            <option value="SEARCH">Search</option>
            <option value="TOP_HEADLINES">Top headlines</option>
          </select>
          <input
            placeholder="Query"
            value={recipeForm.query}
            onChange={(event) => setRecipeForm({ ...recipeForm, query: event.target.value })}
            className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
          />
          <div className="grid gap-3 md:grid-cols-2">
            <input
              placeholder="Category"
              value={recipeForm.category}
              onChange={(event) => setRecipeForm({ ...recipeForm, category: event.target.value })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
            <input
              placeholder="Lang"
              value={recipeForm.lang}
              onChange={(event) => setRecipeForm({ ...recipeForm, lang: event.target.value })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
            <input
              placeholder="Country"
              value={recipeForm.country}
              onChange={(event) => setRecipeForm({ ...recipeForm, country: event.target.value })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
            <input
              placeholder="Max"
              type="number"
              value={recipeForm.max ?? 0}
              onChange={(event) => setRecipeForm({ ...recipeForm, max: Number(event.target.value) })}
              className="rounded-xl bg-slate-900/60 border border-slate-700/80 p-3"
            />
          </div>
          <button onClick={handleCreateRecipe} className="rounded-xl bg-emerald-500 px-4 py-2 font-semibold text-slate-900">
            Add recipe
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
              <button onClick={() => handleUpdateBeat(beat)} className="rounded-lg bg-cyan-500 px-3 py-1 text-xs text-slate-900">
                Update
              </button>
              <button onClick={() => handleDeleteBeat(beat.id)} className="rounded-lg border border-red-500/60 px-3 py-1 text-xs text-red-200">
                Delete
              </button>
            </div>
          ))}
        </div>
      </section>

      <section className="rounded-2xl border border-slate-800/80 bg-slate-900/60 p-6">
        <h2 className="text-lg font-semibold">Query recipes</h2>
        <div className="mt-4 space-y-3">
          {recipes.map((recipe) => (
            <div key={recipe.id} className="grid gap-2 rounded-xl border border-slate-800/80 p-3 md:grid-cols-6">
              <select
                value={recipe.beatId}
                onChange={(event) =>
                  setRecipes((prev) =>
                    prev.map((item) =>
                      item.id === recipe.id ? { ...item, beatId: Number(event.target.value) } : item
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
                value={recipe.endpointType}
                onChange={(event) =>
                  setRecipes((prev) =>
                    prev.map((item) =>
                      item.id === recipe.id ? { ...item, endpointType: event.target.value as Recipe["endpointType"] } : item
                    )
                  )
                }
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              >
                <option value="SEARCH">Search</option>
                <option value="TOP_HEADLINES">Top</option>
              </select>
              <input
                value={recipe.query ?? ""}
                onChange={(event) =>
                  setRecipes((prev) =>
                    prev.map((item) => (item.id === recipe.id ? { ...item, query: event.target.value } : item))
                  )
                }
                placeholder="Query"
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              />
              <input
                value={recipe.lang ?? ""}
                onChange={(event) =>
                  setRecipes((prev) =>
                    prev.map((item) => (item.id === recipe.id ? { ...item, lang: event.target.value } : item))
                  )
                }
                placeholder="Lang"
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              />
              <input
                value={recipe.country ?? ""}
                onChange={(event) =>
                  setRecipes((prev) =>
                    prev.map((item) => (item.id === recipe.id ? { ...item, country: event.target.value } : item))
                  )
                }
                placeholder="Country"
                className="rounded-lg bg-slate-900/60 border border-slate-700/80 p-2"
              />
              <div className="flex items-center gap-2">
                <button onClick={() => handleUpdateRecipe(recipe)} className="rounded-lg bg-emerald-500 px-3 py-1 text-xs text-slate-900">
                  Update
                </button>
                <button onClick={() => handleDeleteRecipe(recipe.id)} className="rounded-lg border border-red-500/60 px-3 py-1 text-xs text-red-200">
                  Delete
                </button>
              </div>
            </div>
          ))}
        </div>
      </section>
    </div>
  );
}

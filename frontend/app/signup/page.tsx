"use client";

import { useEffect, useMemo, useState } from "react";
import { useRouter } from "next/navigation";
import { apiFetch } from "../../lib/api";
import { ErrorBanner } from "../../components/ErrorBanner";
import { getCountryOptions, getLanguageOptions } from "../../lib/locale";

interface Beat {
  id: number;
  name: string;
}

export default function SignupPage() {
  const router = useRouter();
  const [step, setStep] = useState(1);
  const [beats, setBeats] = useState<Beat[]>([]);
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [country, setCountry] = useState("us");
  const [language, setLanguage] = useState("en");
  const [selectedBeats, setSelectedBeats] = useState<number[]>([]);
  const [clientKeywords, setClientKeywords] = useState("");
  const [excludeKeywords, setExcludeKeywords] = useState("");
  const [clients, setClients] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const countryOptions = useMemo(() => getCountryOptions(), []);
  const languageOptions = useMemo(() => getLanguageOptions(), []);

  useEffect(() => {
    apiFetch<Beat[]>("/api/beats")
      .then((data) => setBeats(data))
      .catch(() => setBeats([]));
  }, []);

  const toggleBeat = (beatId: number) => {
    setSelectedBeats((prev) =>
      prev.includes(beatId) ? prev.filter((id) => id !== beatId) : [...prev, beatId]
    );
  };

  const handleSubmit = async () => {
    setError(null);
    setSubmitting(true);
    try {
      const payload = {
        email,
        password,
        preferredCountries: [country],
        preferredLangs: [language],
        beatIds: selectedBeats,
        clientKeywords: clientKeywords
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        excludeKeywords: excludeKeywords
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean),
        clients: clients
          .split(",")
          .map((item) => item.trim())
          .filter(Boolean)
          .map((name) => ({ displayName: name })),
      };
      const data = await apiFetch<{ token: string }>("/api/auth/signup", {
        method: "POST",
        body: JSON.stringify(payload),
      });
      localStorage.setItem("token", data.token);
      router.push("/dashboard");
    } catch (err) {
      setError(err instanceof Error ? err.message : "Registration failed.");
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <div className="flex min-h-[calc(100vh-6rem)] items-center justify-center">
      <div className="grid w-full max-w-6xl gap-8 rounded-[32px] border border-slate-200/70 bg-white/90 p-8 shadow-[0_25px_60px_-45px_rgba(15,23,42,0.25)] lg:grid-cols-[1.1fr_0.9fr]">
        <div className="relative overflow-hidden rounded-2xl border border-slate-200/70 bg-gradient-to-br from-white via-slate-50 to-cyan-50 p-8">
          <div className="absolute -top-10 right-0 h-40 w-40 rounded-full bg-cyan-400/20 blur-3xl" />
          <div className="absolute bottom-0 left-10 h-40 w-40 rounded-full bg-indigo-400/20 blur-3xl" />
          <div className="relative space-y-6">
            <div className="text-xs uppercase tracking-[0.3em] text-cyan-600">Journo AI</div>
            <h1 className="text-3xl font-semibold text-slate-900">Personalize your news feed</h1>
            <p className="text-slate-600">
              Share your beats, clients, and language preferences to get a two-lens PR feed.
            </p>
            <div className="space-y-3 text-sm text-slate-600">
              {[
                "Trending mix delivers local + global headlines.",
                "Client lens highlights brand coverage.",
                "Beat lens keeps broader industry context.",
              ].map((item) => (
                <div key={item} className="flex items-center gap-3">
                  <span className="inline-flex h-6 w-6 items-center justify-center rounded-full bg-cyan-500/15 text-cyan-700">
                    ✓
                  </span>
                  <span>{item}</span>
                </div>
              ))}
            </div>
          </div>
        </div>
        <div className="rounded-2xl border border-slate-200/70 bg-white/90 p-8">
          <div className="flex items-center justify-between">
            <h2 className="text-xl font-semibold">Sign up</h2>
            <button
              type="button"
              onClick={() => router.push("/login")}
              className="text-sm text-cyan-600 hover:underline"
            >
              Already have an account?
            </button>
          </div>
          <p className="text-slate-600 mt-2">Step {step} of 2</p>
          {step === 1 && (
            <div className="mt-6 space-y-4">
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Email</label>
                <input
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3 focus:border-cyan-500/60 focus:outline-none"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Password</label>
                <input
                  type="password"
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3 focus:border-cyan-500/60 focus:outline-none"
                  value={password}
                  onChange={(event) => setPassword(event.target.value)}
                />
              </div>
              <button
                onClick={() => setStep(2)}
                className="w-full rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 py-3 font-semibold text-slate-900 shadow-lg shadow-cyan-500/20 transition hover:translate-y-[-1px]"
              >
                Continue to preferences
              </button>
            </div>
          )}
          {step === 2 && (
            <div className="mt-6 space-y-4">
              <div className="grid gap-4 md:grid-cols-2">
                <div>
                  <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Country</label>
                  <select
                    value={country}
                    onChange={(event) => setCountry(event.target.value)}
                    className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  >
                    {countryOptions.map((option) => (
                      <option key={option.code} value={option.code}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
                <div>
                  <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Language</label>
                  <select
                    value={language}
                    onChange={(event) => setLanguage(event.target.value)}
                    className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  >
                    {languageOptions.map((option) => (
                      <option key={option.code} value={option.code}>
                        {option.label}
                      </option>
                    ))}
                  </select>
                </div>
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">Beats</label>
                <div className="mt-3 flex flex-wrap gap-2">
                  {beats.map((beat) => (
                    <button
                      key={beat.id}
                      type="button"
                      onClick={() => toggleBeat(beat.id)}
                      className={`rounded-full border px-3 py-1 text-xs transition ${
                        selectedBeats.includes(beat.id)
                          ? "border-cyan-300/70 bg-cyan-50 text-cyan-700"
                          : "border-slate-200 text-slate-600 hover:border-cyan-200 hover:text-slate-900"
                      }`}
                    >
                      {beat.name}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">
                  Clients (comma-separated)
                </label>
                <input
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  value={clients}
                  onChange={(event) => setClients(event.target.value)}
                  placeholder="LKS, Acme Corp, Contoso"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">
                  Client keywords (comma-separated)
                </label>
                <input
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  value={clientKeywords}
                  onChange={(event) => setClientKeywords(event.target.value)}
                  placeholder="brand name, spokespeople"
                />
              </div>
              <div>
                <label className="text-xs uppercase tracking-[0.2em] text-slate-600">
                  Exclude keywords (comma-separated)
                </label>
                <input
                  className="mt-2 w-full rounded-xl bg-white/80 border border-slate-200 p-3"
                  value={excludeKeywords}
                  onChange={(event) => setExcludeKeywords(event.target.value)}
                  placeholder="sports, entertainment"
                />
              </div>
              <ErrorBanner message={error} />
              <div className="flex gap-3">
                <button
                  onClick={() => setStep(1)}
                  className="w-full rounded-xl border border-slate-200 py-3 text-sm text-slate-700"
                >
                  Back
                </button>
                <button
                  onClick={handleSubmit}
                  disabled={submitting}
                  className="w-full rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 py-3 font-semibold text-slate-900 shadow-lg shadow-cyan-500/20 transition hover:translate-y-[-1px] disabled:opacity-60"
                >
                  <span className="inline-flex items-center gap-2">
                    {submitting && (
                      <span className="h-3 w-3 animate-spin rounded-full border border-slate-200 border-t-transparent" />
                    )}
                    Finish setup
                  </span>
                </button>
              </div>
            </div>
          )}
          <div className="mt-6 rounded-xl border border-slate-200/70 bg-white/80 p-4 text-sm text-slate-600">
            Already running locally? Use <span className="font-semibold">admin@example.com</span> /{" "}
            <span className="font-semibold">password</span>.
          </div>
        </div>
      </div>
    </div>
  );
}

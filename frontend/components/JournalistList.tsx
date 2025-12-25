"use client";

import Link from "next/link";

interface Journalist {
  id: number;
  name: string;
  outlet: string;
  location: string;
  email: string;
  beats?: string[];
}

interface JournalistListProps {
  journalists: Journalist[];
  articleId: number;
}

export function JournalistList({ journalists, articleId }: JournalistListProps) {
  return (
    <div className="space-y-4">
      {journalists.map((journalist) => (
        <div key={journalist.id} className="rounded-2xl border border-slate-200/70 bg-white/85 p-5 shadow-[0_16px_30px_-28px_rgba(15,23,42,0.35)]">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-semibold">{journalist.name}</p>
              <p className="text-sm text-slate-600">
                {journalist.outlet} · {journalist.location}
              </p>
              {journalist.beats && journalist.beats.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-2">
                  {journalist.beats.map((beat) => (
                    <span key={beat} className="rounded-full bg-slate-100 px-2 py-1 text-xs text-slate-600">
                      {beat}
                    </span>
                  ))}
                </div>
              )}
            </div>
            <div className="text-right">
              <p className="text-sm text-slate-700">{journalist.email}</p>
              <Link
                href={`/outreach/compose?articleId=${articleId}&journalistId=${journalist.id}`}
                className="mt-2 inline-flex rounded-xl bg-gradient-to-r from-cyan-400 via-cyan-500 to-indigo-500 px-3 py-1 text-sm font-semibold text-slate-900 shadow-sm"
              >
                Compose outreach
              </Link>
            </div>
          </div>
        </div>
      ))}
    </div>
  );
}

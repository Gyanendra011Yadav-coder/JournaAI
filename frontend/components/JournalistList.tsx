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
    <div className="space-y-3">
      {journalists.map((journalist) => (
        <div key={journalist.id} className="p-4 border border-slate-800 rounded-lg">
          <div className="flex flex-wrap items-center justify-between gap-3">
            <div>
              <p className="font-semibold">{journalist.name}</p>
              <p className="text-sm text-slate-400">
                {journalist.outlet} · {journalist.location}
              </p>
              {journalist.beats && journalist.beats.length > 0 && (
                <div className="mt-2 flex flex-wrap gap-2">
                  {journalist.beats.map((beat) => (
                    <span key={beat} className="text-xs bg-slate-800 px-2 py-1 rounded-full">
                      {beat}
                    </span>
                  ))}
                </div>
              )}
            </div>
            <div className="text-right">
              <p className="text-sm text-cyan-300">{journalist.email}</p>
              <Link
                href={`/outreach/compose?articleId=${articleId}&journalistId=${journalist.id}`}
                className="mt-2 inline-flex px-3 py-1 rounded-lg bg-cyan-500 text-slate-900 text-sm"
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

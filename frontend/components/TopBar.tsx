"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { LogoutButton } from "./LogoutButton";

export function TopBar() {
  const pathname = usePathname();

  if (pathname === "/login" || pathname === "/signup" || pathname === "/register") {
    return null;
  }

  return (
    <header className="mb-8 flex flex-wrap items-center justify-between gap-6 rounded-[32px] border border-slate-200/70 bg-white/90 px-7 py-5 shadow-[0_25px_60px_-45px_rgba(15,23,42,0.35)] backdrop-blur">
      <div className="flex items-center gap-4">
        <div className="flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 via-sky-500 to-indigo-500 text-lg font-semibold text-white shadow-[0_10px_20px_-12px_rgba(14,116,144,0.8)]">
          J
        </div>
        <div>
          <p className="text-xs uppercase tracking-[0.35em] text-cyan-600">Journo AI</p>
          <p className="text-xl font-semibold text-slate-900">PR News & Outreach</p>
          <p className="text-sm text-slate-600">
            Media intelligence and outreach inspired by Muck Rack and Cision.
          </p>
        </div>
      </div>
      <div className="flex items-center gap-3">
        <Link
          href="/profile"
          className="rounded-full border border-slate-200 bg-white px-4 py-2 text-sm font-semibold text-slate-700 shadow-sm hover:border-cyan-300 hover:text-slate-900"
        >
          Profile
        </Link>
        <LogoutButton
          label="Sign out"
          className="rounded-full border border-rose-200 bg-rose-50 px-4 py-2 text-sm font-semibold text-rose-700 shadow-sm hover:border-rose-300 hover:bg-rose-100"
        />
      </div>
    </header>
  );
}

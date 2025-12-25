"use client";

import { usePathname } from "next/navigation";
import { LogoutButton } from "./LogoutButton";

export function TopBar() {
  const pathname = usePathname();

  if (pathname === "/login" || pathname === "/signup" || pathname === "/register") {
    return null;
  }

  return (
    <div className="mb-8 flex flex-wrap items-center justify-between gap-4 rounded-2xl border border-slate-800/80 bg-slate-900/60 px-4 py-3 shadow-[0_0_0_1px_rgba(59,130,246,0.1)]">
      <div>
        <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Today</p>
        <p className="text-sm text-slate-200">Monitor coverage, orchestrate outreach, ship wins.</p>
      </div>
      <LogoutButton className="rounded-xl border border-cyan-500/60 bg-cyan-500/10 px-4 py-2 text-sm font-semibold text-cyan-100 hover:bg-cyan-500/20" />
    </div>
  );
}

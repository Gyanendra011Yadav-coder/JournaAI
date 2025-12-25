"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch } from "../lib/api";
import { LogoutButton } from "./LogoutButton";

const navItems = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/search", label: "Search" },
  { href: "/trending", label: "Trending" },
  { href: "/saved", label: "Saved Articles" },
  { href: "/profile", label: "Profile" },
];

const adminItems = [
  { href: "/admin/integrations", label: "Admin Integrations" },
  { href: "/admin/beats", label: "Admin Beats" },
  { href: "/admin/publishing", label: "Publishing" },
  { href: "/admin/enrichment", label: "Enrichment Queue" },
  { href: "/admin/journalists", label: "Journalists" },
  { href: "/admin/journalists/import", label: "CSV Import" },
  { href: "/admin/audit", label: "Audit Log" },
];

export function Sidebar() {
  const pathname = usePathname();
  const [role, setRole] = useState<string | null>(null);
  const isAuthPage = pathname === "/login" || pathname === "/register" || pathname === "/signup";

  useEffect(() => {
    if (isAuthPage) {
      setRole(null);
      return;
    }
    const loadProfile = async () => {
      try {
        const data = await apiFetch<{ role: string }>("/api/auth/me");
        setRole(data.role);
        const profile = await apiFetch<{ defaultSidebarMode?: string }>("/api/me/profile");
        if (!profile.defaultSidebarMode) {
          await apiFetch("/api/me/profile", {
            method: "PUT",
            body: JSON.stringify({ defaultSidebarMode: "TRENDING" }),
          });
        }
      } catch {
        setRole(null);
      }
    };
    loadProfile();
  }, [isAuthPage]);

  if (isAuthPage) {
    return null;
  }

  return (
    <aside className="w-72 bg-slate-950/80 border-r border-slate-800/80 p-6 hidden md:flex md:flex-col gap-10 backdrop-blur">
      <div className="space-y-2">
        <div className="text-xs uppercase tracking-[0.2em] text-cyan-300/80">Journa AI</div>
        <div className="text-2xl font-semibold">PR News & Outreach</div>
        <p className="text-sm text-slate-400">Cache-first news, beat configuration, and publishing.</p>
      </div>
      <nav className="space-y-2 flex-1">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`group flex items-center justify-between px-3 py-2 rounded-xl border transition ${
              pathname === item.href
                ? "border-cyan-500/60 bg-cyan-500/10 text-cyan-100"
                : "border-transparent text-slate-200 hover:border-slate-700/80 hover:bg-slate-900/60"
            }`}
          >
            <span>{item.label}</span>
            <span className="text-xs text-slate-400 group-hover:text-slate-200">↗</span>
          </Link>
        ))}
        {role === "ADMIN" && (
          <div className="pt-4 space-y-2">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-500 px-3">Admin</p>
            {adminItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={`group flex items-center justify-between px-3 py-2 rounded-xl border transition ${
                  pathname === item.href
                    ? "border-emerald-500/60 bg-emerald-500/10 text-emerald-100"
                    : "border-transparent text-slate-200 hover:border-slate-700/80 hover:bg-slate-900/60"
                }`}
              >
                <span>{item.label}</span>
                <span className="text-xs text-slate-400 group-hover:text-slate-200">↗</span>
              </Link>
            ))}
          </div>
        )}
      </nav>
      <div className="space-y-4 rounded-2xl border border-slate-800/80 bg-slate-900/60 p-4">
        <div>
          <p className="text-xs uppercase tracking-[0.2em] text-slate-400">Workspace</p>
          <p className="font-semibold">Agency Workspace</p>
          <p className="text-xs text-slate-400">admin@example.com</p>
        </div>
        <LogoutButton className="w-full rounded-xl border border-cyan-500/60 bg-cyan-500/10 px-3 py-2 text-sm font-semibold text-cyan-100 hover:bg-cyan-500/20" />
      </div>
    </aside>
  );
}

"use client";

import Link from "next/link";
import { usePathname } from "next/navigation";
import { useEffect, useState } from "react";
import { apiFetch } from "../lib/api";

const navItems = [
  { href: "/dashboard", label: "Dashboard" },
  { href: "/search", label: "Search" },
  { href: "/trending", label: "Trending" },
  { href: "/saved", label: "Saved Articles" },
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
        try {
          const profile = await apiFetch<{ defaultSidebarMode?: string }>("/api/me/profile");
          if (!profile.defaultSidebarMode || profile.defaultSidebarMode === "TRENDING") {
            await apiFetch("/api/me/profile", {
              method: "PUT",
              body: JSON.stringify({ defaultSidebarMode: "DASHBOARD" }),
            });
          }
        } catch {
          // Keep role even if profile update fails.
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
    <aside className="w-72 border-r border-slate-200/70 bg-white/85 p-7 hidden md:flex md:flex-col gap-10 backdrop-blur">
      <div className="space-y-4">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-2xl bg-gradient-to-br from-cyan-500 via-sky-500 to-indigo-500 text-white font-semibold">
            J
          </div>
          <div>
            <p className="text-xs uppercase tracking-[0.35em] text-cyan-600">Journo AI</p>
            <p className="text-lg font-semibold text-slate-900">Newsroom Desk</p>
          </div>
        </div>
        <p className="text-sm text-slate-600">
          Coverage intel, beat curation, and outreach workflows in one view.
        </p>
      </div>
      <nav className="space-y-2 flex-1">
        {navItems.map((item) => (
          <Link
            key={item.href}
            href={item.href}
            className={`group flex items-center justify-between px-4 py-3 rounded-2xl border transition ${
              pathname === item.href
                ? "border-cyan-500/50 bg-cyan-500/10 text-cyan-700"
                : "border-transparent text-slate-700 hover:border-slate-200 hover:bg-white"
            }`}
          >
            <span>{item.label}</span>
            <span className="text-xs text-slate-500 group-hover:text-slate-700">↗</span>
          </Link>
        ))}
        {role === "ADMIN" && (
          <div className="pt-4 space-y-2">
            <p className="text-xs uppercase tracking-[0.2em] text-slate-500 px-3">Admin</p>
            {adminItems.map((item) => (
              <Link
                key={item.href}
                href={item.href}
                className={`group flex items-center justify-between px-4 py-3 rounded-2xl border transition ${
                  pathname === item.href
                    ? "border-emerald-500/50 bg-emerald-500/10 text-emerald-700"
                    : "border-transparent text-slate-700 hover:border-slate-200 hover:bg-white"
                }`}
              >
                <span>{item.label}</span>
                <span className="text-xs text-slate-500 group-hover:text-slate-700">↗</span>
              </Link>
            ))}
          </div>
        )}
      </nav>
      <div className="space-y-3 rounded-2xl border border-slate-200/70 bg-white/80 p-4">
        <div>
          <p className="text-xs uppercase tracking-[0.35em] text-slate-500">Workspace</p>
          <p className="font-semibold text-slate-900">Agency Workspace</p>
          <p className="text-xs text-slate-500">admin@example.com</p>
        </div>
        <p className="text-xs text-slate-500">Switch teams from the profile menu.</p>
      </div>
    </aside>
  );
}

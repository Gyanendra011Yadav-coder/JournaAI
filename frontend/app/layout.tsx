import "./globals.css";
import type { Metadata } from "next";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";

export const metadata: Metadata = {
  title: "PR Control Tower",
  description: "Streamline PR workflows",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <div className="min-h-screen flex bg-slate-950 text-slate-100">
          <Sidebar />
          <main className="relative flex-1 p-6 md:p-8 overflow-hidden">
            <div className="pointer-events-none absolute -top-24 right-0 h-64 w-64 rounded-full bg-cyan-500/10 blur-3xl" />
            <div className="pointer-events-none absolute bottom-0 left-20 h-72 w-72 rounded-full bg-indigo-500/10 blur-3xl" />
            <div className="relative">
              <TopBar />
              {children}
            </div>
          </main>
        </div>
      </body>
    </html>
  );
}

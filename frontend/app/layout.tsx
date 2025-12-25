import "./globals.css";
import type { Metadata } from "next";
import { Sora, Source_Sans_3 } from "next/font/google";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { GlobalErrorBanner } from "../components/GlobalErrorBanner";

export const metadata: Metadata = {
  title: "Journo AI",
  description: "Cache-first PR news and outreach workflow",
};

const displayFont = Sora({
  subsets: ["latin"],
  variable: "--font-display",
  weight: ["400", "500", "600", "700"],
});

const bodyFont = Source_Sans_3({
  subsets: ["latin"],
  variable: "--font-body",
  weight: ["400", "500", "600", "700"],
});

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body className={`${displayFont.variable} ${bodyFont.variable} font-body`}>
        <div className="min-h-screen flex bg-slate-50 text-slate-900">
          <Sidebar />
          <main className="relative flex-1 p-8 md:p-12 overflow-hidden">
            <div className="pointer-events-none absolute -top-32 right-0 h-80 w-80 rounded-full bg-cyan-400/20 blur-[120px]" />
            <div className="pointer-events-none absolute top-24 left-12 h-64 w-64 rounded-full bg-amber-300/20 blur-[100px]" />
            <div className="pointer-events-none absolute bottom-0 left-1/3 h-72 w-72 rounded-full bg-indigo-400/20 blur-[120px]" />
            <div className="relative">
              <TopBar />
              <GlobalErrorBanner />
              {children}
            </div>
          </main>
        </div>
      </body>
    </html>
  );
}

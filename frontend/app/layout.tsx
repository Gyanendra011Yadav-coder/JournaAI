import "./globals.css";
import type { Metadata } from "next";
import { Sidebar } from "../components/Sidebar";

export const metadata: Metadata = {
  title: "PR Control Tower",
  description: "Streamline PR workflows",
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <div className="min-h-screen flex">
          <Sidebar />
          <main className="flex-1 p-8 bg-slate-950">
            {children}
          </main>
        </div>
      </body>
    </html>
  );
}

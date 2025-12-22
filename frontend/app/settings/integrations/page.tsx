export default function IntegrationsPage() {
  return (
    <div className="space-y-6">
      <header>
        <h1 className="text-2xl font-semibold">Integrations</h1>
        <p className="text-slate-400">Configure vendor APIs securely on the backend.</p>
      </header>
      <div className="grid md:grid-cols-2 gap-4">
        {[
          { title: "News Provider", env: "NEWSAPI_KEY", status: "Mock enabled" },
          { title: "Media Database", env: "CISION_API_KEY / MUCKRACK_API_KEY", status: "Mock enabled" },
          { title: "Email", env: "SMTP_HOST + SMTP_USER", status: "Mock enabled" },
        ].map((item) => (
          <div key={item.title} className="border border-slate-800 rounded-xl p-4 bg-slate-900">
            <h2 className="font-semibold">{item.title}</h2>
            <p className="text-sm text-slate-400">Env vars: {item.env}</p>
            <span className="text-xs text-emerald-400">{item.status}</span>
          </div>
        ))}
      </div>
    </div>
  );
}

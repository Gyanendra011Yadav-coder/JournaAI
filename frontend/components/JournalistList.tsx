interface Journalist {
  id: number;
  name: string;
  outlet: string;
  location: string;
  email: string;
}

interface JournalistListProps {
  journalists: Journalist[];
}

export function JournalistList({ journalists }: JournalistListProps) {
  return (
    <div className="space-y-3">
      {journalists.map((journalist) => (
        <div key={journalist.id} className="p-4 border border-slate-800 rounded-lg">
          <div className="flex items-center justify-between">
            <div>
              <p className="font-semibold">{journalist.name}</p>
              <p className="text-sm text-slate-400">{journalist.outlet} · {journalist.location}</p>
            </div>
            <span className="text-sm text-cyan-300">{journalist.email}</span>
          </div>
        </div>
      ))}
    </div>
  );
}

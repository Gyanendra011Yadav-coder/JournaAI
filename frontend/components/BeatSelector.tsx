"use client";

interface Beat {
  id: number;
  name: string;
}

interface BeatSelectorProps {
  value: number | null;
  beats: Beat[];
  onChange: (value: number) => void;
}

export function BeatSelector({ value, beats, onChange }: BeatSelectorProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {beats.map((beat) => (
        <button
          key={beat.id}
          onClick={() => onChange(beat.id)}
          className={`px-3 py-1 rounded-full text-sm border transition ${
            value === beat.id
              ? "bg-cyan-500/10 text-cyan-700 border-cyan-400/70"
              : "border-slate-200 text-slate-600 hover:border-cyan-300 hover:text-slate-900"
          }`}
        >
          {beat.name}
        </button>
      ))}
    </div>
  );
}

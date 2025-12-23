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
          className={`px-3 py-1 rounded-full text-sm border ${
            value === beat.id
              ? "bg-cyan-500 text-slate-900 border-cyan-400"
              : "border-slate-700 text-slate-200"
          }`}
        >
          {beat.name}
        </button>
      ))}
    </div>
  );
}

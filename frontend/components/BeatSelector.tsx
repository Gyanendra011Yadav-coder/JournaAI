"use client";

interface BeatSelectorProps {
  value: string;
  beats: string[];
  onChange: (value: string) => void;
}

export function BeatSelector({ value, beats, onChange }: BeatSelectorProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {beats.map((beat) => (
        <button
          key={beat}
          onClick={() => onChange(beat)}
          className={`px-3 py-1 rounded-full text-sm border ${
            value === beat
              ? "bg-cyan-500 text-slate-900 border-cyan-400"
              : "border-slate-700 text-slate-200"
          }`}
        >
          {beat}
        </button>
      ))}
    </div>
  );
}

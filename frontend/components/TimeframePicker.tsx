"use client";

const options = ["24h", "7d", "30d", "Custom"]; 

interface TimeframePickerProps {
  value: string;
  onChange: (value: string) => void;
}

export function TimeframePicker({ value, onChange }: TimeframePickerProps) {
  return (
    <div className="flex flex-wrap gap-2">
      {options.map((option) => (
        <button
          key={option}
          onClick={() => onChange(option)}
          className={`px-3 py-1 rounded-lg text-sm border transition ${
            value === option
              ? "bg-emerald-500/10 text-emerald-700 border-emerald-400/70"
              : "border-slate-200 text-slate-600 hover:border-emerald-300 hover:text-slate-900"
          }`}
        >
          {option}
        </button>
      ))}
    </div>
  );
}

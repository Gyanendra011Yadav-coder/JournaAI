const options = ["24h", "7d", "30d", "Custom"]; 

interface TimeframePickerProps {
  value: string;
  onChange: (value: string) => void;
}

export function TimeframePicker({ value, onChange }: TimeframePickerProps) {
  return (
    <div className="flex gap-2">
      {options.map((option) => (
        <button
          key={option}
          onClick={() => onChange(option)}
          className={`px-3 py-1 rounded-lg text-sm border ${
            value === option
              ? "bg-emerald-500 text-slate-900 border-emerald-400"
              : "border-slate-700 text-slate-200"
          }`}
        >
          {option}
        </button>
      ))}
    </div>
  );
}

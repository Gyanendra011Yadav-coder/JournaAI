interface EmailComposerProps {
  subject: string;
  body: string;
  onSubjectChange: (value: string) => void;
  onBodyChange: (value: string) => void;
}

export function EmailComposer({ subject, body, onSubjectChange, onBodyChange }: EmailComposerProps) {
  return (
    <div className="space-y-4">
      <div>
        <label className="text-sm text-slate-600">Subject</label>
        <input
          value={subject}
          onChange={(event) => onSubjectChange(event.target.value)}
          className="mt-2 w-full rounded-xl border border-slate-200 bg-white/90 p-3"
        />
      </div>
      <div>
        <label className="text-sm text-slate-600">Body</label>
        <textarea
          value={body}
          onChange={(event) => onBodyChange(event.target.value)}
          rows={8}
          className="mt-2 w-full rounded-xl border border-slate-200 bg-white/90 p-3"
        />
      </div>
    </div>
  );
}

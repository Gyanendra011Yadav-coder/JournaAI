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
        <label className="text-sm text-slate-300">Subject</label>
        <input
          value={subject}
          onChange={(event) => onSubjectChange(event.target.value)}
          className="mt-1 w-full rounded-lg bg-slate-900 border border-slate-700 p-2"
        />
      </div>
      <div>
        <label className="text-sm text-slate-300">Body</label>
        <textarea
          value={body}
          onChange={(event) => onBodyChange(event.target.value)}
          rows={8}
          className="mt-1 w-full rounded-lg bg-slate-900 border border-slate-700 p-2"
        />
      </div>
    </div>
  );
}

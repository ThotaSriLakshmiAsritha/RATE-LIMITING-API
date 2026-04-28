import { AlertTriangle } from 'lucide-react';
import { Button } from './Button';
import { Card } from './Card';

export function ConfirmDialog({
  open,
  title,
  description,
  onConfirm,
  onCancel,
}: {
  open: boolean;
  title: string;
  description: string;
  onConfirm: () => void;
  onCancel: () => void;
}) {
  if (!open) return null;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/75 p-4">
      <Card className="w-full max-w-md p-6">
        <div className="flex items-start gap-3">
          <AlertTriangle className="mt-1 h-5 w-5 text-amber-300" />
          <div>
            <h3 className="font-display text-xl text-slate-100">{title}</h3>
            <p className="mt-2 text-sm text-slate-400">{description}</p>
          </div>
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={onCancel} aria-label="Cancel confirmation">
            Cancel
          </Button>
          <Button variant="danger" onClick={onConfirm} aria-label="Confirm destructive action">
            Confirm
          </Button>
        </div>
      </Card>
    </div>
  );
}

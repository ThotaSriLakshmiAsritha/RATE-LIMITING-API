import { useEffect, useState } from 'react';
import type { Policy, PolicyFormValues } from '../../types';
import { Button } from '../common/Button';
import { Card } from '../common/Card';

const defaults: PolicyFormValues = {
  tenantId: 'default',
  scopeType: 'GLOBAL',
  scopeId: '',
  endpointPattern: '',
  requestsPerMinute: 60,
  burstCapacity: 10,
  dimension: 'USER',
  errorMessage: 'Rate limit exceeded',
  mode: 'ENFORCE',
  priority: 100,
  enabled: true,
};

export function PolicyModal({
  open,
  policy,
  onClose,
  onSave,
}: {
  open: boolean;
  policy?: Policy | null;
  onClose: () => void;
  onSave: (values: PolicyFormValues) => Promise<void>;
}) {
  const [values, setValues] = useState<PolicyFormValues>(defaults);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    if (!policy) {
      setValues(defaults);
      return;
    }
    setValues({
      tenantId: policy.tenantId,
      scopeType: policy.scopeType,
      scopeId: policy.scopeId ?? '',
      endpointPattern: policy.endpointPattern ?? '',
      requestsPerMinute: policy.requestsPerMinute ?? 60,
      burstCapacity: policy.burstCapacity ?? 10,
      dimension: policy.dimension ?? 'USER',
      errorMessage: policy.errorMessage ?? '',
      mode: policy.mode ?? 'ENFORCE',
      priority: policy.priority ?? 100,
      enabled: true,
    });
  }, [policy]);

  if (!open) return null;

  const updateField = (key: keyof PolicyFormValues, value: string | number | boolean) =>
    setValues((current) => ({ ...current, [key]: value }));

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-slate-950/75 p-4">
      <Card className="w-full max-w-3xl p-6">
        <h3 className="font-display text-2xl text-white">{policy ? 'Edit policy' : 'Add policy'}</h3>
        <div className="mt-5 grid gap-4 md:grid-cols-2">
          {[
            ['tenantId', 'Tenant ID'],
            ['scopeType', 'Scope'],
            ['scopeId', 'Dimension value'],
            ['endpointPattern', 'Endpoint pattern'],
            ['requestsPerMinute', 'Requests per minute'],
            ['burstCapacity', 'Burst capacity'],
            ['dimension', 'Dimension'],
            ['errorMessage', 'Error message'],
            ['mode', 'Mode'],
            ['priority', 'Priority'],
          ].map(([key, label]) => (
            <label key={key} className="grid gap-2 text-sm text-slate-300">
              {label}
              <input
                aria-label={label}
                value={String(values[key as keyof PolicyFormValues])}
                onChange={(event) =>
                  updateField(
                    key as keyof PolicyFormValues,
                    ['requestsPerMinute', 'burstCapacity', 'priority'].includes(key) ? Number(event.target.value) : event.target.value,
                  )
                }
                className="rounded-xl border border-slate-700 bg-slate-900 px-3 py-2 text-slate-100 outline-none focus:border-sky-400"
              />
            </label>
          ))}
        </div>
        <div className="mt-6 flex justify-end gap-3">
          <Button variant="secondary" onClick={onClose} aria-label="Cancel policy editing">
            Cancel
          </Button>
          <Button
            onClick={async () => {
              setSaving(true);
              try {
                await onSave(values);
              } finally {
                setSaving(false);
              }
            }}
            disabled={saving}
            aria-label="Save policy"
          >
            {saving ? 'Saving...' : 'Save policy'}
          </Button>
        </div>
      </Card>
    </div>
  );
}

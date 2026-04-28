import { useEffect, useMemo, useState } from 'react';
import { createPolicy, getPolicies, updatePolicy } from '../api/admin';
import { AuditTable } from '../components/admin/AuditTable';
import { PolicyModal } from '../components/admin/PolicyModal';
import { PolicyTable } from '../components/admin/PolicyTable';
import { Button } from '../components/common/Button';
import { Card } from '../components/common/Card';
import { EmptyState } from '../components/common/EmptyState';
import { useAppContext } from '../context/AppContext';
import { usePageTitle } from '../hooks/usePageTitle';
import type { Policy, PolicyFormValues, StreamEvent } from '../types';

async function connectDashboardStream(baseUrl: string, token: string, onEvent: (event: StreamEvent) => void) {
  const response = await fetch(`${baseUrl}/api/dashboard/stream?tenantId=default`, {
    headers: { Authorization: `Bearer ${token}` },
  });
  if (!response.ok || !response.body) {
    throw new Error('Unable to open stream');
  }

  const reader = response.body.getReader();
  const decoder = new TextDecoder();
  let buffer = '';

  while (true) {
    const { done, value } = await reader.read();
    if (done) break;
    buffer += decoder.decode(value, { stream: true });
    const chunks = buffer.split('\n\n');
    buffer = chunks.pop() ?? '';

    chunks.forEach((chunk) => {
      const lines = chunk.split('\n');
      const dataLine = lines.find((line) => line.startsWith('data:'));
      if (!dataLine) return;
      try {
        const parsed = JSON.parse(dataLine.replace(/^data:\s*/, ''));
        onEvent({
          id: crypto.randomUUID(),
          type: parsed.type ?? 'event',
          emittedAt: parsed.emittedAt ?? new Date().toISOString(),
          payload: parsed.payload ?? parsed,
        });
      } catch {
        onEvent({
          id: crypto.randomUUID(),
          type: 'raw',
          emittedAt: new Date().toISOString(),
          payload: dataLine.replace(/^data:\s*/, ''),
        });
      }
    });
  }
}

export function Admin() {
  usePageTitle('Admin');
  const { state, addToast } = useAppContext();
  const [policies, setPolicies] = useState<Policy[]>([]);
  const [events, setEvents] = useState<StreamEvent[]>([]);
  const [loading, setLoading] = useState(true);
  const [openModal, setOpenModal] = useState(false);
  const [editing, setEditing] = useState<Policy | null>(null);

  const roleDenied = state.role !== 'ADMIN';

  useEffect(() => {
    if (roleDenied || !state.authToken) return;
    let active = true;
    const load = async () => {
      setLoading(true);
      try {
        const response = await getPolicies(state.apiBaseUrl, state.authToken!);
        if (active) setPolicies(response.data);
      } catch {
        if (active) setPolicies([]);
      } finally {
        if (active) setLoading(false);
      }
    };
    load();
    return () => {
      active = false;
    };
  }, [roleDenied, state.apiBaseUrl, state.authToken]);

  useEffect(() => {
    if (roleDenied || !state.authToken) return;
    let cancelled = false;
    connectDashboardStream(state.apiBaseUrl, state.authToken, (event) => {
      if (!cancelled) setEvents((current) => [event, ...current].slice(0, 10));
    }).catch(() => {
      if (!cancelled) {
        addToast({
          tone: 'info',
          title: 'Realtime stream unavailable',
          description: 'The dashboard event stream did not open. Policy management still works through REST calls.',
        });
      }
    });
    return () => {
      cancelled = true;
    };
  }, [roleDenied, state.apiBaseUrl, state.authToken]);

  const emptyState = useMemo(
    () =>
      roleDenied ? (
        <EmptyState title="Access denied" description={`Admin role required. Your current role is ${state.role ?? 'USER'}. The seeded demo account is not an admin account.`} />
      ) : loading ? (
        <Card className="p-5">Loading policies...</Card>
      ) : (
        <EmptyState title="No policies returned" description="The backend policy table is empty or your account could not read it." />
      ),
    [loading, roleDenied, state.role],
  );

  if (roleDenied) {
    return emptyState;
  }

  const savePolicy = async (values: PolicyFormValues) => {
    if (!state.authToken) return;
    const response = editing
      ? await updatePolicy(state.apiBaseUrl, state.authToken, editing.id, values)
      : await createPolicy(state.apiBaseUrl, state.authToken, values);
    setPolicies((current) => {
      const remaining = current.filter((policy) => policy.id !== response.data.id);
      return [response.data, ...remaining];
    });
    setOpenModal(false);
    setEditing(null);
    addToast({
      tone: 'success',
      title: editing ? 'Policy updated' : 'Policy created',
      description: 'The backend accepted the policy mutation.',
    });
  };

  return (
    <div className="grid gap-6">
      <Card className="p-5">
        <div className="flex flex-wrap items-center justify-between gap-4">
          <div>
            <h2 className="font-display text-2xl text-white">Admin Panel</h2>
            <p className="mt-2 text-sm leading-7 text-slate-300">
              Rate limit policies define the rules. Each policy sets request and burst capacity for a scope such as global or per-user. This page is wired to the real `/api/dashboard/policies` endpoints available in the backend today.
            </p>
          </div>
          <Button onClick={() => setOpenModal(true)} aria-label="Add policy">
            Add Policy
          </Button>
        </div>
      </Card>

      {policies.length > 0 ? <PolicyTable policies={policies} onEdit={(policy) => { setEditing(policy); setOpenModal(true); }} /> : emptyState}

      <AuditTable events={events} />

      <Card className="p-5">
        <h3 className="font-display text-xl text-white">Export / Import Status</h3>
        <p className="mt-2 text-sm text-slate-300">
          The target spec calls for `/api/admin/policies/export` and `/api/admin/policies/import`, but the current backend build does not expose those endpoints yet. The UI keeps that gap visible rather than pretending the feature exists.
        </p>
      </Card>

      <PolicyModal open={openModal} policy={editing} onClose={() => { setOpenModal(false); setEditing(null); }} onSave={savePolicy} />
    </div>
  );
}

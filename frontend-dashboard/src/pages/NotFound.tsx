import { Card } from '../components/common/Card';
import { usePageTitle } from '../hooks/usePageTitle';

export function NotFound() {
  usePageTitle('Not Found');
  return (
    <Card className="p-8">
      <h2 className="font-display text-3xl text-white">Page Not Found</h2>
      <p className="mt-3 text-sm text-slate-300">The route you tried to open does not exist in Atlas Pulse.</p>
    </Card>
  );
}

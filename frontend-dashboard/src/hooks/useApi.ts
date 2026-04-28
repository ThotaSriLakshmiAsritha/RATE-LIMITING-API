import { useState } from 'react';
import { useAppContext } from '../context/AppContext';

export function useApi() {
  const [loading, setLoading] = useState(false);
  const { executeTrackedRequest } = useAppContext();

  const run = async <T,>(config: Parameters<typeof executeTrackedRequest<T>>[0]) => {
    setLoading(true);
    try {
      return await executeTrackedRequest<T>(config);
    } finally {
      setLoading(false);
    }
  };

  return { run, loading };
}

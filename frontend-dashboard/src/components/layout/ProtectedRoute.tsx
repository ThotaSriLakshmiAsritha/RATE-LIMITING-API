import { Navigate, Outlet } from 'react-router-dom';
import { useAppContext } from '../../context/AppContext';
import { isTokenExpired } from '../../utils/jwt';

export function ProtectedRoute() {
  const { state, logout } = useAppContext();

  if (!state.authToken) {
    return <Navigate to="/login" replace />;
  }

  if (isTokenExpired(state.authToken)) {
    logout(true);
    return null;
  }

  return <Outlet />;
}

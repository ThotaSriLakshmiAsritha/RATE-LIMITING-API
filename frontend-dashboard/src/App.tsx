import React from 'react';
import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import { MainLayout } from './components/layout/MainLayout';
import { Overview } from './pages/Overview';
import { Policies } from './pages/Policies';
import { LiveTraffic } from './pages/LiveTraffic';
import { APIProducts } from './pages/APIProducts';
import { Logs } from './pages/Logs';
import { Login } from './pages/Login';

// Placeholder components for missing pages
const Analytics = () => <Overview />;

function App() {
  return (
    <Router>
      <Routes>
        <Route path="/login" element={<Login />} />
        <Route
          path="*"
          element={
            <MainLayout>
              <Routes>
                <Route path="/" element={<Overview />} />
                <Route path="/analytics" element={<Analytics />} />
                <Route path="/policies" element={<Policies />} />
                <Route path="/live" element={<LiveTraffic />} />
                <Route path="/products" element={<APIProducts />} />
                <Route path="/logs" element={<Logs />} />
                <Route path="*" element={<Navigate to="/" replace />} />
              </Routes>
            </MainLayout>
          }
        />
      </Routes>
    </Router>
  );
}

export default App;

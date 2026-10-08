import { BrowserRouter, Route, Routes, Navigate } from "react-router-dom";
import { AdminQueryProvider, getToken } from "@/lib/adminApi";
import AdminLayout from "./layouts/AdminLayout";
import AdminLoginPage from "./pages/AdminLoginPage";
import AdminDashboardPage from "./pages/AdminDashboardPage";
import AdminUsersPage from "./pages/AdminUsersPage";
import AdminVideosPage from "./pages/AdminVideosPage";
import AdminChannelsPage from "./pages/AdminChannelsPage";

function RequireAuth({ children }: { children: React.ReactNode }) {
  return getToken() ? <>{children}</> : <Navigate to="/login" replace />;
}

const App = () => (
  <AdminQueryProvider>
    <BrowserRouter>
      <Routes>
        <Route path="/login" element={<AdminLoginPage />} />
        <Route
          path="/"
          element={
            <RequireAuth>
              <AdminLayout />
            </RequireAuth>
          }
        >
          <Route index element={<AdminDashboardPage />} />
          <Route path="users" element={<AdminUsersPage />} />
          <Route path="videos" element={<AdminVideosPage />} />
          <Route path="channels" element={<AdminChannelsPage />} />
        </Route>
        <Route path="*" element={<Navigate to="/" replace />} />
      </Routes>
    </BrowserRouter>
  </AdminQueryProvider>
);

export default App;

import { BrowserRouter, Routes, Route, Navigate } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import ProtectedRoute from "./components/ProtectedRoute";
import Layout from "./components/Layout";

// Pages
import Login from "./pages/Login";
import AdminDashboard from "./pages/admin/Dashboard";
import ProfessorDashboard from "./pages/professor/Dashboard";

import AdminProfessors from "./pages/admin/Professors";
import AdminStudents from "./pages/admin/Students";
import AdminCourses from "./pages/admin/Courses";
import AdminAuditLogs from "./pages/admin/AuditLogs";
import AdminRegister from "./pages/admin/Register";
import AdminSecurity from "./pages/admin/Security";
import CourseDetail from "./pages/professor/CourseDetail";

function App() {
  return (
    <BrowserRouter>
      <AuthProvider>
        <Routes>
          <Route path="/login" element={<Login />} />
          <Route path="/" element={<Navigate to="/login" />} />

          {/* Admin Routes */}
          <Route 
            path="/admin" 
            element={
              <ProtectedRoute allowedRoles={["ADMIN"]}>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<AdminDashboard />} />
            <Route path="security" element={<AdminSecurity />} />
            <Route path="professors" element={<AdminProfessors />} />
            <Route path="students" element={<AdminStudents />} />
            <Route path="courses" element={<AdminCourses />} />
            <Route path="audit-logs" element={<AdminAuditLogs />} />
            <Route path="register" element={<AdminRegister />} />
          </Route>

          {/* Professor Routes */}
          <Route 
            path="/professor" 
            element={
              <ProtectedRoute allowedRoles={["PROFESSOR", "ADMIN"]}>
                <Layout />
              </ProtectedRoute>
            }
          >
            <Route index element={<ProfessorDashboard />} />
            <Route path="courses" element={<ProfessorDashboard />} />
            <Route path="courses/:id" element={<CourseDetail />} />
          </Route>

          <Route path="*" element={<Navigate to="/login" />} />
        </Routes>
      </AuthProvider>
    </BrowserRouter>
  );
}

export default App;

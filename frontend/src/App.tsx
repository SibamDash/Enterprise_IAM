import { Routes, Route } from 'react-router-dom';
import DashboardLayout from './layouts/DashboardLayout';
import Dashboard from './pages/Dashboard';
import OrganizationList from './pages/organizations/OrganizationList';
import CreateOrganization from './pages/organizations/CreateOrganization';
import UserList from './pages/users/UserList';
import AuthLayout from './layouts/AuthLayout';
import Login from './pages/auth/Login';
import ForgotPassword from './pages/auth/ForgotPassword';
import ResetPassword from './pages/auth/ResetPassword';
import './App.css';

function App() {
  return (
    <Routes>
      <Route element={<AuthLayout />}>
        <Route path="/login" element={<Login />} />
        <Route path="/forgot-password" element={<ForgotPassword />} />
        <Route path="/reset-password" element={<ResetPassword />} />
      </Route>
      
      <Route path="/" element={<DashboardLayout />}>
        <Route index element={<Dashboard />} />
        <Route path="organizations" element={<OrganizationList />} />
        <Route path="organizations/new" element={<CreateOrganization />} />
        <Route path="users" element={<UserList />} />
        <Route path="users/new" element={<CreateUser />} />
      </Route>
    </Routes>
  );
}

export default App;

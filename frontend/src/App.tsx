import { Routes, Route } from 'react-router-dom';
import DashboardLayout from './layouts/DashboardLayout';
import Dashboard from './pages/Dashboard';
import OrganizationList from './pages/organizations/OrganizationList';
import CreateOrganization from './pages/organizations/CreateOrganization';
import UserList from './pages/users/UserList';
import CreateUser from './pages/users/CreateUser';
import './App.css';

function App() {
  return (
    <Routes>
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

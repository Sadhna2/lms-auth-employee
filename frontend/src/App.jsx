// Scoped adaptation of the original routes; excluded modules have no imports or routes.
import { Navigate,Route,Routes } from 'react-router-dom';
import { useAuth } from './auth.jsx';
import Layout from './components/Layout.jsx';
import Login from './pages/Login.jsx';
import Employees from './pages/Employees.jsx';
import Team from './pages/Team.jsx';
import Profile from './pages/Profile.jsx';
function Protected({roles,children}) {
  const {user}=useAuth();if(!user)return <Navigate to="/login" replace/>;
  if(roles && !roles.includes(user.role))return <Navigate to="/" replace/>;
  return <Layout>{children}</Layout>;
}
export default function App() {return <Routes>
  <Route path="/login" element={<Login/>}/>
  <Route path="/" element={<Protected><Profile/></Protected>}/>
  <Route path="/employees" element={<Protected roles={['HR','ADMIN']}><Employees/></Protected>}/>
  <Route path="/team" element={<Protected roles={['MANAGER','HR','ADMIN']}><Team/></Protected>}/>
  <Route path="*" element={<Navigate to="/" replace/>}/>
</Routes>;}

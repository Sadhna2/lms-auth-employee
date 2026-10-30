// Necessary new self-profile screen; no leave-dashboard dependencies are carried over.
import { useAuth } from '../auth.jsx';
import { Alert,Page } from '../components/ui.jsx';
export default function Profile() {
  const {user}=useAuth();const profile=user.profile;
  return <Page title="My profile" subtitle="Your account and employee details" back={false}>
    <Alert type="success">{user.message || "Login successful."}</Alert>
    <div className="card p-4"><h2 className="h5">{user.name || user.username}</h2><p>Role: {user.role}</p>
    {profile?<dl className="row mb-0">{['empId','empName','email','phoneNumber','designation','deptId','managerId','gender','address'].map(key=><div key={key} className="col-md-6 mb-3"><dt>{({empId:'Employee ID',empName:'Name',phoneNumber:'Phone number',deptId:'Department ID',managerId:'Manager ID'})[key] || key.charAt(0).toUpperCase()+key.slice(1)}</dt><dd>{profile[key] ?? '-'}</dd></div>)}</dl>
    :<p className="text-secondary mb-0">This is the initial administrator account. Use Employees to create employee profiles and login users.</p>}</div>
  </Page>;
}

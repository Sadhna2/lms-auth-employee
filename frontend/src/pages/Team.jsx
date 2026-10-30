// Original team table adapted to the scoped /api/employees/{id}/team endpoint and HR manager selector.
import { useEffect,useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert,Empty,Page } from '../components/ui.jsx';
export default function Team() {
  const {user}=useAuth();const [team,setTeam]=useState([]);const [managers,setManagers]=useState([]);
  const [managerId,setManagerId]=useState(user.role==='MANAGER'?String(user.empId):'');const [error,setError]=useState('');
  useEffect(()=>{api.managers().then(setManagers).catch(e=>setError(e.message));},[]);
  useEffect(()=>{setError('');setTeam([]);if(managerId)api.team(managerId).then(setTeam).catch(e=>setError(e.message));},[managerId]);
  return <Page title="Manager teams" subtitle="View a manager's direct reports"><Alert>{error}</Alert>
    {user.role!=='MANAGER' && <div className="card p-3 mb-3"><label htmlFor="managerTeam" className="form-label">Manager</label><select id="managerTeam" className="form-select" value={managerId} onChange={e=>setManagerId(e.target.value)}><option value="">Select a manager</option>{managers.map(m=><option key={m.empId} value={m.empId}>{m.empName} ({m.email})</option>)}</select></div>}
    {!team.length?<Empty>{managerId?'No employees report to this manager yet.':'Choose a manager to view their team.'}</Empty>:<div className="table-responsive"><table className="table table-hover bg-white"><thead className="table-light"><tr><th>ID</th><th>Name</th><th>Designation</th><th>Email</th><th>Phone</th></tr></thead><tbody>{team.map(e=><tr key={e.empId}><td>{e.empId}</td><td>{e.empName}</td><td>{e.designation}</td><td>{e.email}</td><td>{e.phoneNumber}</td></tr>)}</tbody></table></div>}
  </Page>;
}

// Copied employee directory, extended only with required profile CRUD and protected login provisioning.
import { useCallback,useEffect,useState } from 'react';
import { api } from '../api.js';
import { useAuth } from '../auth.jsx';
import { Alert,Empty,Page } from '../components/ui.jsx';
const blank={empName:'',email:'',phoneNumber:'',designation:'',deptId:'',managerId:'',gender:'',address:''};
export default function Employees() {
  const {user}=useAuth();const [list,setList]=useState([]);const [managers,setManagers]=useState([]);
  const [error,setError]=useState('');const [message,setMessage]=useState('');const [busy,setBusy]=useState(false);
  const [form,setForm]=useState(null);const [editId,setEditId]=useState(null);const [account,setAccount]=useState(null);
  const [password,setPassword]=useState('');const [role,setRole]=useState('EMPLOYEE');
  const load=useCallback(()=>Promise.all([api.employees(),api.managers()]).then(([employees,leaders])=>{setList(employees);setManagers(leaders);}),[]);
  useEffect(()=>{load().catch(e=>setError(e.message));},[load]);
  function execute(operation) {setError('');setMessage('');setBusy(true);return Promise.resolve().then(operation).catch(e=>setError(e.message)).finally(()=>setBusy(false));}
  function edit(employee=null) {setAccount(null);setEditId(employee?.empId || null);setForm(employee?Object.fromEntries(Object.keys(blank).map(k=>[k,employee[k] ?? ''])):{...blank});setError('');setMessage('');}
  function save(event) {event.preventDefault();execute(async()=>{
    const input={...form,deptId:form.deptId?Number(form.deptId):null,managerId:form.managerId?Number(form.managerId):null};
    if(editId)await api.updateEmployee(editId,input);else await api.createEmployee(input);
    setForm(null);await load();setMessage(editId?'Employee updated.':'Employee created. Choose Create login to grant account access.');
  });}
  function remove(employee) {
    if(!window.confirm('Delete '+employee.empName+' and the linked login account?'))return;
    execute(async()=>{await api.deleteEmployee(employee.empId);await load();setMessage('Employee deleted.');});
  }
  function createLogin(event) {event.preventDefault();execute(async()=>{await api.createLogin(account.email,password,role);setPassword('');setAccount(null);await load();setMessage('Registration successful. Login account created.');});}
  const field=(key,label,type='text',extra={})=><div className="col-md-6" key={key}><label htmlFor={'employee-'+key} className="form-label">{label}</label><input id={'employee-'+key} className="form-control" type={type} value={form[key]} onChange={e=>setForm({...form,[key]:e.target.value})} {...extra}/></div>;
  return <Page title="Employees" subtitle="Maintain profiles, login users and manager relationships">
    <Alert>{error}</Alert><Alert type="success">{message}</Alert>
    <button className="btn btn-primary mb-3" onClick={()=>edit()} disabled={busy}>Add employee</button>
    {form && <form className="card p-4 mb-4" onSubmit={save}><h2 className="h5">{editId?'Edit employee':'Add employee'}</h2><div className="row g-3">
      {field('empName','Full name','text',{required:true,maxLength:100})}{field('email','Email','email',{required:true,maxLength:100})}
      {field('phoneNumber','Phone number','tel',{required:true,pattern:'[0-9]{10}',maxLength:10})}{field('designation','Designation','text',{required:true,maxLength:50})}
      {field('deptId','Department ID','number',{min:1})}<div className="col-md-6"><label htmlFor="employee-manager" className="form-label">Manager</label><select id="employee-manager" className="form-select" value={form.managerId} onChange={e=>setForm({...form,managerId:e.target.value})}><option value="">No manager</option>{managers.filter(m=>m.empId!==editId).map(m=><option key={m.empId} value={m.empId}>{m.empName}</option>)}</select></div>
      {field('gender','Gender','text',{required:true,maxLength:10})}{field('address','Address','text',{required:true,maxLength:200})}
    </div><p className="small text-secondary mt-3">A manager must already have a MANAGER login. Existing account email addresses cannot be changed.</p><div><button className="btn btn-primary me-2" disabled={busy}>Save employee</button><button type="button" className="btn btn-outline-secondary" onClick={()=>setForm(null)}>Cancel</button></div></form>}
    {account && <form className="card p-4 mb-4" onSubmit={createLogin}><h2 className="h5">Create login for {account.empName}</h2><p>{account.email}</p><div className="row g-3"><div className="col-md-6"><label htmlFor="accountPassword" className="form-label">Initial password</label><input id="accountPassword" autoComplete="new-password" type="password" className="form-control" value={password} onChange={e=>setPassword(e.target.value)} minLength={8} maxLength={72} required/></div><div className="col-md-6"><label htmlFor="accountRole" className="form-label">Role</label><select id="accountRole" className="form-select" value={role} onChange={e=>setRole(e.target.value)}>{(user.role==='ADMIN'?['EMPLOYEE','MANAGER','HR','ADMIN']:['EMPLOYEE','MANAGER']).map(r=><option key={r}>{r}</option>)}</select></div></div><p className="small text-secondary mt-3">Share the initial password privately. Duplicate logins cannot overwrite credentials.</p><div><button className="btn btn-primary me-2" disabled={busy}>Create account</button><button type="button" className="btn btn-outline-secondary" onClick={()=>{setAccount(null);setPassword('');}}>Cancel</button></div></form>}
    {!list.length?<Empty>No employees yet.</Empty>:<div className="table-responsive"><table className="table table-hover bg-white align-middle"><thead className="table-light"><tr><th>ID</th><th>Name</th><th>Designation</th><th>Dept</th><th>Manager</th><th>Email</th><th>Actions</th></tr></thead><tbody>{list.map(e=><tr key={e.empId}><td>{e.empId}</td><td>{e.empName}</td><td>{e.designation}</td><td>{e.deptId ?? '-'}</td><td>{e.managerId ?? '-'}</td><td>{e.email}</td><td><div className="d-flex gap-2 flex-wrap"><button className="btn btn-outline-success btn-sm" onClick={()=>edit(e)} disabled={busy}>Edit</button><button className="btn btn-outline-primary btn-sm" onClick={()=>{setForm(null);setAccount(e);setPassword('');setRole('EMPLOYEE');setError('');setMessage('');}} disabled={busy}>Create login</button><button className="btn btn-outline-danger btn-sm" onClick={()=>remove(e)} disabled={busy}>Delete</button></div></td></tr>)}</tbody></table></div>}
  </Page>;
}

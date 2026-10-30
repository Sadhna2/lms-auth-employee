// Scoped same-origin API client. Credentials stay in memory; all server errors share one decoder.
let authorization='';
let csrfToken='';
export function clearCredentials() { authorization='';csrfToken=''; }
async function request(path,options={}) {
  const headers={...options.headers};
  if(authorization && options.auth!==false) headers.Authorization=authorization;
  if(options.body) headers['Content-Type']='application/json';
  if(options.method && !['GET','HEAD','OPTIONS'].includes(options.method)) headers['X-XSRF-TOKEN']=csrfToken;
  const response=await fetch(path,{method:options.method || 'GET',body:options.body,headers,credentials:'same-origin'})
    .catch(()=>{throw new Error('Cannot reach the backend. Check the API deployment or local proxy.');});
  const text=await response.text();const data=text?JSON.parse(text):null;
  if(!response.ok) {
    const details=Object.entries(data?.fieldErrors || {}).map(([key,value])=>key+': '+value).join('; ');
    const error=new Error((data?.message || 'Request failed ('+response.status+')')+(details?' - '+details:''));
    error.status=response.status;throw error;
  }
  return data;
}
async function csrf() { csrfToken=(await request('/api/csrf')).token; }
const write=(path,method,body)=>csrf().then(()=>request(path,{method,body:body?JSON.stringify(body):undefined}));
export const api={
  login:async(username,password)=>{
    clearCredentials();await csrf();
    const user=await request('/api/login',{method:'POST',auth:false,body:JSON.stringify({username,password})});
    authorization='Basic '+btoa(String.fromCharCode(...new TextEncoder().encode(username+':'+password)));
    return user;
  },
  me:()=>request('/api/employees/me'),
  employees:()=>request('/api/employees'),employee:id=>request('/api/employees/'+id),
  managers:()=>request('/api/employees/managers'),team:id=>request('/api/employees/'+id+'/team'),
  createEmployee:body=>write('/api/employees','POST',body),
  updateEmployee:(id,body)=>write('/api/employees/'+id,'PUT',body),
  deleteEmployee:id=>write('/api/employees/'+id,'DELETE'),
  createLogin:(username,password,role)=>write('/api/login/user','POST',{username,password,role})
};

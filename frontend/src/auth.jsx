// Adapted from the original AuthProvider: tab-memory identity replaces persistent localStorage.
import { createContext,useContext,useState } from 'react';
import { clearCredentials } from './api.js';
const AuthContext=createContext(null);
export const ROLE_LABEL={EMPLOYEE:'Employee',MANAGER:'Reporting Manager',HR:'HR',ADMIN:'Admin'};
export function AuthProvider({children}) {
  const [user,setUser]=useState(null);
  const login=identity=>setUser(identity);
  const logout=()=>{clearCredentials();setUser(null);};
  return <AuthContext.Provider value={{user,login,logout}}>{children}</AuthContext.Provider>;
}
export const useAuth=()=>useContext(AuthContext);

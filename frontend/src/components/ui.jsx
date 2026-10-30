// Required shared presentation components copied from the original LMS; leave-status helpers removed.
import { Link } from 'react-router-dom';
export function Page({title,subtitle,children,back=true}) {
  return <div><div className="d-flex justify-content-between align-items-start flex-wrap gap-2 mb-1">
    <h1 className="h3 m-0" style={{color:'#064e3b'}}>{title}</h1>
    {back && <Link to="/" className="btn btn-outline-success btn-sm">Back to profile</Link>}
  </div>{subtitle && <p className="text-secondary">{subtitle}</p>}{children}</div>;
}
export function Alert({type='danger',children}) {return children?<div role="alert" className={'alert alert-'+type}>{children}</div>:null;}
export function Empty({children}) {return <div className="empty-box text-center text-secondary p-4">{children}</div>;}

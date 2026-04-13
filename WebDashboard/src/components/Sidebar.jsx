import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { 
  BarChart2, 
  Users, 
  BookOpen, 
  LogOut, 
  ShieldAlert, 
  ClipboardList,
  GraduationCap,
  Shield,
  Menu,
  X,
} from "lucide-react";
import clsx from "clsx";
import { useState } from "react";

export default function Sidebar() {
  const { userRole, logout, currentUser } = useAuth();
  const location = useLocation();
  const [mobileOpen, setMobileOpen] = useState(false);

  const adminLinks = [
    { label: "Dashboard", path: "/admin", icon: BarChart2 },
    { label: "Security", path: "/admin/security", icon: Shield },
    { label: "Professors", path: "/admin/professors", icon: GraduationCap },
    { label: "Students", path: "/admin/students", icon: Users },
    { label: "Courses", path: "/admin/courses", icon: BookOpen },
    { label: "Audit Logs", path: "/admin/audit-logs", icon: ShieldAlert },
    { label: "Register Users", path: "/admin/register", icon: ClipboardList },
  ];

  const profLinks = [
    { label: "Dashboard", path: "/professor", icon: BarChart2 },
    { label: "My Courses", path: "/professor/courses", icon: BookOpen },
  ];

  const links = userRole === "ADMIN" ? adminLinks : profLinks;

  const isActive = (path) => {
    if (path === "/admin" || path === "/professor") {
      return location.pathname === path;
    }
    return location.pathname.startsWith(path);
  };

  const sidebarContent = (
    <>
      <div className="p-6 border-b border-zinc-800/70">
        <div className="flex items-center gap-3">
          <div className="w-10 h-10 rounded-xl bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center shadow-lg shadow-blue-500/20">
            <ShieldAlert className="text-white" size={20} />
          </div>
          <div>
            <h1 className="text-lg font-bold gradient-text">Smart Attendance</h1>
            <p className="text-[10px] text-zinc-500 uppercase tracking-[0.2em] font-medium">{userRole} Portal</p>
          </div>
        </div>
      </div>

      <nav className="flex-1 overflow-y-auto p-3 space-y-1 custom-scrollbar">
        <p className="px-3 py-2 text-[10px] text-zinc-600 uppercase tracking-[0.15em] font-semibold">Navigation</p>
        {links.map((link, index) => {
          const Icon = link.icon;
          const active = isActive(link.path);
          
          return (
            <Link
              key={link.path}
              to={link.path}
              onClick={() => setMobileOpen(false)}
              className={clsx(
                "flex items-center gap-3 px-3 py-2.5 rounded-xl transition-all duration-200 text-sm font-medium group",
                "animate-slide-in-left",
                `stagger-${index + 1}`,
                active 
                  ? "bg-blue-500/10 text-blue-400 border border-blue-500/20 shadow-sm shadow-blue-500/5" 
                  : "text-zinc-400 hover:text-zinc-100 hover:bg-zinc-800/60 border border-transparent"
              )}
            >
              <Icon size={18} className={clsx(
                "transition-transform duration-200",
                active ? "scale-110" : "group-hover:scale-105"
              )} />
              {link.label}
              {active && (
                <div className="ml-auto w-1.5 h-1.5 rounded-full bg-blue-400 animate-pulse" />
              )}
            </Link>
          );
        })}
      </nav>

      <div className="p-4 border-t border-zinc-800/70">
        <div className="flex items-center gap-3 mb-3 px-2">
          <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500 to-indigo-600 flex items-center justify-center text-xs font-bold text-white shrink-0 shadow-md shadow-blue-500/20">
            {currentUser?.displayName?.charAt(0)?.toUpperCase() || currentUser?.email?.charAt(0)?.toUpperCase() || "U"}
          </div>
          <div className="flex-1 min-w-0">
            <p className="text-sm font-medium truncate text-zinc-100">{currentUser?.displayName || "User"}</p>
            <p className="text-[11px] text-zinc-500 truncate">{currentUser?.email}</p>
          </div>
        </div>
        <button
          onClick={logout}
          className="w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-sm font-medium text-red-400 hover:bg-red-500/10 hover:text-red-300 transition-all duration-200 border border-transparent hover:border-red-500/20"
        >
          <LogOut size={18} />
          Sign Out
        </button>
      </div>
    </>
  );

  return (
    <>
      {/* Mobile Toggle Button */}
      <button
        onClick={() => setMobileOpen(!mobileOpen)}
        className="md:hidden fixed top-4 left-4 z-50 p-2 rounded-xl bg-zinc-900 border border-zinc-800 text-zinc-400 hover:text-white transition-colors"
      >
        {mobileOpen ? <X size={20} /> : <Menu size={20} />}
      </button>

      {/* Mobile Overlay */}
      {mobileOpen && (
        <div 
          className="md:hidden fixed inset-0 bg-black/60 backdrop-blur-sm z-30 animate-fade-in"
          onClick={() => setMobileOpen(false)}
        />
      )}

      {/* Mobile Sidebar */}
      <div className={clsx(
        "md:hidden fixed inset-y-0 left-0 w-72 bg-zinc-950 border-r border-zinc-800 flex flex-col z-40 transition-transform duration-300",
        mobileOpen ? "translate-x-0" : "-translate-x-full"
      )}>
        {sidebarContent}
      </div>

      {/* Desktop Sidebar */}
      <div className="w-64 border-r border-zinc-800/70 bg-zinc-950/80 flex-col glass-panel z-20 hidden md:flex">
        {sidebarContent}
      </div>
    </>
  );
}

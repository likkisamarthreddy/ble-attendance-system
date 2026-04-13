import { Outlet, useLocation } from "react-router-dom";
import Sidebar from "./Sidebar";
import { useAuth } from "../context/AuthContext";
import { ChevronRight, Bell } from "lucide-react";

export default function Layout() {
  const location = useLocation();
  const { currentUser } = useAuth();

  // Generate breadcrumbs from the path
  const pathSegments = location.pathname.split("/").filter(Boolean);
  const breadcrumbs = pathSegments.map((segment, index) => ({
    label: segment.charAt(0).toUpperCase() + segment.slice(1).replace(/-/g, " "),
    isLast: index === pathSegments.length - 1,
  }));

  const getGreeting = () => {
    const hour = new Date().getHours();
    if (hour < 12) return "Good morning";
    if (hour < 17) return "Good afternoon";
    return "Good evening";
  };

  const isHome = location.pathname === "/admin" || location.pathname === "/professor";

  return (
    <div className="flex h-screen bg-[#09090b] text-zinc-100 overflow-hidden">
      <Sidebar />
      <div className="flex-1 flex flex-col overflow-hidden">
        {/* Top Header Bar */}
        <header className="h-16 border-b border-zinc-800/50 bg-zinc-950/50 backdrop-blur-md flex items-center justify-between px-4 md:px-8 shrink-0 z-10">
          <div className="flex items-center gap-2 text-sm">
            {/* Add left margin on mobile for the hamburger button */}
            <div className="ml-10 md:ml-0 flex items-center gap-2">
              {breadcrumbs.map((crumb, i) => (
                <span key={i} className="flex items-center gap-2">
                  {i > 0 && <ChevronRight size={14} className="text-zinc-600" />}
                  <span className={crumb.isLast ? "text-zinc-200 font-medium" : "text-zinc-500"}>
                    {crumb.label}
                  </span>
                </span>
              ))}
            </div>
          </div>
          
          <div className="flex items-center gap-4">
            {isHome && (
              <span className="text-sm text-zinc-500 hidden lg:block">
                {getGreeting()}, <span className="text-zinc-300 font-medium">{currentUser?.displayName || currentUser?.email?.split("@")[0]}</span>
              </span>
            )}
            <button className="relative p-2 rounded-lg text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800 transition-colors">
              <Bell size={18} />
              <span className="absolute top-1 right-1 w-2 h-2 bg-blue-500 rounded-full"></span>
            </button>
          </div>
        </header>

        {/* Main Content */}
        <main className="flex-1 overflow-y-auto p-4 md:p-8 bg-[#09090b] relative z-0 w-full custom-scrollbar">
          <Outlet />
        </main>
      </div>
    </div>
  );
}

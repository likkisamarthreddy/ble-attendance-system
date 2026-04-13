import { useEffect, useState } from "react";
import api from "../../lib/api";
import { Users, BookOpen, ShieldCheck, AlertTriangle, GraduationCap, Activity, ArrowRight, Shield } from "lucide-react";
import { Link } from "react-router-dom";
import StatsCard from "../../components/StatsCard";
import { 
  LineChart, Line, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, 
  PieChart, Pie, Cell, Legend 
} from "recharts";
import { format } from "date-fns";

const CHART_COLORS = ["#ef4444", "#f59e0b", "#6366f1", "#3b82f6", "#8b5cf6"];

// Custom tooltip for charts
function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-zinc-900 border border-zinc-700 rounded-xl px-4 py-3 shadow-xl">
      <p className="text-xs text-zinc-400 mb-1">{label}</p>
      {payload.map((p, i) => (
        <p key={i} className="text-sm font-semibold" style={{ color: p.color }}>
          {p.name}: {p.value}
        </p>
      ))}
    </div>
  );
}

export default function AdminDashboard() {
  const [stats, setStats] = useState(null);
  const [security, setSecurity] = useState(null);
  const [recentLogs, setRecentLogs] = useState([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchAll = async () => {
      try {
        const [statsRes, securityRes, logsRes] = await Promise.all([
          api.get("/admin/dashboard/stats"),
          api.get("/admin/dashboard/security"),
          api.get("/admin/dashboard/audit-logs", { params: { limit: 8 } }),
        ]);
        setStats(statsRes.data);
        setSecurity(securityRes.data);
        setRecentLogs(logsRes.data.logs || []);
      } catch (err) {
        console.error("Failed to load dashboard", err);
      } finally {
        setLoading(false);
      }
    };
    fetchAll();
  }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {[...Array(4)].map((_, i) => (
            <div key={i} className="glass-card p-6">
              <div className="skeleton h-4 w-24 mb-3"></div>
              <div className="skeleton h-8 w-16 mb-2"></div>
              <div className="skeleton h-3 w-20"></div>
            </div>
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          <div className="lg:col-span-2 glass-card p-6"><div className="skeleton h-72 w-full"></div></div>
          <div className="glass-card p-6"><div className="skeleton h-72 w-full"></div></div>
        </div>
      </div>
    );
  }

  // Attendance trend mock data (weekly pattern)
  const trendData = [
    { day: "Mon", attendance: 82, sessions: 5 },
    { day: "Tue", attendance: 88, sessions: 6 },
    { day: "Wed", attendance: 75, sessions: 4 },
    { day: "Thu", attendance: 91, sessions: 7 },
    { day: "Fri", attendance: 85, sessions: 5 },
    { day: "Sat", attendance: 70, sessions: 2 },
  ];

  // Security pie data
  const pieData = [
    { name: "Face Mismatch", value: security?.faceMismatchCount || 0 },
    { name: "Replay Attacks", value: security?.replayAttempts || 0 },
    { name: "Geofence Fail", value: security?.geofenceFailures || 0 },
    { name: "Token Invalid", value: security?.tokenInvalid || 0 },
    { name: "Timing Reject", value: security?.timingRejected || 0 },
  ].filter(d => d.value > 0);

  const totalThreats = pieData.reduce((sum, d) => sum + d.value, 0);

  const getStatusIcon = (status) => {
    switch(status) {
      case "SUCCESS": return "✓";
      case "FAILURE": return "✕";
      case "WARNING": return "⚠";
      default: return "•";
    }
  };

  const getStatusColor = (status) => {
    switch(status) {
      case "SUCCESS": return "text-emerald-400 bg-emerald-500/10";
      case "FAILURE": return "text-red-400 bg-red-500/10";
      case "WARNING": return "text-amber-400 bg-amber-500/10";
      default: return "text-zinc-400 bg-zinc-800";
    }
  };

  return (
    <div className="space-y-6">
      {/* Stats Row */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="animate-slide-up stagger-1">
          <StatsCard 
            title="Total Students" 
            value={stats?.totalStudents || 0} 
            icon={Users} 
            color="blue" 
          />
        </div>
        <div className="animate-slide-up stagger-2">
          <StatsCard 
            title="Professors" 
            value={stats?.totalProfessors || 0} 
            icon={GraduationCap} 
            color="indigo" 
          />
        </div>
        <div className="animate-slide-up stagger-3">
          <StatsCard 
            title="Active Courses" 
            value={stats?.totalCourses || 0} 
            icon={BookOpen} 
            color="purple" 
          />
        </div>
        <div className="animate-slide-up stagger-4">
          <StatsCard 
            title="Attendance Rate" 
            value={`${stats?.overallAttendancePercentage || 0}%`} 
            icon={ShieldCheck} 
            color="emerald" 
          />
        </div>
        <div className="animate-slide-up stagger-5">
          <StatsCard 
            title="Critical Students" 
            value={stats?.criticalCount || 0} 
            subtitle="Below 60% attendance"
            icon={AlertTriangle} 
            color="red" 
          />
        </div>
      </div>

      {/* Charts Row */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in" style={{ animationDelay: "0.3s" }}>
        {/* Attendance Trend Line Chart */}
        <div className="lg:col-span-2 glass-card p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="font-semibold text-zinc-100 text-lg">Attendance Trend</h3>
              <p className="text-xs text-zinc-500 mt-0.5">Weekly attendance rate across all courses</p>
            </div>
            <span className="px-3 py-1 rounded-lg bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">
              This Week
            </span>
          </div>
          <ResponsiveContainer width="100%" height={280}>
            <LineChart data={trendData}>
              <CartesianGrid strokeDasharray="3 3" stroke="#27272a" />
              <XAxis dataKey="day" stroke="#71717a" fontSize={12} tickLine={false} axisLine={false} />
              <YAxis stroke="#71717a" fontSize={12} tickLine={false} axisLine={false} tickFormatter={(v) => `${v}%`} domain={[0, 100]} />
              <Tooltip content={<CustomTooltip />} />
              <Line 
                type="monotone" 
                dataKey="attendance" 
                name="Attendance %"
                stroke="#3b82f6" 
                strokeWidth={3}
                dot={{ fill: "#3b82f6", r: 5, strokeWidth: 2, stroke: "#09090b" }}
                activeDot={{ r: 7, fill: "#60a5fa", stroke: "#3b82f6", strokeWidth: 2 }}
              />
            </LineChart>
          </ResponsiveContainer>
        </div>

        {/* Security Threats Pie Chart */}
        <div className="glass-card p-6">
          <div className="flex items-center justify-between mb-6">
            <div>
              <h3 className="font-semibold text-zinc-100 text-lg">Security Overview</h3>
              <p className="text-xs text-zinc-500 mt-0.5">Threat distribution</p>
            </div>
            <Link to="/admin/security" className="text-xs text-blue-400 hover:text-blue-300 transition-colors">
              Details →
            </Link>
          </div>
          
          {totalThreats === 0 ? (
            <div className="h-64 flex flex-col items-center justify-center text-center">
              <Shield className="text-emerald-500 mb-3" size={40} />
              <p className="text-emerald-400 font-medium text-sm">All Clear</p>
              <p className="text-zinc-500 text-xs mt-1">No security threats detected</p>
            </div>
          ) : (
            <ResponsiveContainer width="100%" height={280}>
              <PieChart>
                <Pie 
                  data={pieData} 
                  cx="50%" 
                  cy="50%" 
                  innerRadius={55}
                  outerRadius={85}
                  paddingAngle={4}
                  dataKey="value"
                  stroke="none"
                >
                  {pieData.map((_, index) => (
                    <Cell key={index} fill={CHART_COLORS[index % CHART_COLORS.length]} />
                  ))}
                </Pie>
                <Legend 
                  verticalAlign="bottom" 
                  iconType="circle" 
                  iconSize={8}
                  formatter={(value) => <span className="text-xs text-zinc-400 ml-1">{value}</span>}
                />
                <Tooltip content={<CustomTooltip />} />
              </PieChart>
            </ResponsiveContainer>
          )}
        </div>
      </div>

      {/* Bottom Row: Activity Feed + Extra Data */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6 animate-fade-in" style={{ animationDelay: "0.5s" }}>
        {/* Recent Activity */}
        <div className="lg:col-span-2 glass-card p-6">
          <div className="flex items-center justify-between mb-5">
            <div className="flex items-center gap-2">
              <Activity className="text-blue-400" size={18} />
              <h3 className="font-semibold text-zinc-100">Recent Activity</h3>
            </div>
            <Link to="/admin/audit-logs" className="text-xs text-blue-400 hover:text-blue-300 transition-colors flex items-center gap-1">
              View All <ArrowRight size={12} />
            </Link>
          </div>
          
          <div className="space-y-2">
            {recentLogs.length === 0 ? (
              <p className="text-zinc-500 text-sm text-center py-8">No recent activity</p>
            ) : (
              recentLogs.map((log, idx) => (
                <div key={log.id || idx} className="flex items-center gap-3 p-3 rounded-xl hover:bg-zinc-800/40 transition-colors group">
                  <div className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold shrink-0 ${getStatusColor(log.status)}`}>
                    {getStatusIcon(log.status)}
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-sm text-zinc-200 font-medium truncate">
                      {log.action?.replace(/_/g, " ")}
                    </p>
                    <p className="text-[11px] text-zinc-500 truncate">
                      {log.role} • {log.ipAddress || "N/A"}
                    </p>
                  </div>
                  <span className="text-[11px] text-zinc-600 whitespace-nowrap shrink-0">
                    {log.timestamp ? format(new Date(log.timestamp), 'MMM d, HH:mm') : ""}
                  </span>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Quick Actions & Demographics Split */}
        <div className="flex flex-col gap-6">
          {/* Quick Actions */}
          <div className="glass-card p-6">
            <h3 className="font-semibold text-zinc-100 mb-5">Quick Actions</h3>
            <div className="space-y-3">
              {[
                { label: "Register Users", path: "/admin/register", icon: Users, color: "text-blue-400", bg: "bg-blue-500/10 border-blue-500/20" },
                { label: "View Audit Logs", path: "/admin/audit-logs", icon: ShieldCheck, color: "text-emerald-400", bg: "bg-emerald-500/10 border-emerald-500/20" },
                { label: "Security Monitor", path: "/admin/security", icon: Shield, color: "text-amber-400", bg: "bg-amber-500/10 border-amber-500/20" },
              ].map(action => (
                <Link key={action.path} to={action.path} className="flex items-center gap-3 w-full text-left px-4 py-2.5 rounded-xl bg-zinc-800/30 hover:bg-zinc-800/70 border border-zinc-700/50 hover:border-zinc-600 transition-all text-sm text-zinc-300 group">
                  <div className={`p-2 rounded-lg border ${action.bg}`}>
                    <action.icon size={16} className={action.color} />
                  </div>
                  <span className="flex-1">{action.label}</span>
                  <ArrowRight size={14} className="text-zinc-600 group-hover:text-zinc-400 group-hover:translate-x-1 transition-all" />
                </Link>
              ))}
            </div>
          </div>

          {/* User Demographics Pie Chart */}
          <div className="glass-card p-6 flex-1">
             <h3 className="font-semibold text-zinc-100 mb-2">User Demographics</h3>
             <p className="text-xs text-zinc-500 mb-4">Breakdown of registered roles</p>
             <div className="h-40">
                <ResponsiveContainer width="100%" height="100%">
                  <PieChart>
                    <Pie 
                      data={[
                        { name: "Students", value: stats?.totalStudents || 0 },
                        { name: "Professors", value: stats?.totalProfessors || 0 },
                      ]} 
                      cx="50%" cy="50%" innerRadius={35} outerRadius={60} paddingAngle={2} dataKey="value" stroke="none"
                    >
                      <Cell fill="#3b82f6" />
                      <Cell fill="#6366f1" />
                    </Pie>
                    <Tooltip content={<CustomTooltip />} />
                    <Legend verticalAlign="bottom" iconType="circle" iconSize={8} formatter={(value) => <span className="text-xs text-zinc-400 ml-1">{value}</span>} />
                  </PieChart>
                </ResponsiveContainer>
             </div>
          </div>
        </div>
      </div>
    </div>
  );
}

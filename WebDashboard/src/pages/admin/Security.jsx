import { useEffect, useState } from "react";
import api from "../../lib/api";
import { Shield, AlertTriangle, Eye, MapPin, Key, Clock, RefreshCw } from "lucide-react";
import StatsCard from "../../components/StatsCard";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from "recharts";
import { format } from "date-fns";

const THREAT_COLORS = {
  FACE_VERIFY_FAILED: "#ef4444",
  REPLAY_REJECTED: "#f59e0b",
  GEOFENCE_FAILED: "#6366f1",
  TOKEN_INVALID: "#3b82f6",
  TIMING_REJECTED: "#8b5cf6",
};

const THREAT_LABELS = {
  FACE_VERIFY_FAILED: "Face Mismatch",
  REPLAY_REJECTED: "Replay Attack",
  GEOFENCE_FAILED: "Geofence Fail",
  TOKEN_INVALID: "Token Invalid",
  TIMING_REJECTED: "Timing Reject",
};

function CustomTooltip({ active, payload, label }) {
  if (!active || !payload?.length) return null;
  return (
    <div className="bg-zinc-900 border border-zinc-700 rounded-xl px-4 py-3 shadow-xl">
      <p className="text-xs text-zinc-400 mb-1">{THREAT_LABELS[label] || label}</p>
      <p className="text-sm font-semibold text-zinc-100">{payload[0]?.value} events</p>
    </div>
  );
}

export default function SecurityDashboard() {
  const [security, setSecurity] = useState(null);
  const [loading, setLoading] = useState(true);

  const fetchSecurity = async () => {
    setLoading(true);
    try {
      const res = await api.get("/admin/dashboard/security");
      setSecurity(res.data);
    } catch (err) {
      console.error("Failed to load security stats", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { fetchSecurity(); }, []);

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
          {[...Array(5)].map((_, i) => (
            <div key={i} className="glass-card p-6">
              <div className="skeleton h-4 w-24 mb-3"></div>
              <div className="skeleton h-8 w-12"></div>
            </div>
          ))}
        </div>
        <div className="glass-card p-6"><div className="skeleton h-72 w-full"></div></div>
      </div>
    );
  }

  const chartData = [
    { name: "FACE_VERIFY_FAILED", count: security?.faceMismatchCount || 0 },
    { name: "REPLAY_REJECTED", count: security?.replayAttempts || 0 },
    { name: "GEOFENCE_FAILED", count: security?.geofenceFailures || 0 },
    { name: "TOKEN_INVALID", count: security?.tokenInvalid || 0 },
    { name: "TIMING_REJECTED", count: security?.timingRejected || 0 },
  ];

  const totalThreats = chartData.reduce((sum, d) => sum + d.count, 0);

  const getStatusColor = (action) => {
    const map = {
      FACE_VERIFY_FAILED: "bg-red-500/10 text-red-400 border-red-500/20",
      REPLAY_REJECTED: "bg-amber-500/10 text-amber-400 border-amber-500/20",
      GEOFENCE_FAILED: "bg-indigo-500/10 text-indigo-400 border-indigo-500/20",
      TOKEN_INVALID: "bg-blue-500/10 text-blue-400 border-blue-500/20",
      TIMING_REJECTED: "bg-purple-500/10 text-purple-400 border-purple-500/20",
    };
    return map[action] || "bg-zinc-800 text-zinc-300";
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Shield className="text-blue-400" size={24} />
            Security Dashboard
          </h1>
          <p className="text-sm text-zinc-400 mt-1">Monitor anti-spoofing and fraud detection events across all sessions.</p>
        </div>
        <button 
          onClick={fetchSecurity}
          className="flex items-center gap-2 px-4 py-2 rounded-xl bg-zinc-800 border border-zinc-700 text-zinc-300 hover:text-white hover:bg-zinc-700 transition-all text-sm"
        >
          <RefreshCw size={14} />
          Refresh
        </button>
      </div>

      {/* Threat Stats */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-4">
        <div className="animate-slide-up stagger-1">
          <StatsCard title="Face Mismatch" value={security?.faceMismatchCount || 0} icon={Eye} color="red" />
        </div>
        <div className="animate-slide-up stagger-2">
          <StatsCard title="Replay Attacks" value={security?.replayAttempts || 0} icon={RefreshCw} color="amber" />
        </div>
        <div className="animate-slide-up stagger-3">
          <StatsCard title="Geofence Failures" value={security?.geofenceFailures || 0} icon={MapPin} color="indigo" />
        </div>
        <div className="animate-slide-up stagger-4">
          <StatsCard title="Token Invalid" value={security?.tokenInvalid || 0} icon={Key} color="blue" />
        </div>
        <div className="animate-slide-up stagger-5">
          <StatsCard title="Timing Rejected" value={security?.timingRejected || 0} icon={Clock} color="purple" />
        </div>
      </div>

      {/* Threat Distribution Chart */}
      <div className="glass-card p-6 animate-fade-in" style={{ animationDelay: "0.3s" }}>
        <div className="flex items-center justify-between mb-6">
          <div>
            <h3 className="font-semibold text-zinc-100 text-lg">Threat Distribution</h3>
            <p className="text-xs text-zinc-500 mt-0.5">Breakdown of security events by type</p>
          </div>
          <span className="px-3 py-1 rounded-lg bg-zinc-800 text-zinc-300 text-xs font-medium border border-zinc-700">
            Total: {totalThreats} events
          </span>
        </div>

        {totalThreats === 0 ? (
          <div className="h-64 flex flex-col items-center justify-center">
            <Shield className="text-emerald-500 mb-3 animate-float" size={48} />
            <p className="text-emerald-400 font-semibold">System Secure</p>
            <p className="text-zinc-500 text-sm mt-1">No security events have been recorded</p>
          </div>
        ) : (
          <ResponsiveContainer width="100%" height={300}>
            <BarChart data={chartData} barSize={40}>
              <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
              <XAxis 
                dataKey="name" 
                stroke="#71717a" 
                fontSize={11} 
                tickLine={false} 
                axisLine={false}
                tickFormatter={v => THREAT_LABELS[v] || v}
              />
              <YAxis stroke="#71717a" fontSize={12} tickLine={false} axisLine={false} allowDecimals={false} />
              <Tooltip content={<CustomTooltip />} />
              <Bar dataKey="count" radius={[6, 6, 0, 0]}>
                {chartData.map((entry, index) => (
                  <Cell key={index} fill={THREAT_COLORS[entry.name] || "#3b82f6"} />
                ))}
              </Bar>
            </BarChart>
          </ResponsiveContainer>
        )}
      </div>

      {/* Recent Security Events Table */}
      <div className="glass-card overflow-hidden animate-fade-in" style={{ animationDelay: "0.5s" }}>
        <div className="p-6 border-b border-zinc-800">
          <h3 className="font-semibold text-zinc-100 flex items-center gap-2">
            <AlertTriangle size={16} className="text-amber-400" />
            Recent Security Events
          </h3>
          <p className="text-xs text-zinc-500 mt-0.5">Latest 50 flagged events</p>
        </div>
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-zinc-300">
            <thead className="bg-zinc-900/50 text-zinc-500 text-xs uppercase tracking-wider border-b border-zinc-800">
              <tr>
                <th className="px-6 py-3 font-medium">Time</th>
                <th className="px-6 py-3 font-medium">Threat Type</th>
                <th className="px-6 py-3 font-medium">User</th>
                <th className="px-6 py-3 font-medium">IP Address</th>
                <th className="px-6 py-3 font-medium">Details</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {(!security?.recentEvents || security.recentEvents.length === 0) ? (
                <tr>
                  <td colSpan="5" className="px-6 py-12 text-center text-zinc-500">
                    <Shield className="mx-auto mb-2 text-emerald-500" size={24} />
                    No security events recorded — system is clean!
                  </td>
                </tr>
              ) : (
                security.recentEvents.map((event, idx) => (
                  <tr key={event.id || idx} className="table-row-hover">
                    <td className="px-6 py-3 text-zinc-400 whitespace-nowrap text-xs">
                      {event.timestamp ? format(new Date(event.timestamp), 'MMM d, HH:mm:ss') : "N/A"}
                    </td>
                    <td className="px-6 py-3">
                      <span className={`px-2.5 py-1 rounded-md text-xs font-medium border ${getStatusColor(event.action)}`}>
                        {THREAT_LABELS[event.action] || event.action}
                      </span>
                    </td>
                    <td className="px-6 py-3 text-xs text-zinc-400">
                      {event.role} #{event.userId}
                    </td>
                    <td className="px-6 py-3 text-xs text-zinc-500 font-mono">
                      {event.ipAddress || "—"}
                    </td>
                    <td className="px-6 py-3 text-xs text-zinc-500 max-w-xs truncate">
                      {event.details ? JSON.stringify(event.details) : "—"}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

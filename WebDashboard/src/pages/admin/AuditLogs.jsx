import { useEffect, useState } from "react";
import api from "../../lib/api";
import { format } from "date-fns";
import { Activity, ChevronDown, ChevronRight } from "lucide-react";

export default function AdminAuditLogs() {
  const [logs, setLogs] = useState([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);
  const [totalPages, setTotalPages] = useState(1);
  const [filterAction, setFilterAction] = useState("");
  const [expandedRow, setExpandedRow] = useState(null);

  const fetchLogs = async (currentPage, actionFilter) => {
    setLoading(true);
    try {
      const params = { page: currentPage, limit: 20 };
      if (actionFilter) params.action = actionFilter;
      const res = await api.get("/admin/dashboard/audit-logs", { params });
      setLogs(res.data.logs || []);
      setTotalPages(res.data.totalPages || 1);
    } catch (err) {
      console.error("Failed to load audit logs", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchLogs(page, filterAction);
  }, [page, filterAction]);

  const getStatusColor = (status) => {
    switch(status) {
      case "SUCCESS": return "bg-emerald-500/10 text-emerald-400 border-emerald-500/20";
      case "FAILURE": return "bg-red-500/10 text-red-400 border-red-500/20";
      case "WARNING": return "bg-amber-500/10 text-amber-400 border-amber-500/20";
      default: return "bg-zinc-800 text-zinc-300 border-zinc-700";
    }
  };

  const actionOptions = [
    { value: "", label: "All Actions" },
    { value: "ATTENDANCE_MARKED", label: "Attendance Marked" },
    { value: "ATTENDANCE_MANUAL", label: "Manual Attendance" },
    { value: "FACE_VERIFY_FAILED", label: "Face Verify Failed" },
    { value: "FACE_REGISTERED", label: "Face Registered" },
    { value: "TOKEN_INVALID", label: "Invalid Tokens" },
    { value: "GEOFENCE_FAILED", label: "Geofence Failed" },
    { value: "REPLAY_REJECTED", label: "Replay Rejected" },
    { value: "TIMING_REJECTED", label: "Timing Rejected" },
    { value: "CSV_UPLOAD", label: "CSV Upload" },
    { value: "ADMIN_ACTION", label: "Admin Actions" },
  ];

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Activity className="text-blue-400" size={24} />
            System Audit Logs
          </h1>
          <p className="text-sm text-zinc-400 mt-1">Review critical security and system events.</p>
        </div>
        
        <select 
          value={filterAction}
          onChange={(e) => { setFilterAction(e.target.value); setPage(1); }}
          className="bg-zinc-900 border border-zinc-800 text-white text-sm rounded-xl focus:ring-blue-500 focus:border-blue-500 px-4 py-2.5 appearance-none cursor-pointer min-w-48"
        >
          {actionOptions.map(opt => (
            <option key={opt.value} value={opt.value}>{opt.label}</option>
          ))}
        </select>
      </div>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-zinc-300">
            <thead className="bg-zinc-900/50 text-zinc-500 text-xs uppercase tracking-wider border-b border-zinc-800">
              <tr>
                <th className="px-6 py-4 font-medium w-8"></th>
                <th className="px-6 py-4 font-medium">Timestamp</th>
                <th className="px-6 py-4 font-medium">Action</th>
                <th className="px-6 py-4 font-medium">User</th>
                <th className="px-6 py-4 font-medium">Status</th>
                <th className="px-6 py-4 font-medium">IP</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {loading ? (
                [...Array(6)].map((_, i) => (
                  <tr key={i}>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-4"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-28"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-32"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-24"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-5 w-16"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-20"></div></td>
                  </tr>
                ))
              ) : logs.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-12 text-center text-zinc-500">
                    No logs found matching criteria.
                  </td>
                </tr>
              ) : (
                logs.map((log) => (
                  <>
                    <tr 
                      key={log.id} 
                      className="table-row-hover cursor-pointer"
                      onClick={() => setExpandedRow(expandedRow === log.id ? null : log.id)}
                    >
                      <td className="px-6 py-4 text-zinc-500">
                        {expandedRow === log.id ? <ChevronDown size={14} /> : <ChevronRight size={14} />}
                      </td>
                      <td className="px-6 py-4 text-zinc-400 whitespace-nowrap text-xs">
                        {log.timestamp ? format(new Date(log.timestamp), 'MMM d, HH:mm:ss') : "N/A"}
                      </td>
                      <td className="px-6 py-4 font-medium text-zinc-100 text-xs">
                        {log.action?.replace(/_/g, " ")}
                      </td>
                      <td className="px-6 py-4 text-xs text-zinc-400">{log.role} #{log.userId}</td>
                      <td className="px-6 py-4">
                        <span className={`px-2.5 py-1 rounded-md text-xs font-medium border ${getStatusColor(log.status)}`}>
                          {log.status}
                        </span>
                      </td>
                      <td className="px-6 py-4 text-xs text-zinc-500 font-mono">
                        {log.ipAddress || "—"}
                      </td>
                    </tr>
                    {expandedRow === log.id && (
                      <tr key={`${log.id}-detail`}>
                        <td colSpan="6" className="px-10 py-4 bg-zinc-900/40 border-t border-zinc-800/50">
                          <p className="text-xs text-zinc-500 mb-1 font-medium uppercase tracking-wider">Details</p>
                          <pre className="text-xs text-zinc-400 font-mono whitespace-pre-wrap bg-zinc-950 p-3 rounded-lg border border-zinc-800 max-h-32 overflow-y-auto custom-scrollbar">
                            {log.details ? JSON.stringify(log.details, null, 2) : "No additional details"}
                          </pre>
                        </td>
                      </tr>
                    )}
                  </>
                ))
              )}
            </tbody>
          </table>
        </div>
        
        {/* Pagination */}
        <div className="p-4 border-t border-zinc-800 flex justify-between items-center bg-zinc-900/30">
          <span className="text-sm text-zinc-400">
            Page <span className="font-medium text-white">{page}</span> of <span className="font-medium text-white">{totalPages}</span>
          </span>
          <div className="flex gap-2">
            <button 
              onClick={() => setPage(p => Math.max(1, p - 1))}
              disabled={page === 1 || loading}
              className="px-4 py-1.5 rounded-lg bg-zinc-800 text-zinc-300 disabled:opacity-40 text-sm hover:bg-zinc-700 transition border border-zinc-700"
            >
              Previous
            </button>
            <button 
              onClick={() => setPage(p => Math.min(totalPages, p + 1))}
              disabled={page === totalPages || loading}
              className="px-4 py-1.5 rounded-lg bg-zinc-800 text-zinc-300 disabled:opacity-40 text-sm hover:bg-zinc-700 transition border border-zinc-700"
            >
              Next
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

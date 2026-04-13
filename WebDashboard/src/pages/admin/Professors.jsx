import { useEffect, useState } from "react";
import api from "../../lib/api";
import { format } from "date-fns";
import { GraduationCap, Search } from "lucide-react";

export default function AdminProfessors() {
  const [professors, setProfessors] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");

  useEffect(() => {
    const fetchProfessors = async () => {
      try {
        const res = await api.get("/admin/professor/viewAll");
        setProfessors(res.data.professor || []);
      } catch (err) {
        console.error("Failed to load professors", err);
      } finally {
        setLoading(false);
      }
    };
    fetchProfessors();
  }, []);

  const filtered = professors.filter(p =>
    p.name?.toLowerCase().includes(search.toLowerCase()) ||
    p.email?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <GraduationCap className="text-indigo-400" size={24} />
            Manage Professors
          </h1>
          <p className="text-sm text-zinc-400 mt-1">View all registered professors in the system.</p>
        </div>
        <div className="flex items-center gap-3">
          <span className="px-3 py-1.5 rounded-lg bg-indigo-500/10 text-indigo-400 text-xs font-medium border border-indigo-500/20">
            {professors.length} total
          </span>
          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-zinc-500" size={16} />
            <input 
              type="text" 
              placeholder="Search professors..." 
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="pl-9 pr-4 py-2 bg-zinc-900 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-indigo-500/50 focus:border-indigo-500 text-white w-56 transition-all"
            />
          </div>
        </div>
      </div>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-zinc-300">
            <thead className="bg-zinc-900/50 text-zinc-500 text-xs uppercase tracking-wider border-b border-zinc-800">
              <tr>
                <th className="px-6 py-4 font-medium">Name</th>
                <th className="px-6 py-4 font-medium">Email</th>
                <th className="px-6 py-4 font-medium">Status</th>
                <th className="px-6 py-4 font-medium">Joined</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {loading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i}>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-32"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-40"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-5 w-14"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-24"></div></td>
                  </tr>
                ))
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan="4" className="px-6 py-12 text-center text-zinc-500">
                    {professors.length === 0 ? "No professors found." : "No professors match your search."}
                  </td>
                </tr>
              ) : (
                filtered.map((prof, idx) => (
                  <tr key={prof.id || prof._id} className="table-row-hover animate-fade-in" style={{ animationDelay: `${idx * 0.03}s` }}>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="w-8 h-8 rounded-full bg-gradient-to-br from-indigo-500/30 to-purple-500/30 flex items-center justify-center text-xs font-bold text-indigo-300 border border-indigo-500/20 shrink-0">
                          {prof.name?.charAt(0)?.toUpperCase() || "?"}
                        </div>
                        <span className="font-medium text-zinc-100">{prof.name}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 text-zinc-400">{prof.email}</td>
                    <td className="px-6 py-4">
                      {prof.isDisabled ? (
                        <span className="px-2.5 py-1 rounded-md bg-red-500/10 text-red-400 text-xs font-medium border border-red-500/20">Disabled</span>
                      ) : (
                        <span className="px-2.5 py-1 rounded-md bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">Active</span>
                      )}
                    </td>
                    <td className="px-6 py-4 text-zinc-400 text-xs">
                      {prof.createdAt ? format(new Date(prof.createdAt), 'MMM d, yyyy') : "N/A"}
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

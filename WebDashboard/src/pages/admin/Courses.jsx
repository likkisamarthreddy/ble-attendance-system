import { useEffect, useState } from "react";
import api from "../../lib/api";
import { format } from "date-fns";
import { BookOpen, Search, Users } from "lucide-react";

export default function AdminCourses() {
  const [courses, setCourses] = useState([]);
  const [loading, setLoading] = useState(true);
  const [showArchived, setShowArchived] = useState(false);
  const [search, setSearch] = useState("");

  const fetchCourses = async (archived) => {
    setLoading(true);
    try {
      const endpoint = archived ? "/admin/course/viewArchive" : "/admin/course/viewCurrent";
      const res = await api.get(endpoint);
      setCourses(res.data.courses || []);
    } catch (err) {
      console.error("Failed to load courses", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchCourses(showArchived);
  }, [showArchived]);

  const filtered = courses.filter(c =>
    c.name?.toLowerCase().includes(search.toLowerCase()) ||
    c.batch?.toLowerCase().includes(search.toLowerCase()) ||
    c.professor?.name?.toLowerCase().includes(search.toLowerCase())
  );

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <BookOpen className="text-blue-400" size={24} />
            Manage Courses
          </h1>
          <p className="text-sm text-zinc-400 mt-1">View active and archived courses across all professors.</p>
        </div>
        
        <div className="flex items-center gap-3">
          <div className="flex bg-zinc-900 p-1 rounded-xl border border-zinc-800">
            <button
              onClick={() => { setShowArchived(false); setSearch(""); }}
              className={`px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${!showArchived ? 'bg-blue-600 text-white shadow-md shadow-blue-500/20' : 'text-zinc-400 hover:text-white hover:bg-zinc-800'}`}
            >
              Active
            </button>
            <button
              onClick={() => { setShowArchived(true); setSearch(""); }}
              className={`px-4 py-2 text-sm font-medium rounded-lg transition-all duration-200 ${showArchived ? 'bg-zinc-700 text-white shadow' : 'text-zinc-400 hover:text-white hover:bg-zinc-800'}`}
            >
              Archived
            </button>
          </div>

          <div className="relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-zinc-500" size={16} />
            <input 
              type="text" 
              placeholder="Search courses..." 
              value={search}
              onChange={e => setSearch(e.target.value)}
              className="pl-9 pr-4 py-2 bg-zinc-900 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500 text-white w-56 transition-all"
            />
          </div>
        </div>
      </div>

      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-zinc-300">
            <thead className="bg-zinc-900/50 text-zinc-500 text-xs uppercase tracking-wider border-b border-zinc-800">
              <tr>
                <th className="px-6 py-4 font-medium">Course Name</th>
                <th className="px-6 py-4 font-medium">Batch / Year</th>
                <th className="px-6 py-4 font-medium">Professor</th>
                <th className="px-6 py-4 font-medium">Students</th>
                <th className="px-6 py-4 font-medium">Joining Code</th>
                <th className="px-6 py-4 font-medium">Expiry</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {loading ? (
                [...Array(4)].map((_, i) => (
                  <tr key={i}>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-32"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-24"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-28"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-8"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-20"></div></td>
                    <td className="px-6 py-4"><div className="skeleton h-4 w-24"></div></td>
                  </tr>
                ))
              ) : filtered.length === 0 ? (
                <tr>
                  <td colSpan="6" className="px-6 py-12 text-center text-zinc-500">
                    No {showArchived ? "archived" : "active"} courses found.
                  </td>
                </tr>
              ) : (
                filtered.map((course, idx) => (
                  <tr key={course.id || course._id} className="table-row-hover animate-fade-in" style={{ animationDelay: `${idx * 0.03}s` }}>
                    <td className="px-6 py-4 font-medium text-zinc-100">{course.name}</td>
                    <td className="px-6 py-4 text-zinc-400">{course.batch} (Year {course.year})</td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-2">
                        <div className="w-6 h-6 rounded-full bg-indigo-500/20 flex items-center justify-center text-[10px] font-bold text-indigo-300 shrink-0">
                          {course.professor?.name?.charAt(0) || "?"}
                        </div>
                        <span>{course.professor?.name || "Unknown"}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-1.5">
                        <Users size={14} className="text-zinc-500" />
                        <span className="text-zinc-300">{course._count?.students ?? course.students?.length ?? "—"}</span>
                      </div>
                    </td>
                    <td className="px-6 py-4 font-mono text-zinc-400 text-xs">{course.joiningCode || course.joining_code}</td>
                    <td className="px-6 py-4 text-xs">
                       {course.courseExpiry || course.course_expiry 
                         ? format(new Date(course.courseExpiry || course.course_expiry), 'MMM d, yyyy') 
                         : <span className="text-zinc-500">No Expiry</span>}
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

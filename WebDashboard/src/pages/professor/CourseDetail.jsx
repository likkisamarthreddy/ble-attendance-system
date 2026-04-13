import { useEffect, useState } from "react";
import { useParams, useNavigate } from "react-router-dom";
import api from "../../lib/api";
import { Users, Calendar, ArrowLeft, Search } from "lucide-react";

export default function CourseDetail() {
  const { id } = useParams();
  const navigate = useNavigate();
  const [loading, setLoading] = useState(true);
  const [students, setStudents] = useState([]);
  const [search, setSearch] = useState("");
  
  useEffect(() => {
    const fetchCourseDetails = async () => {
      try {
        const res = await api.get("/professor/course/students", { params: { courseId: id } });
        setStudents(res.data.students || []);
      } catch (err) {
        console.error("Failed to load course details", err);
      } finally {
        setLoading(false);
      }
    };
    if (id) fetchCourseDetails();
  }, [id]);

  const filtered = students.filter(s => 
    s.name?.toLowerCase().includes(search.toLowerCase()) ||
    s.rollno?.toLowerCase().includes(search.toLowerCase())
  );

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="skeleton h-8 w-48"></div>
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          <div className="glass-card p-6"><div className="skeleton h-64 w-full"></div></div>
          <div className="glass-card p-6"><div className="skeleton h-64 w-full"></div></div>
        </div>
      </div>
    );
  }

  return (
    <div className="space-y-6">
      <div className="flex items-center gap-4">
        <button 
          onClick={() => navigate(-1)}
          className="p-2.5 bg-zinc-900 border border-zinc-800 rounded-xl text-zinc-400 hover:text-white hover:bg-zinc-800 transition-all"
        >
          <ArrowLeft size={20} />
        </button>
        <div>
          <h1 className="text-2xl font-bold text-white">Course Details</h1>
          <p className="text-sm text-zinc-400 mt-1">Manage enrolled students and view attendance history.</p>
        </div>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
        {/* Enrolled Students Panel */}
        <div className="glass-card animate-slide-up stagger-1">
          <div className="p-6 border-b border-zinc-800">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-3">
                <div className="p-2 rounded-lg bg-blue-500/10 border border-blue-500/20">
                  <Users size={20} className="text-blue-400" />
                </div>
                <div>
                  <h2 className="text-lg font-semibold text-white">Enrolled Students</h2>
                  <p className="text-xs text-zinc-500">{students.length} students</p>
                </div>
              </div>
            </div>

            {students.length > 3 && (
              <div className="relative mt-4">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-zinc-500" size={14} />
                <input 
                  type="text" 
                  placeholder="Search students..." 
                  value={search}
                  onChange={e => setSearch(e.target.value)}
                  className="w-full pl-9 pr-4 py-2 bg-zinc-900 border border-zinc-800 rounded-lg text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 text-white transition-all"
                />
              </div>
            )}
          </div>
          
          <div className="max-h-96 overflow-y-auto custom-scrollbar p-4">
            {filtered.length === 0 ? (
              <p className="text-zinc-500 text-sm p-4 text-center bg-zinc-900/50 rounded-lg border border-zinc-800">
                {students.length === 0
                  ? "No students enrolled in this course yet. Share the joining code for students to enroll."
                  : "No students match your search."}
              </p>
            ) : (
              <div className="space-y-2">
                {filtered.map((student, idx) => (
                  <div 
                    key={student.id} 
                    className="bg-zinc-900/50 border border-zinc-800 rounded-xl p-3.5 flex items-center gap-3 hover:bg-zinc-800/50 transition-all animate-fade-in"
                    style={{ animationDelay: `${idx * 0.03}s` }}
                  >
                    <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500/30 to-indigo-500/30 flex items-center justify-center text-xs font-bold text-blue-300 border border-blue-500/20 shrink-0">
                      {student.name?.charAt(0)?.toUpperCase() || "?"}
                    </div>
                    <div className="flex-1 min-w-0">
                      <p className="font-medium text-zinc-200 text-sm truncate">{student.name}</p>
                      <p className="text-xs text-zinc-500 font-mono">Roll: {student.rollno}</p>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>

        {/* Sessions Panel */}
        <div className="glass-card flex flex-col items-center justify-center text-center p-8 animate-slide-up stagger-2">
          <div className="w-16 h-16 rounded-2xl bg-zinc-800/50 flex items-center justify-center mb-4 border border-zinc-700">
            <Calendar className="text-zinc-500" size={32} />
          </div>
          <h3 className="text-lg font-semibold text-zinc-300 mb-2">Previous Sessions</h3>
          <p className="text-sm text-zinc-500 max-w-sm mb-6">
            View historical attendance data, export CSV sheets, or view detailed session analytics.
          </p>
          <button className="bg-blue-600 hover:bg-blue-500 text-white px-6 py-2.5 rounded-xl font-medium transition-all shadow-md shadow-blue-500/20 hover:shadow-lg hover:shadow-blue-500/30">
            View History
          </button>
        </div>
      </div>
    </div>
  );
}

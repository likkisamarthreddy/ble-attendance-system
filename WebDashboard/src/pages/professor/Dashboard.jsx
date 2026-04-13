import { useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import api from "../../lib/api";
import { BookOpen, Search, UserCheck, TrendingUp, Plus, X, BarChart2 } from "lucide-react";
import StatsCard from "../../components/StatsCard";
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, ResponsiveContainer, Cell } from "recharts";

export default function ProfessorDashboard() {
  const [profile, setProfile] = useState(null);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const navigate = useNavigate();

  // Modal State
  const [showModal, setShowModal] = useState(false);
  const [createLoading, setCreateLoading] = useState(false);
  const [formData, setFormData] = useState({
    name: "",
    batch: "",
    year: new Date().getFullYear().toString(),
    courseExpiry: "",
  });

  const fetchProfile = async () => {
    try {
      const res = await api.get("/professor/profile");
      setProfile(res.data);
    } catch (err) {
      console.error("Failed to load profile", err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProfile();
  }, []);

  const handleCreateCourse = async (e) => {
    e.preventDefault();
    setCreateLoading(true);
    try {
      await api.post("/professor/course", formData);
      setShowModal(false);
      setFormData({ name: "", batch: "", year: new Date().getFullYear().toString(), courseExpiry: "" });
      fetchProfile(); // Refresh the courses list
    } catch (err) {
      alert(err.response?.data?.message || err.message);
    } finally {
      setCreateLoading(false);
    }
  };

  if (loading) {
    return (
      <div className="space-y-6">
        <div className="skeleton h-8 w-60 mb-4"></div>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
          {[...Array(3)].map((_, i) => (
            <div key={i} className="glass-card p-6"><div className="skeleton h-16 w-full"></div></div>
          ))}
        </div>
        <div className="glass-card p-6"><div className="skeleton h-64 w-full"></div></div>
      </div>
    );
  }

  const activeCourses = (profile?.courses || []).filter(c => !c.isArchived);
  const totalStudents = profile?.courses?.reduce((acc, c) => acc + (c._count?.students || 0), 0) ?? 0;

  const filteredCourses = activeCourses.filter(c =>
    c.name.toLowerCase().includes(search.toLowerCase()) ||
    c.batch.toLowerCase().includes(search.toLowerCase())
  );

  // Chart Data: Students per active course
  const chartData = activeCourses.map(c => ({
    name: c.name,
    students: c._count?.students || 0
  }));

  const CustomTooltip = ({ active, payload, label }) => {
    if (active && payload && payload.length) {
      return (
        <div className="bg-zinc-900 border border-zinc-700 rounded-xl px-4 py-3 shadow-xl">
          <p className="text-xs text-zinc-400 mb-1">{label}</p>
          <p className="text-sm font-semibold text-blue-400">{payload[0].value} Enrolled Students</p>
        </div>
      );
    }
    return null;
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white">Welcome back, <span className="gradient-text">{profile?.name}</span></h1>
          <p className="text-sm text-zinc-400 mt-1">Manage your courses and view class statistics.</p>
        </div>
        <button 
          onClick={() => setShowModal(true)}
          className="bg-blue-600 hover:bg-blue-500 text-white px-4 py-2 rounded-xl text-sm font-medium transition-all shadow-md flex items-center gap-2"
        >
          <Plus size={16} />
          Create Course
        </button>
      </div>

      {/* Stats */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
        <div className="animate-slide-up stagger-1">
          <StatsCard title="Active Courses" value={activeCourses.length} icon={BookOpen} color="blue" />
        </div>
        <div className="animate-slide-up stagger-2">
          <StatsCard title="Total Students" value={totalStudents} subtitle="Across all courses" icon={UserCheck} color="indigo" />
        </div>
        <div className="animate-slide-up stagger-3">
          <StatsCard title="Archived Courses" value={(profile?.courses || []).filter(c => c.isArchived).length} icon={TrendingUp} color="amber" />
        </div>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Course List (Spans 2 cols) */}
        <div className="lg:col-span-2 glass-card p-6 animate-fade-in" style={{ animationDelay: "0.2s" }}>
          <div className="flex justify-between items-center mb-6">
            <h2 className="text-lg font-semibold text-zinc-100">Your Active Courses</h2>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-zinc-500" size={16} />
              <input 
                type="text" 
                placeholder="Search courses..." 
                value={search}
                onChange={e => setSearch(e.target.value)}
                className="pl-9 pr-4 py-2 bg-zinc-900 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:ring-1 focus:ring-blue-500/50 text-white w-full sm:w-64 transition-all"
              />
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4 max-h-[400px] overflow-y-auto custom-scrollbar pr-2">
            {filteredCourses.length === 0 ? (
              <div className="col-span-full border border-zinc-800 border-dashed rounded-xl p-8 text-center text-zinc-500">
                {activeCourses.length === 0 
                  ? "You don't have any active courses. Click 'Create Course' to get started."
                  : "No courses match your search."}
              </div>
            ) : (
              filteredCourses.map((course, idx) => (
                <div 
                  key={course.id} 
                  className="bg-zinc-900/50 border border-zinc-800 rounded-xl group hover:border-blue-500/30 transition-all cursor-pointer animate-slide-up"
                  style={{ animationDelay: `${(idx + 1) * 0.05}s` }}
                  onClick={() => navigate(`/professor/courses/${course.id}`)}
                >
                  <div className="p-5">
                    <div className="flex justify-between items-start mb-3">
                      <div>
                        <h3 className="text-base font-semibold text-zinc-100 group-hover:text-blue-400 transition-colors truncate max-w-[150px]">
                          {course.name}
                        </h3>
                        <p className="text-xs text-zinc-400">Batch {course.batch} • Yr {course.year}</p>
                      </div>
                      <span className="px-2 py-0.5 rounded text-[10px] font-medium bg-emerald-500/10 text-emerald-400 border border-emerald-500/20">
                        Active
                      </span>
                    </div>
                    
                    <div className="flex justify-between items-center pt-3 border-t border-zinc-800/50 mt-4">
                      <div className="flex items-center gap-1.5 text-xs text-zinc-400">
                        <UserCheck size={14} className="text-blue-400" />
                        <span>{course._count?.students || 0} students</span>
                      </div>
                      <span className="text-[10px] text-zinc-500 font-mono bg-zinc-900 px-1.5 py-0.5 rounded border border-zinc-700">
                        {course.joiningCode}
                      </span>
                    </div>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* Chart Panel */}
        <div className="glass-card p-6 animate-fade-in" style={{ animationDelay: "0.3s" }}>
          <div className="flex items-center gap-2 mb-6">
            <BarChart2 className="text-indigo-400" size={18} />
            <h2 className="text-base font-semibold text-zinc-100">Students per Course</h2>
          </div>
          
          {chartData.length === 0 ? (
            <div className="h-64 flex items-center justify-center text-zinc-500 text-sm">
              No data available
            </div>
          ) : (
            <div className="h-64">
              <ResponsiveContainer width="100%" height="100%">
                <BarChart data={chartData} margin={{ top: 0, right: 0, left: -20, bottom: 0 }}>
                  <CartesianGrid strokeDasharray="3 3" stroke="#27272a" vertical={false} />
                  <XAxis dataKey="name" stroke="#71717a" fontSize={10} tickLine={false} axisLine={false} tickFormatter={(v) => v.substring(0, 10)} />
                  <YAxis stroke="#71717a" fontSize={10} tickLine={false} axisLine={false} allowDecimals={false} />
                  <Tooltip content={<CustomTooltip />} cursor={{ fill: '#27272a', opacity: 0.4 }} />
                  <Bar dataKey="students" radius={[4, 4, 0, 0]} maxBarSize={40}>
                    {chartData.map((entry, index) => (
                      <Cell key={`cell-${index}`} fill={index % 2 === 0 ? "#3b82f6" : "#6366f1"} />
                    ))}
                  </Bar>
                </BarChart>
              </ResponsiveContainer>
            </div>
          )}
        </div>
      </div>

      {/* Create Course Modal */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-zinc-900 border border-zinc-800 rounded-2xl w-full max-w-md shadow-2xl overflow-hidden animate-slide-up">
            <div className="px-6 py-4 border-b border-zinc-800 flex justify-between items-center bg-zinc-900/50">
              <h3 className="text-lg font-semibold text-white">Create New Course</h3>
              <button 
                onClick={() => setShowModal(false)}
                className="text-zinc-400 hover:text-white transition-colors"
              >
                <X size={20} />
              </button>
            </div>
            
            <form onSubmit={handleCreateCourse} className="p-6 space-y-4">
              <div>
                <label className="block text-xs font-medium text-zinc-400 mb-1.5">Course Name</label>
                <input
                  type="text"
                  required
                  value={formData.name}
                  onChange={e => setFormData({ ...formData, name: e.target.value })}
                  placeholder="e.g. Advanced AI"
                  className="w-full bg-zinc-900 border border-zinc-700 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                />
              </div>
              
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-medium text-zinc-400 mb-1.5">Batch</label>
                  <input
                    type="text"
                    required
                    value={formData.batch}
                    onChange={e => setFormData({ ...formData, batch: e.target.value })}
                    placeholder="e.g. B1"
                    className="w-full bg-zinc-900 border border-zinc-700 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                  />
                </div>
                <div>
                  <label className="block text-xs font-medium text-zinc-400 mb-1.5">Year</label>
                  <input
                    type="number"
                    required
                    value={formData.year}
                    onChange={e => setFormData({ ...formData, year: e.target.value })}
                    placeholder="e.g. 2026"
                    className="w-full bg-zinc-900 border border-zinc-700 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-medium text-zinc-400 mb-1.5">Course Expiry Date (Optional)</label>
                <input
                  type="date"
                  value={formData.courseExpiry}
                  onChange={e => setFormData({ ...formData, courseExpiry: e.target.value })}
                  className="w-full bg-zinc-900 border border-zinc-700 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all appearance-none"
                />
              </div>

              <div className="pt-4 flex gap-3">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="flex-1 py-2.5 rounded-xl border border-zinc-700 text-zinc-300 hover:bg-zinc-800 transition-all font-medium text-sm"
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  disabled={createLoading}
                  className="flex-1 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-500 text-white shadow-lg shadow-blue-500/20 transition-all font-medium text-sm flex items-center justify-center disable:opacity-50"
                >
                  {createLoading ? (
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  ) : "Create Course"}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
}

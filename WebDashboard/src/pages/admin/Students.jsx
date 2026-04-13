import { useEffect, useState, useMemo } from "react";
import api from "../../lib/api";
import { format } from "date-fns";
import {
  ShieldCheck,
  ShieldAlert,
  Search,
  Users,
  Trash2,
  ArrowUpDown,
  AlertTriangle,
  Filter,
  X,
  UserX,
} from "lucide-react";

export default function AdminStudents() {
  const [students, setStudents] = useState([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState("");
  const [sortAsc, setSortAsc] = useState(true);
  const [batchFilter, setBatchFilter] = useState("");
  const [showDeleteConfirm, setShowDeleteConfirm] = useState(null);
  const [deletingId, setDeletingId] = useState(null);

  useEffect(() => {
    const fetchStudents = async () => {
      try {
        // Use the new detailed endpoint that includes attendance data
        const res = await api.get("/admin/students/detailed");
        setStudents(res.data.students || []);
      } catch (err) {
        console.error("Failed to load students", err);
        // Fallback to old endpoint
        try {
          const res = await api.get("/professor/students");
          setStudents(res.data.student || []);
        } catch (err2) {
          console.error("Fallback also failed", err2);
        }
      } finally {
        setLoading(false);
      }
    };
    fetchStudents();
  }, []);

  // Derive unique batches for filter dropdown
  const allBatches = useMemo(() => {
    const batchSet = new Set();
    students.forEach((s) => {
      if (Array.isArray(s.batch)) {
        s.batch.forEach((b) => {
          if (b) batchSet.add(String(b));
        });
      }
    });
    return Array.from(batchSet).sort();
  }, [students]);

  // Filter + search + sort
  const processed = useMemo(() => {
    let result = [...students];

    // Batch filter
    if (batchFilter) {
      result = result.filter((s) =>
        Array.isArray(s.batch)
          ? s.batch.some(
              (b) =>
                String(b).toLowerCase() === batchFilter.toLowerCase()
            )
          : false
      );
    }

    // Search
    if (search) {
      const q = search.toLowerCase();
      result = result.filter(
        (s) =>
          s.name?.toLowerCase().includes(q) ||
          s.email?.toLowerCase().includes(q) ||
          s.rollno?.toString().includes(q) ||
          (Array.isArray(s.batch) &&
            s.batch.some((b) => String(b).toLowerCase().includes(q)))
      );
    }

    // Sort by roll number
    result.sort((a, b) =>
      sortAsc ? (a.rollno || 0) - (b.rollno || 0) : (b.rollno || 0) - (a.rollno || 0)
    );

    return result;
  }, [students, search, sortAsc, batchFilter]);

  const criticalStudents = useMemo(() => {
    return students.filter(
      (s) => s.attendancePercentage !== null && s.attendancePercentage < 75
    );
  }, [students]);

  const handleRemoveFace = async (studentId) => {
    if (
      !window.confirm(
        "Are you sure you want to remove this student's face profile? They will need to register their face again."
      )
    ) {
      return;
    }

    try {
      await api.delete(`/admin/student/${studentId}/face`);
      setStudents((prev) =>
        prev.map((s) => {
          if (s.id === studentId || s._id === studentId) {
            return { ...s, faceRegisteredAt: null, hasRegisteredFace: false };
          }
          return s;
        })
      );
    } catch (err) {
      console.error("Failed to remove face profile", err);
      alert("Failed to remove face profile");
    }
  };

  const handleDeleteStudent = async (studentId) => {
    setDeletingId(studentId);
    try {
      await api.delete(`/admin/student/${studentId}`);
      setStudents((prev) =>
        prev.filter((s) => s.id !== studentId && s._id !== studentId)
      );
      setShowDeleteConfirm(null);
    } catch (err) {
      console.error("Failed to delete student", err);
      alert("Failed to delete student: " + (err.response?.data?.message || err.message));
    } finally {
      setDeletingId(null);
    }
  };

  const getAttendanceColor = (pct) => {
    if (pct === null || pct === undefined) return "text-zinc-500";
    if (pct >= 85) return "text-emerald-400";
    if (pct >= 75) return "text-blue-400";
    if (pct >= 60) return "text-amber-400";
    return "text-red-400";
  };

  const getAttendanceBgColor = (pct) => {
    if (pct === null || pct === undefined) return "bg-zinc-500/10";
    if (pct >= 85) return "bg-emerald-500/10";
    if (pct >= 75) return "bg-blue-500/10";
    if (pct >= 60) return "bg-amber-500/10";
    return "bg-red-500/10";
  };

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row justify-between items-start md:items-center gap-4">
        <div>
          <h1 className="text-2xl font-bold text-white flex items-center gap-2">
            <Users className="text-blue-400" size={24} />
            Manage Students
          </h1>
          <p className="text-sm text-zinc-400 mt-1">
            View all registered students, attendance stats, and face enrollment status.
          </p>
        </div>
        <div className="flex items-center gap-3 flex-wrap">
          {/* Critical badge */}
          {criticalStudents.length > 0 && (
            <span className="px-3 py-1.5 rounded-lg bg-red-500/10 text-red-400 text-xs font-medium border border-red-500/20 flex items-center gap-1.5">
              <AlertTriangle size={13} />
              {criticalStudents.length} critical (&lt;75%)
            </span>
          )}
          <span className="px-3 py-1.5 rounded-lg bg-blue-500/10 text-blue-400 text-xs font-medium border border-blue-500/20">
            {students.length} total
          </span>
        </div>
      </div>

      {/* Controls row */}
      <div className="flex flex-col sm:flex-row gap-3 items-stretch sm:items-center">
        {/* Search */}
        <div className="relative flex-1">
          <Search
            className="absolute left-3 top-1/2 transform -translate-y-1/2 text-zinc-500"
            size={16}
          />
          <input
            id="student-search"
            type="text"
            placeholder="Search by name, email, roll no, or batch..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2.5 bg-zinc-900 border border-zinc-800 rounded-xl text-sm focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:border-blue-500 text-white transition-all"
          />
          {search && (
            <button
              onClick={() => setSearch("")}
              className="absolute right-3 top-1/2 transform -translate-y-1/2 text-zinc-500 hover:text-white transition-colors"
            >
              <X size={14} />
            </button>
          )}
        </div>

        {/* Batch filter */}
        {allBatches.length > 0 && (
          <div className="relative">
            <Filter
              className="absolute left-3 top-1/2 transform -translate-y-1/2 text-zinc-500"
              size={14}
            />
            <select
              id="batch-filter"
              value={batchFilter}
              onChange={(e) => setBatchFilter(e.target.value)}
              className="pl-9 pr-8 py-2.5 bg-zinc-900 border border-zinc-800 rounded-xl text-sm text-white focus:outline-none focus:ring-2 focus:ring-blue-500/50 appearance-none cursor-pointer min-w-[140px]"
            >
              <option value="">All Batches</option>
              {allBatches.map((b) => (
                <option key={b} value={b}>
                  {b}
                </option>
              ))}
            </select>
          </div>
        )}

        {/* Sort toggle */}
        <button
          id="sort-toggle"
          onClick={() => setSortAsc(!sortAsc)}
          className="flex items-center gap-2 px-4 py-2.5 bg-zinc-900 border border-zinc-800 rounded-xl text-sm text-zinc-300 hover:border-blue-500/50 hover:text-white transition-all whitespace-nowrap"
        >
          <ArrowUpDown size={14} />
          Roll No {sortAsc ? "↑" : "↓"}
        </button>
      </div>

      {/* Table */}
      <div className="glass-card overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-sm text-zinc-300">
            <thead className="bg-zinc-900/50 text-zinc-500 text-xs uppercase tracking-wider border-b border-zinc-800">
              <tr>
                <th className="px-6 py-4 font-medium">Roll No</th>
                <th className="px-6 py-4 font-medium">Student</th>
                <th className="px-6 py-4 font-medium">Email</th>
                <th className="px-6 py-4 font-medium text-center">Attendance</th>
                <th className="px-6 py-4 font-medium text-center">Face Profile</th>
                <th className="px-6 py-4 font-medium">Status</th>
                <th className="px-6 py-4 font-medium text-center">Actions</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-zinc-800/50">
              {loading ? (
                [...Array(5)].map((_, i) => (
                  <tr key={i}>
                    <td className="px-6 py-4">
                      <div className="skeleton h-4 w-16"></div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="flex items-center gap-3">
                        <div className="skeleton h-9 w-9 rounded-full"></div>
                        <div className="skeleton h-4 w-28"></div>
                      </div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="skeleton h-4 w-40"></div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="skeleton h-5 w-12 mx-auto"></div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="skeleton h-5 w-5 mx-auto rounded-full"></div>
                    </td>
                    <td className="px-6 py-4">
                      <div className="skeleton h-5 w-14"></div>
                    </td>
                    <td className="px-6 py-4 text-center">
                      <div className="skeleton h-5 w-8 mx-auto"></div>
                    </td>
                  </tr>
                ))
              ) : processed.length === 0 ? (
                <tr>
                  <td colSpan="7" className="px-6 py-12 text-center text-zinc-500">
                    {students.length === 0
                      ? "No students registered yet."
                      : "No students match your search."}
                  </td>
                </tr>
              ) : (
                processed.map((student, idx) => {
                  const studentId = student.id || student._id;
                  const pct = student.attendancePercentage;
                  const isCritical = pct !== null && pct !== undefined && pct < 75;

                  return (
                    <tr
                      key={studentId}
                      className="table-row-hover animate-fade-in"
                      style={{ animationDelay: `${idx * 0.03}s` }}
                    >
                      {/* Roll No */}
                      <td className="px-6 py-4 font-mono text-zinc-400 text-xs">
                        {student.rollno}
                      </td>

                      {/* Name + Avatar */}
                      <td className="px-6 py-4">
                        <div className="flex items-center gap-3">
                          {student.profilePicture ? (
                            <img
                              src={
                                student.profilePicture.startsWith("data:")
                                  ? student.profilePicture
                                  : `data:image/jpeg;base64,${student.profilePicture}`
                              }
                              alt={student.name}
                              className="w-9 h-9 rounded-full object-cover border border-zinc-700 shrink-0"
                            />
                          ) : (
                            <div className="w-9 h-9 rounded-full bg-gradient-to-br from-blue-500/30 to-indigo-500/30 flex items-center justify-center text-xs font-bold text-blue-300 border border-blue-500/20 shrink-0">
                              {student.name?.charAt(0)?.toUpperCase() || "?"}
                            </div>
                          )}
                          <div>
                            <span className="font-medium text-zinc-100 block">
                              {student.name}
                            </span>
                            {isCritical && (
                              <span className="inline-flex items-center gap-1 text-[10px] text-red-400 font-medium mt-0.5">
                                <AlertTriangle size={10} /> Critical
                              </span>
                            )}
                          </div>
                        </div>
                      </td>

                      {/* Email */}
                      <td className="px-6 py-4 text-zinc-400">{student.email}</td>

                      {/* Attendance % */}
                      <td className="px-6 py-4 text-center">
                        {pct !== null && pct !== undefined ? (
                          <div className="flex flex-col items-center gap-1">
                            <span
                              className={`px-2.5 py-1 rounded-md text-xs font-semibold ${getAttendanceColor(
                                pct
                              )} ${getAttendanceBgColor(pct)}`}
                            >
                              {pct}%
                            </span>
                            {/* Mini progress bar */}
                            <div className="w-16 h-1 rounded-full bg-zinc-800 overflow-hidden">
                              <div
                                className={`h-full rounded-full transition-all duration-500 ${
                                  pct >= 85
                                    ? "bg-emerald-500"
                                    : pct >= 75
                                    ? "bg-blue-500"
                                    : pct >= 60
                                    ? "bg-amber-500"
                                    : "bg-red-500"
                                }`}
                                style={{ width: `${Math.min(pct, 100)}%` }}
                              ></div>
                            </div>
                          </div>
                        ) : (
                          <span className="text-zinc-600 text-xs">N/A</span>
                        )}
                      </td>

                      {/* Face Profile */}
                      <td className="px-6 py-4">
                        {student.faceRegisteredAt || student.hasRegisteredFace ? (
                          <div className="flex justify-center items-center gap-3">
                            <div
                              title={
                                student.faceRegisteredAt
                                  ? `Registered ${format(
                                      new Date(student.faceRegisteredAt),
                                      "MMM d, yyyy"
                                    )}`
                                  : "Face registered"
                              }
                            >
                              <ShieldCheck size={18} className="text-emerald-500" />
                            </div>
                            <button
                              onClick={() =>
                                handleRemoveFace(studentId)
                              }
                              title="Remove Face Registration"
                              className="p-1 rounded hover:bg-red-500/10 text-zinc-500 hover:text-red-400 transition-colors"
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        ) : (
                          <div
                            className="flex justify-center"
                            title="No face profile"
                          >
                            <ShieldAlert size={18} className="text-red-400/60" />
                          </div>
                        )}
                      </td>

                      {/* Status */}
                      <td className="px-6 py-4">
                        {student.isDisabled ? (
                          <span className="px-2.5 py-1 rounded-md bg-red-500/10 text-red-400 text-xs font-medium border border-red-500/20">
                            Disabled
                          </span>
                        ) : (
                          <span className="px-2.5 py-1 rounded-md bg-emerald-500/10 text-emerald-400 text-xs font-medium border border-emerald-500/20">
                            Active
                          </span>
                        )}
                      </td>

                      {/* Delete */}
                      <td className="px-6 py-4 text-center">
                        {showDeleteConfirm === studentId ? (
                          <div className="flex items-center gap-1 justify-center">
                            <button
                              onClick={() =>
                                handleDeleteStudent(studentId)
                              }
                              disabled={deletingId === studentId}
                              className="px-2 py-1 rounded bg-red-600 text-white text-xs hover:bg-red-700 transition-colors disabled:opacity-50"
                            >
                              {deletingId === studentId
                                ? "..."
                                : "Yes"}
                            </button>
                            <button
                              onClick={() =>
                                setShowDeleteConfirm(null)
                              }
                              className="px-2 py-1 rounded bg-zinc-700 text-zinc-300 text-xs hover:bg-zinc-600 transition-colors"
                            >
                              No
                            </button>
                          </div>
                        ) : (
                          <button
                            onClick={() =>
                              setShowDeleteConfirm(studentId)
                            }
                            title="Delete Student"
                            className="p-1.5 rounded hover:bg-red-500/10 text-zinc-500 hover:text-red-400 transition-colors"
                          >
                            <UserX size={16} />
                          </button>
                        )}
                      </td>
                    </tr>
                  );
                })
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
}

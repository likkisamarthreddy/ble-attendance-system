import { useState } from "react";
import api from "../../lib/api";
import { Upload, Users, UserPlus, CheckCircle, AlertTriangle, User } from "lucide-react";

export default function AdminRegister() {
  const [activeTab, setActiveTab] = useState("bulk"); // "bulk" | "manual"
  const [role, setRole] = useState("student"); // "student" | "professor"
  
  // Bulk State
  const [file, setFile] = useState(null);
  const [bulkLoading, setBulkLoading] = useState(false);
  const [bulkResult, setBulkResult] = useState(null);

  // Manual State
  const [formData, setFormData] = useState({
    name: "",
    email: "",
    password: "",
    rollno: "",
  });
  const [manualLoading, setManualLoading] = useState(false);
  const [manualResult, setManualResult] = useState(null);

  // --- Bulk Handlers ---
  const handleFileUpload = async (e) => {
    e.preventDefault();
    if (!file) {
      alert("Please select a file first");
      return;
    }

    setBulkLoading(true);
    setBulkResult(null);

    const formData = new FormData();
    formData.append("csvfile", file);

    try {
      const res = await api.post("/admin/register/csv", formData, {
        headers: {
          "Content-Type": "multipart/form-data",
          isProfessor: role === "professor",
        },
      });
      setBulkResult({ type: "success", data: res.data });
      setFile(null); // reset file input
    } catch (err) {
      setBulkResult({ type: "error", message: err.response?.data?.message || err.message });
    } finally {
      setBulkLoading(false);
    }
  };

  // --- Manual Handlers ---
  const handleManualSubmit = async (e) => {
    e.preventDefault();
    setManualLoading(true);
    setManualResult(null);

    try {
      const payload = {
        ...formData,
        isProfessor: role === "professor"
      };
      const res = await api.post("/admin/create/student", payload);
      setManualResult({ type: "success", message: `Successfully created ${role} ${formData.name}!` });
      setFormData({ name: "", email: "", password: "", rollno: "" });
    } catch (err) {
      setManualResult({ type: "error", message: err.response?.data?.message || err.message });
    } finally {
      setManualLoading(false);
    }
  };

  const TabButton = ({ value, label, icon: Icon }) => (
    <button
      onClick={() => { setActiveTab(value); setBulkResult(null); setManualResult(null); }}
      className={`flex-1 flex items-center justify-center gap-2 py-3 px-4 text-sm font-medium transition-all border-b-2 ${
        activeTab === value
          ? "border-blue-500 text-blue-400 bg-blue-500/5"
          : "border-transparent text-zinc-500 hover:text-zinc-300 hover:bg-zinc-800/50"
      }`}
    >
      <Icon size={16} />
      {label}
    </button>
  );

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-white flex items-center gap-2">
          <UserPlus className="text-blue-400" size={24} />
          Register Users
        </h1>
        <p className="text-sm text-zinc-400 mt-1">Create new student or professor accounts.</p>
      </div>

      <div className="glass-card overflow-hidden">
        {/* Role Selector (Global for both tabs) */}
        <div className="p-6 border-b border-zinc-800 bg-zinc-900/40">
          <h3 className="text-sm font-medium text-zinc-300 mb-3">1. Select User Role</h3>
          <div className="flex gap-4">
            <button
              onClick={() => setRole("student")}
              className={`flex-1 py-3 px-4 rounded-xl border transition-all ${
                role === "student"
                  ? "border-blue-500 bg-blue-500/10 text-white shadow-md shadow-blue-500/10"
                  : "border-zinc-700 bg-zinc-800 text-zinc-400 hover:bg-zinc-700 hover:text-zinc-200"
              }`}
            >
              <div className="flex items-center justify-center gap-2 font-medium">
                <Users size={18} className={role === "student" ? "text-blue-400" : ""} />
                Student
              </div>
            </button>
            <button
              onClick={() => setRole("professor")}
              className={`flex-1 py-3 px-4 rounded-xl border transition-all ${
                role === "professor"
                  ? "border-indigo-500 bg-indigo-500/10 text-white shadow-md shadow-indigo-500/10"
                  : "border-zinc-700 bg-zinc-800 text-zinc-400 hover:bg-zinc-700 hover:text-zinc-200"
              }`}
            >
              <div className="flex items-center justify-center gap-2 font-medium">
                <User size={18} className={role === "professor" ? "text-indigo-400" : ""} />
                Professor
              </div>
            </button>
          </div>
        </div>

        {/* Tab System */}
        <div className="flex border-b border-zinc-800">
          <TabButton value="bulk" label="Bulk CSV Upload" icon={Upload} />
          <TabButton value="manual" label="Manual Creation" icon={UserPlus} />
        </div>

        {/* Tab Content */}
        <div className="p-6">
          {activeTab === "bulk" ? (
            <div className="animate-fade-in">
              <h3 className="text-lg font-medium text-white mb-4">Upload CSV File</h3>
              <p className="text-sm text-zinc-400 mb-6">
                Upload a CSV containing <code className="text-blue-400 bg-zinc-900 px-1 py-0.5 rounded">name</code>, <code className="text-blue-400 bg-zinc-900 px-1 py-0.5 rounded">email</code>, <code className="text-blue-400 bg-zinc-900 px-1 py-0.5 rounded">password</code>, and <code className="text-blue-400 bg-zinc-900 px-1 py-0.5 rounded">rollno</code> (if student).
              </p>

              <form onSubmit={handleFileUpload} className="space-y-6">
                <div className="border-2 border-dashed border-zinc-700 rounded-2xl p-10 text-center hover:border-blue-500/50 hover:bg-blue-500/5 transition-all group">
                  <input
                    type="file"
                    accept=".csv"
                    onChange={(e) => setFile(e.target.files[0])}
                    className="hidden"
                    id="csv-upload"
                  />
                  <label htmlFor="csv-upload" className="cursor-pointer flex flex-col items-center">
                    <div className="p-4 bg-zinc-800 rounded-full group-hover:bg-blue-500/20 group-hover:text-blue-400 transition-colors mb-3 text-zinc-400">
                      <Upload size={28} />
                    </div>
                    <span className="text-sm font-medium text-zinc-200 mb-1">
                      {file ? file.name : "Click to select CSV file"}
                    </span>
                    <span className="text-xs text-zinc-500">
                      {file ? `${(file.size / 1024).toFixed(1)} KB` : "Excel or raw CSV format"}
                    </span>
                  </label>
                </div>

                <button
                  type="submit"
                  disabled={!file || bulkLoading}
                  className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-500 text-white font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg shadow-blue-500/20 flex items-center justify-center"
                >
                  {bulkLoading ? (
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                  ) : (
                    `Upload and Register ${role === "student" ? "Students" : "Professors"}`
                  )}
                </button>
              </form>

              {/* Bulk Results */}
              {bulkResult && (
                <div className={`mt-6 p-4 rounded-xl border ${bulkResult.type === 'error' ? 'bg-red-500/10 border-red-500/20 text-red-400' : 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'} animate-slide-up`}>
                  {bulkResult.type === 'error' ? (
                    <div className="flex gap-3">
                      <AlertTriangle size={20} className="shrink-0" />
                      <div>
                        <p className="font-semibold text-sm">Upload Failed</p>
                        <p className="text-xs mt-1 opacity-80">{bulkResult.message}</p>
                      </div>
                    </div>
                  ) : (
                    <div>
                      <div className="flex gap-3 mb-3">
                        <CheckCircle size={20} className="shrink-0" />
                        <div>
                          <p className="font-semibold text-sm">{bulkResult.data.message}</p>
                          <p className="text-xs mt-1 opacity-80">Processed {bulkResult.data.total} records.</p>
                        </div>
                      </div>
                      
                      {bulkResult.data.errors?.length > 0 && (
                        <div className="mt-4 border-t border-emerald-500/20 pt-3">
                          <p className="text-xs font-semibold text-amber-500 mb-2">Errors ({bulkResult.data.errors.length}):</p>
                          <ul className="text-xs space-y-1 max-h-32 overflow-y-auto custom-scrollbar">
                            {bulkResult.data.errors.map((err, i) => (
                              <li key={i} className="text-zinc-400"><span className="text-zinc-300 font-mono">{err.email}</span>: {err.error}</li>
                            ))}
                          </ul>
                        </div>
                      )}
                    </div>
                  )}
                </div>
              )}
            </div>
          ) : (
            <div className="animate-fade-in">
              <h3 className="text-lg font-medium text-white mb-6">Create New {role === "student" ? "Student" : "Professor"}</h3>
              
              <form onSubmit={handleManualSubmit} className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  <div>
                    <label className="block text-xs font-medium text-zinc-400 mb-1.5">Full Name</label>
                    <input
                      type="text"
                      required
                      value={formData.name}
                      onChange={e => setFormData({ ...formData, name: e.target.value })}
                      placeholder="e.g. John Doe"
                      className="w-full bg-zinc-900 border border-zinc-800 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-zinc-400 mb-1.5">Email Address</label>
                    <input
                      type="email"
                      required
                      value={formData.email}
                      onChange={e => setFormData({ ...formData, email: e.target.value })}
                      placeholder="e.g. john@example.com"
                      className="w-full bg-zinc-900 border border-zinc-800 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                    />
                  </div>
                  <div>
                    <label className="block text-xs font-medium text-zinc-400 mb-1.5">Temporary Password</label>
                    <input
                      type="text"
                      required
                      value={formData.password}
                      onChange={e => setFormData({ ...formData, password: e.target.value })}
                      placeholder="min 6 characters"
                      className="w-full bg-zinc-900 border border-zinc-800 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                    />
                  </div>
                  {role === "student" && (
                    <div>
                      <label className="block text-xs font-medium text-zinc-400 mb-1.5">Roll Number</label>
                      <input
                        type="number"
                        required
                        value={formData.rollno}
                        onChange={e => setFormData({ ...formData, rollno: e.target.value })}
                        placeholder="e.g. 101"
                        className="w-full bg-zinc-900 border border-zinc-800 text-white px-4 py-2.5 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all"
                      />
                    </div>
                  )}
                </div>

                <div className="pt-4">
                  <button
                    type="submit"
                    disabled={manualLoading}
                    className={`w-full py-3 rounded-xl text-white font-medium transition-all disabled:opacity-50 disabled:cursor-not-allowed shadow-lg flex items-center justify-center ${
                      role === "student" ? "bg-blue-600 hover:bg-blue-500 shadow-blue-500/20" : "bg-indigo-600 hover:bg-indigo-500 shadow-indigo-500/20"
                    }`}
                  >
                    {manualLoading ? (
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin"></div>
                    ) : (
                      `Create ${role === "student" ? "Student" : "Professor"} Account`
                    )}
                  </button>
                </div>
              </form>

              {manualResult && (
                <div className={`mt-6 p-4 rounded-xl border flex items-center gap-3 animate-slide-up ${
                  manualResult.type === 'error' ? 'bg-red-500/10 border-red-500/20 text-red-400' : 'bg-emerald-500/10 border-emerald-500/20 text-emerald-400'
                }`}>
                  {manualResult.type === 'error' ? <AlertTriangle size={20} /> : <CheckCircle size={20} />}
                  <span className="text-sm font-medium">{manualResult.message}</span>
                </div>
              )}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

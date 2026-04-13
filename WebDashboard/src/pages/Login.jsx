import { useEffect, useState } from "react";
import { useNavigate, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { ShieldAlert } from "lucide-react";

export default function Login() {
  const { loginWithGoogle, loginWithEmail, currentUser, userRole, error: authError } = useAuth();
  const navigate = useNavigate();
  const location = useLocation();

  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [localError, setLocalError] = useState("");
  const [isLoading, setIsLoading] = useState(false);

  const displayError = authError || localError;

  useEffect(() => {
    if (currentUser && userRole) {
      const from = location.state?.from?.pathname || (userRole === "ADMIN" ? "/admin" : "/professor");
      navigate(from, { replace: true });
    }
  }, [currentUser, userRole, navigate, location]);

  const handleEmailLogin = async (e) => {
    e.preventDefault();
    setLocalError("");
    setIsLoading(true);
    try {
      await loginWithEmail(email, password);
    } catch (err) {
      setLocalError(err.message.replace("Firebase: ", ""));
    } finally {
      setIsLoading(false);
    }
  };

  return (
    <div className="min-h-screen bg-[#09090b] flex items-center justify-center p-4">
      <div className="max-w-md w-full glass-card p-8">
        <div className="text-center mb-8">
          <div className="w-16 h-16 bg-blue-500/10 rounded-2xl flex items-center justify-center mx-auto mb-4 border border-blue-500/20">
            <ShieldAlert className="text-blue-400" size={32} />
          </div>
          <h1 className="text-3xl font-bold text-white mb-2">Smart Attendance</h1>
          <p className="text-zinc-400">Sign in to manage the system</p>
        </div>

        {displayError && (
          <div className="bg-red-500/10 border border-red-500/20 text-red-400 p-4 rounded-lg mb-6 text-sm">
            {displayError}
          </div>
        )}

        <form onSubmit={handleEmailLogin} className="flex flex-col gap-4 mb-6">
          <input 
            type="email" 
            placeholder="Email address"
            value={email}
            onChange={(e) => setEmail(e.target.value)}
            className="w-full bg-zinc-900 border border-zinc-800 text-white px-4 py-3 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all placeholder:text-zinc-600"
            required
          />
          <input 
            type="password" 
            placeholder="Password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            className="w-full bg-zinc-900 border border-zinc-800 text-white px-4 py-3 rounded-xl focus:border-blue-500 focus:ring-1 focus:ring-blue-500 outline-none transition-all placeholder:text-zinc-600"
            required
          />
          <button 
            type="submit"
            disabled={isLoading}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-semibold py-3 px-4 rounded-xl transition-colors focus:ring-4 focus:ring-blue-500/30 disabled:opacity-50 disabled:cursor-not-allowed"
          >
            {isLoading ? "Signing in..." : "Sign in with Email"}
          </button>
        </form>

        <div className="relative mb-6">
          <div className="absolute inset-0 flex items-center">
            <div className="w-full border-t border-zinc-800"></div>
          </div>
          <div className="relative flex justify-center text-sm">
            <span className="px-2 bg-zinc-900 text-zinc-500 rounded-full">Or continue with</span>
          </div>
        </div>

        <button
          onClick={loginWithGoogle}
          type="button"
          className="w-full bg-white text-zinc-900 font-semibold py-3 px-4 rounded-xl flex items-center justify-center gap-3 hover:bg-zinc-200 transition-colors focus:ring-4 focus:ring-blue-500/30"
        >
          <img 
            src="https://www.gstatic.com/firebasejs/ui/2.0.0/images/auth/google.svg" 
            alt="Google" 
            className="w-5 h-5"
          />
          Sign in with Google
        </button>

        <p className="mt-8 text-center text-xs text-zinc-500">
          Access is restricted to authorized Admins and Professors.
          <br />Student accounts are not permitted on the web dashboard.
        </p>
      </div>
    </div>
  );
}

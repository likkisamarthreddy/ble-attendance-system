import { createContext, useContext, useEffect, useState } from "react";
import { onAuthStateChanged, signInWithPopup, signInWithEmailAndPassword, signOut } from "firebase/auth";
import { auth, googleProvider } from "../firebase";
import api from "../lib/api";

const AuthContext = createContext({});

// eslint-disable-next-line react-refresh/only-export-components
export const useAuth = () => useContext(AuthContext);

export const AuthProvider = ({ children }) => {
  const [currentUser, setCurrentUser] = useState(null);
  const [userRole, setUserRole] = useState(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState("");

  useEffect(() => {
    if (!auth) {
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setLoading(false);
      // eslint-disable-next-line react-hooks/set-state-in-effect
      setError("Firebase configuration missing");
      return;
    }

    const unsubscribe = onAuthStateChanged(auth, async (user) => {
      setLoading(true);
      if (user) {
        setCurrentUser(user);
        try {
          const response = await api.get('/auth', {
            params: { androidId: "web-dashboard" }
          });
          
          const role = response.data.role?.toUpperCase();
          if (role === "STUDENT") {
            setError("Students are not allowed to access the dashboard.");
            await auth.signOut();
            setCurrentUser(null);
            setUserRole(null);
          } else {
            setUserRole(role);
          }
        } catch (err) {
          console.error("Failed to verify role:", err);
          setError("Access denied or account not found.");
          await auth.signOut();
          setCurrentUser(null);
          setUserRole(null);
        }
      } else {
        setCurrentUser(null);
        setUserRole(null);
      }
      setLoading(false);
    });

    return unsubscribe;
  }, []);

  const loginWithGoogle = async () => {
    setError("");
    return signInWithPopup(auth, googleProvider);
  };

  const loginWithEmail = async (email, password) => {
    setError("");
    return signInWithEmailAndPassword(auth, email, password);
  };

  const logout = () => {
    return signOut(auth);
  };

  const value = {
    currentUser,
    userRole,
    loginWithGoogle,
    loginWithEmail,
    logout,
    loading,
    error,
  };

  return <AuthContext.Provider value={value}>{!loading && children}</AuthContext.Provider>;
};

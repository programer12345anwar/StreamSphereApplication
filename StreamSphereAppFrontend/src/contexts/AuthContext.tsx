import React, { createContext, useContext, useEffect, useMemo, useState } from "react";
import { getAuthToken, validateToken } from "@/lib/api";

const AuthContext = createContext({
  user: null,
  login: () => {},
  logout: () => {},
  isAuthenticated: false,
  initializing: true,
});

const USER_KEY = "yt_user";
const TOKEN_KEY = "yt_token";

export function AuthProvider({ children }) {
  const [user, setUser] = useState(null);
  const [initializing, setInitializing] = useState(true);

  useEffect(() => {
    let mounted = true;

    const bootstrapAuth = async () => {
      const storedUser = localStorage.getItem(USER_KEY);
      const token = getAuthToken();

      if (!storedUser || !token) {
        if (mounted) {
          setInitializing(false);
        }
        return;
      }

      try {
        const parsedUser = JSON.parse(storedUser);
        const validation = await validateToken(token);
        if (validation?.success) {
          if (mounted) {
            setUser({ ...parsedUser, token });
          }
        } else {
          localStorage.removeItem(USER_KEY);
          localStorage.removeItem(TOKEN_KEY);
        }
      } catch {
        localStorage.removeItem(USER_KEY);
        localStorage.removeItem(TOKEN_KEY);
      } finally {
        if (mounted) {
          setInitializing(false);
        }
      }
    };

    bootstrapAuth();

    return () => {
      mounted = false;
    };
  }, []);

  const login = (u) => {
    setUser(u);
    localStorage.setItem(USER_KEY, JSON.stringify({ name: u.name, email: u.email }));
    localStorage.setItem(TOKEN_KEY, u.token);
  };

  const logout = () => {
    setUser(null);
    localStorage.removeItem(USER_KEY);
    localStorage.removeItem(TOKEN_KEY);
  };

  const value = useMemo(
    () => ({
      user,
      login,
      logout,
      isAuthenticated: !!user,
      initializing,
    }),
    [user, initializing],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);

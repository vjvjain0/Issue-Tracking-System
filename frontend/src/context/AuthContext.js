import React, { createContext, useContext, useState, useEffect, useRef } from 'react';
import { authAPI, userAPI } from '../services/api';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

const HEARTBEAT_INTERVAL = 30000; // 30 seconds

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [loading, setLoading] = useState(true);
  const heartbeatIntervalRef = useRef(null);

  // Start heartbeat when user is authenticated
  const startHeartbeat = () => {
    // Clear any existing interval
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
    }

    // Send initial heartbeat
    sendHeartbeat();

    // Set up interval for subsequent heartbeats
    heartbeatIntervalRef.current = setInterval(() => {
      sendHeartbeat();
    }, HEARTBEAT_INTERVAL);
  };

  // Stop heartbeat
  const stopHeartbeat = () => {
    if (heartbeatIntervalRef.current) {
      clearInterval(heartbeatIntervalRef.current);
      heartbeatIntervalRef.current = null;
    }
  };

  // Send heartbeat to server
  const sendHeartbeat = async () => {
    try {
      await userAPI.heartbeat();
    } catch (error) {
      // Silently handle heartbeat errors (e.g., if token expired)
      console.debug('Heartbeat failed:', error.message);
    }
  };

  useEffect(() => {
    const storedUser = localStorage.getItem('user');
    const storedToken = localStorage.getItem('token');

    if (storedUser && storedToken) {
      const userData = JSON.parse(storedUser);
      setUser(userData);
      startHeartbeat();
    }
    setLoading(false);

    // Cleanup on unmount
    return () => {
      stopHeartbeat();
    };
  }, []);

  const login = async (email, password) => {
    try {
      const response = await authAPI.login(email, password);
      const { token, ...userData } = response.data;

      localStorage.setItem('token', token);
      localStorage.setItem('user', JSON.stringify(userData));
      setUser(userData);

      // Start heartbeat after successful login
      startHeartbeat();

      return { success: true };
    } catch (error) {
      return {
        success: false,
        error: error.response?.data?.message || 'Login failed',
      };
    }
  };

  const logout = () => {
    // Stop heartbeat before logging out
    stopHeartbeat();

    localStorage.removeItem('token');
    localStorage.removeItem('user');
    setUser(null);
  };

  const isManager = () => user?.role === 'MANAGER';
  const isAgent = () => user?.role === 'AGENT';

  const value = {
    user,
    login,
    logout,
    loading,
    isAuthenticated: !!user,
    isManager,
    isAgent,
  };

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
};

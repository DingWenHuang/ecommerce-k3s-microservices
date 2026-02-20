import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { JSX } from "react";

export function RequireAdmin({ children }: { children: JSX.Element }) {
    const auth = useAuth();
    if (!auth.accessToken) return <Navigate to="/login" replace />;
    if (auth.role !== "ADMIN") return <Navigate to="/products" replace />;
    return children;
}
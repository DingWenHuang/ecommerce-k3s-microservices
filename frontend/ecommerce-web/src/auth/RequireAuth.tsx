import { Navigate } from "react-router-dom";
import { useAuth } from "./AuthContext";
import type { JSX } from "react";

export function RequireAuth({ children }: { children: JSX.Element }) {
    const auth = useAuth();
    if (!auth.accessToken) {
        return <Navigate to="/login" replace />;
    }
    return children;
}
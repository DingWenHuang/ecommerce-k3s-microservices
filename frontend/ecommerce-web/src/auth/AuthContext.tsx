import React, { createContext, useContext, useEffect, useMemo, useReducer } from "react";
import { setAccessToken } from "../api/apiClient";
import { parseJwt } from "./jwt";
import type { AuthState, UserRole } from "./authTypes";

type AuthAction =
    | { type: "LOGIN"; token: string }
    | { type: "LOGOUT" };

const STORAGE_KEY = "ecommerce_access_token";

const initialState: AuthState = {
    accessToken: null,
    role: null,
    userId: null,
};

function deriveStateFromToken(token: string | null): AuthState {
    if (!token) return initialState;

    const payload = parseJwt(token);
    const roleRaw = payload?.roles;
    const role = (roleRaw === "ADMIN" || roleRaw === "USER") ? (roleRaw as UserRole) : null;

    const userId = payload?.userId !== undefined ? String(payload.userId) : null;

    return { accessToken: token, role, userId };
}

function reducer(state: AuthState, action: AuthAction): AuthState {
    switch (action.type) {
        case "LOGIN": {
            const next = deriveStateFromToken(action.token);
            return next;
        }
        case "LOGOUT":
            return initialState;
        default:
            return state;
    }
}

type AuthContextValue = AuthState & {
    login: (token: string) => void;
    logout: () => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
    const [state, dispatch] = useReducer(reducer, initialState);

    // app 初始化：從 localStorage 還原 token
    useEffect(() => {
        const token = localStorage.getItem(STORAGE_KEY);
        if (token) {
            dispatch({ type: "LOGIN", token });
            setAccessToken(token);
        }
    }, []);

    const value = useMemo<AuthContextValue>(() => ({
        ...state,
        login: (token: string) => {
            localStorage.setItem(STORAGE_KEY, token);
            setAccessToken(token);
            dispatch({ type: "LOGIN", token });
        },
        logout: () => {
            localStorage.removeItem(STORAGE_KEY);
            setAccessToken(null);
            dispatch({ type: "LOGOUT" });
        },
    }), [state]);

    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export function useAuth(): AuthContextValue {
    const ctx = useContext(AuthContext);
    if (!ctx) {
        throw new Error("useAuth 必須在 AuthProvider 內使用");
    }
    return ctx;
}
import React from "react";
import { NavBar } from "./NavBar";

export function Layout({ children }: { children: React.ReactNode }) {
    return (
        <div style={{ fontFamily: "system-ui, sans-serif" }}>
            <NavBar />
            <main style={{ maxWidth: 1000, margin: "0 auto", padding: 16 }}>
                {children}
            </main>
        </div>
    );
}
import React from "react";
import ReactDOM from "react-dom/client";
import "antd/dist/reset.css"; // antd v5：重置樣式，避免瀏覽器預設差異
import { App } from "./App";
import { AuthProvider } from "./auth/AuthContext";
import { CartProvider } from "./cart/CartContext";
import "./index.css";

ReactDOM.createRoot(document.getElementById("root")!).render(
    <React.StrictMode>
        <AuthProvider>
            <CartProvider>
                <App />
            </CartProvider>
        </AuthProvider>
    </React.StrictMode>
);
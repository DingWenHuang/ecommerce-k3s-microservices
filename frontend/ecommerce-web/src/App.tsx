import { BrowserRouter, Navigate, Route, Routes } from "react-router-dom";
import { AppLayout } from "./ui/AppLayout";
import { LoginPage } from "./pages/LoginPage";
import { RegisterPage } from "./pages/RegisterPage";
import { ProductsPage } from "./pages/ProductsPage";
import { CartPage } from "./pages/CartPage";
import { OrdersPage } from "./pages/OrdersPage";
import { AdminPage } from "./pages/AdminPage";
import { RequireAuth } from "./auth/RequireAuth";
import { RequireAdmin } from "./auth/RequireAdmin";

export function App() {
    return (
        <BrowserRouter>
            <AppLayout>
                <Routes>
                    <Route path="/" element={<Navigate to="/products" replace />} />
                    <Route path="/login" element={<LoginPage />} />
                    <Route path="/register" element={<RegisterPage />} />

                    <Route path="/products" element={<ProductsPage />} />

                    <Route
                        path="/cart"
                        element={
                            <RequireAuth>
                                <CartPage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/orders"
                        element={
                            <RequireAuth>
                                <OrdersPage />
                            </RequireAuth>
                        }
                    />

                    <Route
                        path="/admin"
                        element={
                            <RequireAdmin>
                                <AdminPage />
                            </RequireAdmin>
                        }
                    />

                    <Route path="*" element={<div>找不到頁面</div>} />
                </Routes>
            </AppLayout>
        </BrowserRouter>
    );
}
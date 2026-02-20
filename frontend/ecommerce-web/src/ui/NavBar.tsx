import { Link, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useCart } from "../cart/CartContext";

export function NavBar() {
    const auth = useAuth();
    const cart = useCart();
    const navigate = useNavigate();

    const cartCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);

    return (
        <header style={{ borderBottom: "1px solid #eee", padding: 12 }}>
            <div style={{ maxWidth: 1000, margin: "0 auto", display: "flex", gap: 12, alignItems: "center" }}>
                <strong style={{ marginRight: 12 }}>ecommerce-k3s</strong>

                <Link to="/products">商品</Link>
                <Link to="/cart">購物車({cartCount})</Link>
                <Link to="/orders">訂單</Link>

                {auth.role === "ADMIN" && (
                    <Link to="/admin" style={{ marginLeft: 12 }}>Admin</Link>
                )}

                <div style={{ marginLeft: "auto", display: "flex", gap: 12, alignItems: "center" }}>
                    {auth.accessToken ? (
                        <>
                            <span style={{ color: "#666" }}>Role: {auth.role ?? "未知"}</span>
                            <button
                                onClick={() => {
                                    auth.logout();
                                    navigate("/login");
                                }}
                            >
                                登出
                            </button>
                        </>
                    ) : (
                        <>
                            <Link to="/login">登入</Link>
                            <Link to="/register">註冊</Link>
                        </>
                    )}
                </div>
            </div>
        </header>
    );
}
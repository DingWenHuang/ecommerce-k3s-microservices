import { useEffect, useState } from "react";
import { fetchProducts, type Product } from "../api/productApi";
import { toErrorMessage } from "../api/apiClient";
import { useCart } from "../cart/CartContext";

export function ProductsPage() {
    const [products, setProducts] = useState<Product[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const cart = useCart();

    useEffect(() => {
        async function load() {
            setError(null);
            setIsLoading(true);
            try {
                const data = await fetchProducts();
                setProducts(data);
            } catch (e) {
                setError(toErrorMessage(e));
            } finally {
                setIsLoading(false);
            }
        }
        load();
    }, []);

    return (
        <div>
            <h2>商品列表</h2>
            {isLoading && <div>載入中...</div>}
            {error && <div style={{ color: "crimson" }}>{error}</div>}

            <div style={{ display: "grid", gap: 12 }}>
                {products.map((p) => (
                    <div key={p.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 12 }}>
                        <div style={{ display: "flex", justifyContent: "space-between" }}>
                            <strong>{p.name}</strong>
                            <span>庫存：{p.stock}</span>
                        </div>
                        <div style={{ marginTop: 6 }}>價格：{p.price.toFixed(2)}</div>

                        <div style={{ marginTop: 10, display: "flex", gap: 8 }}>
                            <button onClick={() => cart.addToCart(p)} disabled={p.stock <= 0}>
                                加入購物車
                            </button>
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
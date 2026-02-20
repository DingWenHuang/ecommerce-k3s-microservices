import { useEffect, useState } from "react";
import { adminCreateProduct, adminRestock, fetchProducts, type Product } from "../api/productApi";
import { toErrorMessage } from "../api/apiClient";

export function AdminPage() {
    const [products, setProducts] = useState<Product[]>([]);
    const [error, setError] = useState<string | null>(null);

    const [newName, setNewName] = useState("New Product");
    const [newPrice, setNewPrice] = useState(1999.99);
    const [newStock, setNewStock] = useState(10);

    async function reload() {
        setError(null);
        try {
            setProducts(await fetchProducts());
        } catch (e) {
            setError(toErrorMessage(e));
        }
    }

    useEffect(() => { reload(); }, []);

    async function create() {
        setError(null);
        try {
            await adminCreateProduct(newName, newPrice, newStock);
            await reload();
        } catch (e) {
            setError(toErrorMessage(e));
        }
    }

    async function restock(productId: number) {
        const amount = Number(prompt("補貨數量", "100"));
        if (!amount || amount <= 0) return;

        setError(null);
        try {
            await adminRestock(productId, amount);
            await reload();
        } catch (e) {
            setError(toErrorMessage(e));
        }
    }

    return (
        <div>
            <h2>Admin 管理</h2>
            {error && <div style={{ color: "crimson" }}>{error}</div>}

            <section style={{ border: "1px solid #eee", borderRadius: 8, padding: 12, marginBottom: 12 }}>
                <h3>新增商品</h3>
                <div style={{ display: "grid", gap: 8, maxWidth: 420 }}>
                    <label>名稱 <input value={newName} onChange={(e) => setNewName(e.target.value)} /></label>
                    <label>價格 <input type="number" value={newPrice} onChange={(e) => setNewPrice(Number(e.target.value))} /></label>
                    <label>庫存 <input type="number" value={newStock} onChange={(e) => setNewStock(Number(e.target.value))} /></label>
                    <button onClick={create}>新增</button>
                </div>
            </section>

            <section>
                <h3>商品列表（補貨）</h3>
                <div style={{ display: "grid", gap: 12 }}>
                    {products.map((p) => (
                        <div key={p.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 12 }}>
                            <div style={{ display: "flex", justifyContent: "space-between" }}>
                                <strong>{p.name}</strong>
                                <span>庫存：{p.stock}</span>
                            </div>
                            <div>價格：{p.price.toFixed(2)}</div>
                            <button style={{ marginTop: 8 }} onClick={() => restock(p.id)}>補貨</button>
                        </div>
                    ))}
                </div>
            </section>
        </div>
    );
}
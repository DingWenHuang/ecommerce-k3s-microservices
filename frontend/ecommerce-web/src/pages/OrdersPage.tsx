import { useEffect, useState } from "react";
import { fetchMyOrders, type Order } from "../api/orderApi";
import { toErrorMessage } from "../api/apiClient";

export function OrdersPage() {
    const [orders, setOrders] = useState<Order[]>([]);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function load() {
            setError(null);
            try {
                const data = await fetchMyOrders();
                setOrders(data);
            } catch (e) {
                setError(toErrorMessage(e));
            }
        }
        load();
    }, []);

    return (
        <div>
            <h2>我的訂單</h2>
            {error && <div style={{ color: "crimson" }}>{error}</div>}

            <div style={{ display: "grid", gap: 12 }}>
                {orders.map((o) => (
                    <div key={o.orderId} style={{ border: "1px solid #eee", borderRadius: 8, padding: 12 }}>
                        <div><strong>Order #{o.orderId}</strong>（{o.status}）</div>
                        <div>總金額：{o.totalAmount.toFixed(2)}</div>
                        <div style={{ marginTop: 8 }}>
                            {o.items.map((i) => (
                                <div key={i.productId}>
                                    商品 {i.productId} × {i.quantity}（單價 {i.unitPrice.toFixed(2)}，小計 {i.lineAmount.toFixed(2)}）
                                </div>
                            ))}
                        </div>
                    </div>
                ))}
            </div>
        </div>
    );
}
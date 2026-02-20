import { useState } from "react";
import { useCart } from "../cart/CartContext";
import { createOrder } from "../api/orderApi";
import { toErrorMessage } from "../api/apiClient";

export function CartPage() {
    const cart = useCart();
    const [error, setError] = useState<string | null>(null);
    const [lastOrderId, setLastOrderId] = useState<number | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    async function checkoutSingleItem() {
        setError(null);
        setLastOrderId(null);

        if (cart.items.length === 0) {
            setError("購物車是空的");
            return;
        }
        if (cart.items.length > 1) {
            setError("Demo 版本先支援單一商品下單（搶購場景）");
            return;
        }

        const item = cart.items[0];
        setIsSubmitting(true);
        try {
            const order = await createOrder(item.product.id, item.quantity, item.product.price);
            setLastOrderId(order.orderId);
            cart.clearCart();
        } catch (e) {
            setError(toErrorMessage(e));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div>
            <h2>購物車</h2>

            {cart.items.length === 0 ? (
                <div>目前沒有商品</div>
            ) : (
                <div style={{ display: "grid", gap: 12 }}>
                    {cart.items.map((item) => (
                        <div key={item.product.id} style={{ border: "1px solid #eee", borderRadius: 8, padding: 12 }}>
                            <div style={{ display: "flex", justifyContent: "space-between" }}>
                                <strong>{item.product.name}</strong>
                                <button onClick={() => cart.removeFromCart(item.product.id)}>移除</button>
                            </div>

                            <div>單價：{item.product.price.toFixed(2)}</div>

                            <label style={{ display: "flex", gap: 8, alignItems: "center", marginTop: 8 }}>
                                數量
                                <input
                                    type="number"
                                    min={1}
                                    value={item.quantity}
                                    onChange={(e) => cart.setQuantity(item.product.id, Number(e.target.value))}
                                    style={{ width: 100 }}
                                />
                            </label>
                        </div>
                    ))}

                    <div style={{ marginTop: 8 }}>
                        <strong>總金額（前端顯示）：{cart.totalAmount.toFixed(2)}</strong>
                    </div>

                    <button onClick={checkoutSingleItem} disabled={isSubmitting}>
                        {isSubmitting ? "下單中..." : "下單（搶購）"}
                    </button>
                </div>
            )}

            {lastOrderId && <div style={{ marginTop: 12, color: "green" }}>下單成功！Order ID: {lastOrderId}</div>}
            {error && <div style={{ marginTop: 12, color: "crimson" }}>{error}</div>}
        </div>
    );
}
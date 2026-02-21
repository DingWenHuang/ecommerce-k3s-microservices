import { useState } from "react";
import { Alert, Button, Card, InputNumber, Space, Table, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { createNormalOrder } from "../api/orderApi";
import { toErrorMessage } from "../api/apiClient";
import { useCart } from "../cart/CartContext";

const { Title, Text } = Typography;

type CartRow = {
    key: number;
    productId: number;
    name: string;
    unitPrice: number;
    quantity: number;
    lineAmount: number;
};

export function CartPage() {
    const cart = useCart();
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const rows: CartRow[] = cart.items.map((item) => ({
        key: item.product.id,
        productId: item.product.id,
        name: item.product.name,
        unitPrice: item.product.price,
        quantity: item.quantity,
        lineAmount: item.product.price * item.quantity,
    }));

    const columns: ColumnsType<CartRow> = [
        { title: "商品", dataIndex: "name" },
        { title: "單價", dataIndex: "unitPrice", render: (v: number) => v.toFixed(2) },
        {
            title: "數量",
            dataIndex: "quantity",
            render: (_, row) => (
                <InputNumber
                    min={1}
                    value={row.quantity}
                    onChange={(value) => cart.setQuantity(row.productId, Number(value ?? 1))}
                />
            ),
        },
        { title: "小計", dataIndex: "lineAmount", render: (v: number) => v.toFixed(2) },
        {
            title: "操作",
            render: (_, row) => (
                <Button danger onClick={() => cart.removeFromCart(row.productId)}>移除</Button>
            ),
        },
    ];

    async function checkout() {
        setError(null);

        if (cart.items.length === 0) {
            setError("購物車是空的");
            return;
        }
        const request = {
            items: cart.items.map((item) => ({
                productId: item.product.id,
                quantity: item.quantity,
            })),
        };

        setIsSubmitting(true);
        try {
            const order = await createNormalOrder(request);
            cart.clearCart();
            message.success(`下單成功！Order #${order.orderId}，共 ${order.items.length} 項`);
        } catch (e) {
            setError(toErrorMessage(e));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div>
            <Title level={3} style={{ marginTop: 0 }}>購物車</Title>
            <Text type="secondary">展示：一般商品支援多品項結帳（NORMAL）</Text>

            <div style={{ height: 16 }} />

            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}

            <Card>
                <Table columns={columns} dataSource={rows} pagination={false} />

                <div style={{ display: "flex", justifyContent: "space-between", marginTop: 12 }}>
                    <div />
                    <Space>
                        <strong>總金額（前端顯示）：{cart.totalAmount.toFixed(2)}</strong>
                        <Button type="primary" onClick={checkout} loading={isSubmitting}>
                            多品項結帳
                        </Button>
                    </Space>
                </div>
            </Card>
        </div>
    );
}
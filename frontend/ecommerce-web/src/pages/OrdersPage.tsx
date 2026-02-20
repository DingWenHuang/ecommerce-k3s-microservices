import { useEffect, useState } from "react";
import { Alert, Card, Table, Typography } from "antd";
import type { ColumnsType } from "antd/es/table";
import { fetchMyOrders, type Order } from "../api/orderApi";
import { toErrorMessage } from "../api/apiClient";

const { Title, Text } = Typography;

type OrderRow = {
    key: number;
    orderId: number;
    status: string;
    totalAmount: number;
    itemsText: string;
};

export function OrdersPage() {
    const [orders, setOrders] = useState<Order[]>([]);
    const [error, setError] = useState<string | null>(null);

    useEffect(() => {
        async function load() {
            setError(null);
            try {
                setOrders(await fetchMyOrders());
            } catch (e) {
                setError(toErrorMessage(e));
            }
        }
        load();
    }, []);

    const rows: OrderRow[] = orders.map((o) => ({
        key: o.orderId,
        orderId: o.orderId,
        status: o.status,
        totalAmount: o.totalAmount,
        itemsText: o.items
            .map((i) => `商品${i.productId}×${i.quantity}（小計${i.lineAmount.toFixed(2)}）`)
            .join(" / "),
    }));

    const columns: ColumnsType<OrderRow> = [
        { title: "訂單編號", dataIndex: "orderId" },
        { title: "狀態", dataIndex: "status" },
        { title: "總金額", dataIndex: "totalAmount", render: (v: number) => v.toFixed(2) },
        { title: "明細", dataIndex: "itemsText" },
    ];

    return (
        <div>
            <Title level={3} style={{ marginTop: 0 }}>我的訂單</Title>
            <Text type="secondary">展示：呼叫 /orders 查詢（需登入）</Text>

            <div style={{ height: 16 }} />

            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}

            <Card>
                <Table columns={columns} dataSource={rows} />
            </Card>
        </div>
    );
}
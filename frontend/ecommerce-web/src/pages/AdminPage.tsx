import { useEffect, useState } from "react";
import { Alert, Button, Card, Form, Input, InputNumber, Modal, Select, Space, Table, Typography, message } from "antd";
import type { ColumnsType } from "antd/es/table";
import { adminCreateProduct, adminRestock, fetchProductsByType, type Product, type ProductType } from "../api/productApi";
import { toErrorMessage } from "../api/apiClient";

const { Title, Text } = Typography;

type ProductRow = Product & { key: number };

export function AdminPage() {
    const [normalProducts, setNormalProducts] = useState<Product[]>([]);
    const [flashProducts, setFlashProducts] = useState<Product[]>([]);
    const [error, setError] = useState<string | null>(null);

    const [isRestockModalOpen, setIsRestockModalOpen] = useState(false);
    const [restockProductId, setRestockProductId] = useState<number | null>(null);
    const [restockAmount, setRestockAmount] = useState<number>(100);

    async function reload() {
        setError(null);
        try {
            const [normal, flash] = await Promise.all([
                fetchProductsByType("NORMAL"),
                fetchProductsByType("FLASH_SALE"),
            ]);
            setNormalProducts(normal);
            setFlashProducts(flash);
        } catch (e) {
            setError(toErrorMessage(e));
        }
    }

    useEffect(() => { reload(); }, []);

    async function onCreate(values: { name: string; price: number; stock: number; productType: ProductType }) {
        setError(null);
        try {
            await adminCreateProduct(values.name, values.price, values.stock, values.productType);
            message.success("新增商品成功");
            await reload();
        } catch (e) {
            setError(toErrorMessage(e));
        }
    }

    function openRestock(productId: number) {
        setRestockProductId(productId);
        setRestockAmount(100);
        setIsRestockModalOpen(true);
    }

    async function confirmRestock() {
        if (!restockProductId) return;

        setError(null);
        try {
            await adminRestock(restockProductId, restockAmount);
            message.success("補貨成功");
            setIsRestockModalOpen(false);
            await reload();
        } catch (e) {
            setError(toErrorMessage(e));
        }
    }

    const columns: ColumnsType<ProductRow> = [
        { title: "ID", dataIndex: "id", width: 80 },
        { title: "名稱", dataIndex: "name" },
        { title: "類型", dataIndex: "productType", width: 140 },
        { title: "價格", dataIndex: "price", render: (v: number) => v.toFixed(2) },
        { title: "庫存", dataIndex: "stock" },
        {
            title: "操作",
            width: 120,
            render: (_, row) => (
                <Button type="primary" onClick={() => openRestock(row.id)}>
                    補貨
                </Button>
            ),
        },
    ];

    return (
        <div>
            <Title level={3} style={{ marginTop: 0 }}>Admin 管理</Title>
            <Text type="secondary">新增商品時可指定 NORMAL / FLASH_SALE</Text>

            <div style={{ height: 16 }} />

            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}

            <Card style={{ marginBottom: 12 }}>
                <Title level={5} style={{ marginTop: 0 }}>新增商品</Title>

                <Form
                    layout="inline"
                    onFinish={onCreate}
                    initialValues={{ name: "New Product", price: 1999.99, stock: 10, productType: "NORMAL" }}
                >
                    <Form.Item name="name" rules={[{ required: true, message: "請輸入名稱" }]}>
                        <Input placeholder="商品名稱" style={{ width: 220 }} />
                    </Form.Item>

                    <Form.Item name="productType" rules={[{ required: true, message: "請選擇商品類型" }]}>
                        <Select style={{ width: 200 }}
                                options={[
                                    { value: "NORMAL", label: "NORMAL（一般）" },
                                    { value: "FLASH_SALE", label: "FLASH_SALE（搶購）" },
                                ]}
                        />
                    </Form.Item>

                    <Form.Item name="price" rules={[{ required: true, message: "請輸入價格" }]}>
                        <InputNumber min={0} step={0.01} style={{ width: 140 }} />
                    </Form.Item>

                    <Form.Item name="stock" rules={[{ required: true, message: "請輸入庫存" }]}>
                        <InputNumber min={0} step={1} style={{ width: 140 }} />
                    </Form.Item>

                    <Form.Item>
                        <Button type="primary" htmlType="submit">新增</Button>
                    </Form.Item>
                </Form>
            </Card>

            <Card>
                <Space style={{ width: "100%", justifyContent: "space-between" }}>
                    <Title level={5} style={{ margin: 0 }}>商品列表</Title>
                    <Button onClick={reload}>重新整理</Button>
                </Space>

                <div style={{ height: 12 }} />

                <Title level={5}>一般商品（NORMAL）</Title>
                <Table columns={columns} dataSource={normalProducts.map((p) => ({ ...p, key: p.id }))} pagination={false} />

                <div style={{ height: 16 }} />

                <Title level={5}>搶購商品（FLASH_SALE）</Title>
                <Table columns={columns} dataSource={flashProducts.map((p) => ({ ...p, key: p.id }))} />
            </Card>

            <Modal
                title="補貨"
                open={isRestockModalOpen}
                onOk={confirmRestock}
                onCancel={() => setIsRestockModalOpen(false)}
                okText="確認"
                cancelText="取消"
            >
                <div style={{ display: "grid", gap: 8 }}>
                    <div>商品 ID：{restockProductId}</div>
                    <div>
                        補貨數量：
                        <InputNumber min={1} value={restockAmount} onChange={(v) => setRestockAmount(Number(v ?? 1))} />
                    </div>
                </div>
            </Modal>
        </div>
    );
}
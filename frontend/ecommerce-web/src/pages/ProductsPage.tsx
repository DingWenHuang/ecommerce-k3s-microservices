import { useEffect, useState } from "react";
import { Alert, Button, Card, Col, Row, Skeleton, Statistic, Typography } from "antd";
import { ShoppingCartOutlined } from "@ant-design/icons";
import { fetchProducts, type Product } from "../api/productApi";
import { toErrorMessage } from "../api/apiClient";
import { useCart } from "../cart/CartContext";

const { Title, Text } = Typography;

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
                setProducts(await fetchProducts());
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
            <Title level={3} style={{ marginTop: 0 }}>商品列表</Title>
            <Text type="secondary">展示：商品查詢（公開 API）＋ 加入購物車（前端狀態）</Text>

            <div style={{ height: 16 }} />

            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}

            {isLoading ? (
                <Skeleton active />
            ) : (
                <Row gutter={[12, 12]}>
                    {products.map((p) => (
                        <Col key={p.id} xs={24} sm={12} md={8}>
                            <Card
                                title={p.name}
                                actions={[
                                    <Button
                                        key="add"
                                        type="primary"
                                        icon={<ShoppingCartOutlined />}
                                        onClick={() => cart.addToCart(p)}
                                        disabled={p.stock <= 0}
                                    >
                                        加入購物車
                                    </Button>,
                                ]}
                            >
                                <Row gutter={12}>
                                    <Col span={12}>
                                        <Statistic title="價格" value={p.price} precision={2} />
                                    </Col>
                                    <Col span={12}>
                                        <Statistic title="庫存" value={p.stock} />
                                    </Col>
                                </Row>
                            </Card>
                        </Col>
                    ))}
                </Row>
            )}
        </div>
    );
}
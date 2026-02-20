import { useEffect, useState } from "react";
import { Alert, Button, Card, Col, Divider, Row, Skeleton, Statistic, Tag, Typography } from "antd";
import { ShoppingCartOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { fetchProductsByType, type Product } from "../api/productApi";
import { toErrorMessage } from "../api/apiClient";
import { useCart } from "../cart/CartContext";
import { useAuth } from "../auth/AuthContext";

const { Title, Text } = Typography;

/**
 * 商品頁：
 * - 一般商品（NORMAL）：可加入購物車（Step 15 會支援多品項結帳）
 * - 搶購商品（FLASH_SALE）：先做 UI 區隔；Step 16 才做排隊 join / 狀態輪詢
 */
export function ProductsPage() {
    const [normalProducts, setNormalProducts] = useState<Product[]>([]);
    const [flashSaleProducts, setFlashSaleProducts] = useState<Product[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    const cart = useCart();
    const auth = useAuth();

    useEffect(() => {
        async function load() {
            setError(null);
            setIsLoading(true);
            try {
                const [normal, flash] = await Promise.all([
                    fetchProductsByType("NORMAL"),
                    fetchProductsByType("FLASH_SALE"),
                ]);
                setNormalProducts(normal);
                setFlashSaleProducts(flash);
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
            <Title level={3} style={{ marginTop: 0 }}>商品</Title>
            <Text type="secondary">
                一般商品支援多品項結帳（下一步會完成）；搶購商品將走排隊與限購流程（下一步會完成）。
            </Text>

            <div style={{ height: 16 }} />

            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}
            {isLoading && <Skeleton active />}

            {!isLoading && (
                <>
                    <SectionTitle icon={<ShoppingCartOutlined />} title="一般商品區" subtitle="可加入購物車，支援多品項結帳" />
                    <Row gutter={[12, 12]}>
                        {normalProducts.map((p) => (
                            <Col key={p.id} xs={24} sm={12} md={8}>
                                <Card
                                    title={<span>{p.name} <Tag color="blue">NORMAL</Tag></span>}
                                    actions={[
                                        <Button
                                            key="add"
                                            type="primary"
                                            icon={<ShoppingCartOutlined />}
                                            onClick={() => cart.addToCart(p)}
                                            disabled={!auth.accessToken || p.stock <= 0}
                                        >
                                            加入購物車
                                        </Button>,
                                    ]}
                                >
                                    <Row gutter={12}>
                                        <Col span={12}><Statistic title="價格" value={p.price} precision={2} /></Col>
                                        <Col span={12}><Statistic title="庫存" value={p.stock} /></Col>
                                    </Row>
                                </Card>
                            </Col>
                        ))}
                    </Row>

                    <Divider style={{ margin: "24px 0" }} />

                    <SectionTitle icon={<ThunderboltOutlined />} title="限量搶購區" subtitle="一次只能買一種且限購 1（下一步加入排隊購買）" />
                    <Row gutter={[12, 12]}>
                        {flashSaleProducts.map((p) => (
                            <Col key={p.id} xs={24} sm={12} md={8}>
                                <Card
                                    title={<span>{p.name} <Tag color="red">FLASH_SALE</Tag></span>}
                                    actions={[
                                        <Button
                                            key="flash"
                                            type="primary"
                                            danger
                                            icon={<ThunderboltOutlined />}
                                            disabled={!auth.accessToken || p.stock <= 0}
                                            onClick={() => {
                                                // Step 16 會改成 join queue + 顯示排隊狀態
                                                alert("下一步（Step 16）會新增：排隊搶購 join + status。現在先完成商品分區。");
                                            }}
                                        >
                                            進入搶購
                                        </Button>,
                                    ]}
                                >
                                    <Row gutter={12}>
                                        <Col span={12}><Statistic title="價格" value={p.price} precision={2} /></Col>
                                        <Col span={12}><Statistic title="庫存" value={p.stock} /></Col>
                                    </Row>
                                    {!auth.accessToken && <div style={{ marginTop: 8, color: "#888" }}>需先登入才可搶購</div>}
                                </Card>
                            </Col>
                        ))}
                    </Row>
                </>
            )}
        </div>
    );
}

function SectionTitle(props: { icon: React.ReactNode; title: string; subtitle: string }) {
    return (
        <div style={{ display: "flex", alignItems: "baseline", gap: 8, marginBottom: 12 }}>
            <span>{props.icon}</span>
            <Title level={4} style={{ margin: 0 }}>{props.title}</Title>
            <Text type="secondary">{props.subtitle}</Text>
        </div>
    );
}
import { useEffect, useState } from "react";
import { Alert, Button, Card, Col, Divider, Modal, Row, Skeleton, Statistic, Tag, Typography } from "antd";
import { ShoppingCartOutlined, ThunderboltOutlined } from "@ant-design/icons";
import { fetchProductsByType, type Product } from "../api/productApi";
import { joinFlashSale, getFlashSaleTicketStatus } from "../api/flashSaleApi";
import { toErrorMessage } from "../api/apiClient";
import { useCart } from "../cart/CartContext";
import { useAuth } from "../auth/AuthContext";

const { Title, Text } = Typography;

/**
 * 商品頁：
 * - NORMAL：加入購物車 → 多品項結帳
 * - FLASH_SALE：join queue → polling status → success 顯示 orderId
 */
export function ProductsPage() {
    const [normalProducts, setNormalProducts] = useState<Product[]>([]);
    const [flashSaleProducts, setFlashSaleProducts] = useState<Product[]>([]);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(true);

    // 搶購狀態 UI
    const [isFlashModalOpen, setIsFlashModalOpen] = useState(false);
    const [flashTicketId, setFlashTicketId] = useState<string | null>(null);
    const [flashStatusText, setFlashStatusText] = useState<string>("尚未加入隊列");
    const [flashOrderId, setFlashOrderId] = useState<number | null>(null);
    const [flashPosition, setFlashPosition] = useState<number | null>(null);
    const [flashSelectedProduct, setFlashSelectedProduct] = useState<Product | null>(null);

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

    async function reloadFlashSaleProducts() {
        try {
            const flash = await fetchProductsByType("FLASH_SALE");
            setFlashSaleProducts(flash);

            if (flashSelectedProduct) {
                const updated = flash.find((x) => x.id === flashSelectedProduct.id);
                if (updated) setFlashSelectedProduct(updated);
            }
        } catch (e) {
            // 不要蓋掉成功訊息，只記錄在 console
            console.error(e);
        }
    }

    // polling：每 1 秒查一次狀態，視同心跳（刷新 TTL）
    useEffect(() => {
        if (!flashTicketId) return;

        let timerId: number | null = null;
        let stopped = false;

        async function poll() {
            if (stopped) return;
            try {
                const status = await getFlashSaleTicketStatus(flashTicketId as string);
                setFlashPosition(status.position);
                setFlashOrderId(status.orderId);

                if (status.status === "QUEUED") setFlashStatusText(`排隊中... 目前順位：${status.position ?? "?"}`);
                if (status.status === "PROCESSING") setFlashStatusText("處理中，請稍候...");
                if (status.status === "SUCCESS") setFlashStatusText(`✅ 搶購成功！Order #${status.orderId}`);
                if (status.status === "SOLD_OUT") setFlashStatusText("❌ 已售罄");
                if (status.status === "EXPIRED") setFlashStatusText("⏳ 已離隊（可能離線太久），請重新加入");

                // 成功/售完/過期就停止輪詢
                if (status.status === "SUCCESS" || status.status === "SOLD_OUT" || status.status === "EXPIRED") {
                    await reloadFlashSaleProducts(); // 刷新商品更新庫存
                    return;
                }

                timerId = window.setTimeout(poll, 1000);
            } catch (e) {
                setFlashStatusText(`查詢狀態失敗：${toErrorMessage(e)}`);
                timerId = window.setTimeout(poll, 1500);
            }
        }

        poll();

        return () => {
            stopped = true;
            if (timerId) window.clearTimeout(timerId);
        };
    }, [flashTicketId]);

    async function startFlashSale(product: Product) {
        setFlashSelectedProduct(product);
        setIsFlashModalOpen(true);
        setFlashTicketId(null);
        setFlashOrderId(null);
        setFlashPosition(null);
        setFlashStatusText("準備加入隊列...");

        try {
            const join = await joinFlashSale(product.id);
            setFlashTicketId(join.ticketId);
            setFlashStatusText("已加入隊列，正在取得狀態...");
        } catch (e) {
            setFlashStatusText(`加入隊列失敗：${toErrorMessage(e)}`);
        }
    }

    return (
        <div>
            <Title level={3} style={{ marginTop: 0 }}>商品</Title>
            <Text type="secondary">
                一般商品支援多品項結帳；搶購商品採 FIFO 排隊 + 斷線離隊（輪詢=心跳）。
            </Text>

            <div style={{ height: 16 }} />
            {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}
            {isLoading && <Skeleton active />}

            {!isLoading && (
                <>
                    <SectionTitle icon={<ShoppingCartOutlined />} title="一般商品區" subtitle="可加入購物車，多品項結帳" />
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

                    <SectionTitle icon={<ThunderboltOutlined />} title="限量搶購區" subtitle="單品項＋單件（排隊處理）" />
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
                                            onClick={() => startFlashSale(p)}
                                        >
                                            排隊搶購
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

            <Modal
                title={`搶購排隊：${flashSelectedProduct?.name ?? ""}`}
                open={isFlashModalOpen}
                onCancel={() => setIsFlashModalOpen(false)}
                footer={null}
            >
                <div style={{ display: "grid", gap: 8 }}>
                    <div><strong>狀態：</strong>{flashStatusText}</div>
                    {flashTicketId && <div><strong>Ticket：</strong>{flashTicketId}</div>}
                    {flashPosition && <div><strong>順位：</strong>{flashPosition}</div>}
                    {flashOrderId && <div><strong>OrderId：</strong>{flashOrderId}</div>}

                    <div style={{ color: "#888" }}>
                        提示：保持此視窗開著會持續輪詢（視同心跳）。關閉或離線超過 TTL 會自動離隊。
                    </div>
                </div>
            </Modal>
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
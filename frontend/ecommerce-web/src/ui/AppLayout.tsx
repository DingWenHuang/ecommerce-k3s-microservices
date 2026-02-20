import { Badge, Button, Layout, Menu, Tag, Typography } from "antd";
import {
    AppstoreOutlined,
    ShoppingCartOutlined,
    ProfileOutlined,
    SettingOutlined,
    LoginOutlined,
    LogoutOutlined,
    UserAddOutlined,
} from "@ant-design/icons";
import { Link, useLocation, useNavigate } from "react-router-dom";
import { useAuth } from "../auth/AuthContext";
import { useCart } from "../cart/CartContext";

const { Header, Sider, Content } = Layout;
const { Title } = Typography;

/**
 * AppLayout：整個網站的 UI 骨架
 * - Header：品牌、角色、登入/登出
 * - Sider：功能選單（依角色顯示 Admin）
 * - Content：頁面內容
 */
export function AppLayout({ children }: { children: React.ReactNode }) {
    const auth = useAuth();
    const cart = useCart();
    const location = useLocation();
    const navigate = useNavigate();

    const cartCount = cart.items.reduce((sum, item) => sum + item.quantity, 0);

    // 將 path 對應到 menu key（簡化選中狀態）
    const selectedKey = (() => {
        if (location.pathname.startsWith("/products")) return "products";
        if (location.pathname.startsWith("/cart")) return "cart";
        if (location.pathname.startsWith("/orders")) return "orders";
        if (location.pathname.startsWith("/admin")) return "admin";
        if (location.pathname.startsWith("/login")) return "login";
        if (location.pathname.startsWith("/register")) return "register";
        return "products";
    })();

    const roleTag = auth.role === "ADMIN"
        ? <Tag color="red">ADMIN</Tag>
        : auth.role === "USER"
            ? <Tag color="blue">USER</Tag>
            : <Tag>UNKNOWN</Tag>;

    const isLoggedIn = !!auth.accessToken;
    const isAdmin = auth.role === "ADMIN";

    const menuItems = [
        {
            key: "products",
            icon: <AppstoreOutlined />,
            label: <Link to="/products">商品</Link>,
        },

        // 只有登入後才顯示
        ...(isLoggedIn
            ? [
                {
                    key: "cart",
                    icon: (
                        <Badge count={cartCount} size="small">
                            <ShoppingCartOutlined />
                        </Badge>
                    ),
                    label: <Link to="/cart">購物車</Link>,
                },
                {
                    key: "orders",
                    icon: <ProfileOutlined />,
                    label: <Link to="/orders">訂單</Link>,
                },
            ]
            : []),

        // 只有 ADMIN 才顯示
        ...(isAdmin
            ? [
                {
                    key: "admin",
                    icon: <SettingOutlined />,
                    label: <Link to="/admin">Admin</Link>,
                },
            ]
            : []),

        // 未登入才顯示
        ...(!isLoggedIn
            ? [
                {
                    key: "login",
                    icon: <LoginOutlined />,
                    label: <Link to="/login">登入</Link>,
                },
                {
                    key: "register",
                    icon: <UserAddOutlined />,
                    label: <Link to="/register">註冊</Link>,
                },
            ]
            : []),
    ];

    return (
        <Layout style={{ minHeight: "100vh" }}>
            <Sider breakpoint="lg" collapsedWidth={64}>
                <div style={{ height: 48, display: "flex", alignItems: "center", paddingLeft: 16 }}>
                    <Title level={5} style={{ color: "white", margin: 0 }}>ecommerce</Title>
                </div>
                <Menu theme="dark" mode="inline" selectedKeys={[selectedKey]} items={menuItems} />
            </Sider>

            <Layout>
                <Header style={{ background: "white", display: "flex", alignItems: "center", gap: 12 }}>
                    <strong>Microservices Demo</strong>
                    <span style={{ color: "#888" }}>（k3s / gateway / redis sentinel / spring security）</span>

                    <div style={{ marginLeft: "auto", display: "flex", alignItems: "center", gap: 12 }}>
                        {auth.accessToken && (
                            <>
                                {roleTag}
                                <Button
                                    icon={<LogoutOutlined />}
                                    onClick={() => {
                                        auth.logout();
                                        navigate("/login");
                                    }}
                                >
                                    登出
                                </Button>
                            </>
                        )}

                        {!auth.accessToken && (
                            <Button icon={<LoginOutlined />} onClick={() => navigate("/login")}>
                                登入
                            </Button>
                        )}
                    </div>
                </Header>

                <Content style={{ padding: 16 }}>
                    <div style={{ maxWidth: 1100, margin: "0 auto" }}>
                        {children}
                    </div>
                </Content>
            </Layout>
        </Layout>
    );
}
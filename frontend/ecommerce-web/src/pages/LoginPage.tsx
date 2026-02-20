import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Button, Card, Form, Input, Typography } from "antd";
import { loginUser } from "../api/authApi";
import { toErrorMessage } from "../api/apiClient";
import { useAuth } from "../auth/AuthContext";

const { Title, Text } = Typography;

export function LoginPage() {
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const auth = useAuth();
    const navigate = useNavigate();

    async function onFinish(values: { username: string; password: string }) {
        setError(null);
        setIsSubmitting(true);
        try {
            const result = await loginUser(values.username, values.password);
            auth.login(result.accessToken);
            navigate("/products");
        } catch (e) {
            setError(toErrorMessage(e));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div style={{ display: "grid", placeItems: "center", paddingTop: 48 }}>
            <Card style={{ width: 420 }}>
                <Title level={3} style={{ marginTop: 0 }}>登入</Title>
                <Text type="secondary">使用 USER/ADMIN 身分登入以展示角色差異。</Text>

                <div style={{ height: 16 }} />

                {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}

                <Form layout="vertical" onFinish={onFinish} initialValues={{ username: "bob", password: "Passw0rd!" }}>
                    <Form.Item label="帳號" name="username" rules={[{ required: true, message: "請輸入帳號" }]}>
                        <Input autoComplete="username" />
                    </Form.Item>

                    <Form.Item label="密碼" name="password" rules={[{ required: true, message: "請輸入密碼" }]}>
                        <Input.Password autoComplete="current-password" />
                    </Form.Item>

                    <Button type="primary" htmlType="submit" loading={isSubmitting} block>
                        登入
                    </Button>
                </Form>
            </Card>
        </div>
    );
}
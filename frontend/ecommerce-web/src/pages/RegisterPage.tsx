import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { Alert, Button, Card, Form, Input, Typography } from "antd";
import { registerUser } from "../api/authApi";
import { toErrorMessage } from "../api/apiClient";

const { Title, Text } = Typography;

export function RegisterPage() {
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);
    const navigate = useNavigate();

    async function onFinish(values: { username: string; password: string }) {
        setError(null);
        setIsSubmitting(true);
        try {
            await registerUser(values.username, values.password);
            navigate("/login");
        } catch (e) {
            setError(toErrorMessage(e));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div style={{ display: "grid", placeItems: "center", paddingTop: 48 }}>
            <Card style={{ width: 420 }}>
                <Title level={3} style={{ marginTop: 0 }}>註冊</Title>
                <Text type="secondary">註冊後預設為 USER。若要做 ADMIN demo，可在 DB 內調整角色。</Text>

                <div style={{ height: 16 }} />

                {error && <Alert type="error" message={error} showIcon style={{ marginBottom: 12 }} />}

                <Form layout="vertical" onFinish={onFinish}>
                    <Form.Item label="帳號" name="username" rules={[{ required: true, message: "請輸入帳號" }]}>
                        <Input placeholder="例如：bob" autoComplete="username" />
                    </Form.Item>

                    <Form.Item label="密碼" name="password" rules={[{ required: true, message: "請輸入密碼" }]}>
                        <Input.Password autoComplete="new-password" />
                    </Form.Item>

                    <Button type="primary" htmlType="submit" loading={isSubmitting} block>
                        註冊
                    </Button>
                </Form>
            </Card>
        </div>
    );
}
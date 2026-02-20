import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { loginUser } from "../api/authApi";
import { toErrorMessage } from "../api/apiClient";
import { useAuth } from "../auth/AuthContext";

export function LoginPage() {
    const [username, setUsername] = useState("bob");
    const [password, setPassword] = useState("Passw0rd!");
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const auth = useAuth();
    const navigate = useNavigate();

    async function onSubmit() {
        setError(null);
        setIsSubmitting(true);
        try {
            const result = await loginUser(username, password);
            auth.login(result.accessToken);
            navigate("/products");
        } catch (e) {
            setError(toErrorMessage(e));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div>
            <h2>登入</h2>
            <div style={{ display: "grid", gap: 8, maxWidth: 360 }}>
                <label>
                    帳號
                    <input value={username} onChange={(e) => setUsername(e.target.value)} />
                </label>
                <label>
                    密碼
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
                </label>

                <button onClick={onSubmit} disabled={isSubmitting}>
                    {isSubmitting ? "登入中..." : "登入"}
                </button>

                {error && <div style={{ color: "crimson" }}>{error}</div>}
            </div>
        </div>
    );
}
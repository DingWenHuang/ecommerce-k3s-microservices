import { useState } from "react";
import { useNavigate } from "react-router-dom";
import { registerUser } from "../api/authApi";
import { toErrorMessage } from "../api/apiClient";

export function RegisterPage() {
    const [username, setUsername] = useState("");
    const [password, setPassword] = useState("Passw0rd!");
    const [error, setError] = useState<string | null>(null);
    const [isSubmitting, setIsSubmitting] = useState(false);

    const navigate = useNavigate();

    async function onSubmit() {
        setError(null);
        setIsSubmitting(true);
        try {
            await registerUser(username, password);
            navigate("/login");
        } catch (e) {
            setError(toErrorMessage(e));
        } finally {
            setIsSubmitting(false);
        }
    }

    return (
        <div>
            <h2>註冊</h2>
            <div style={{ display: "grid", gap: 8, maxWidth: 360 }}>
                <label>
                    帳號
                    <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="例如: bob" />
                </label>
                <label>
                    密碼
                    <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
                </label>

                <button onClick={onSubmit} disabled={isSubmitting || !username}>
                    {isSubmitting ? "註冊中..." : "註冊"}
                </button>

                {error && <div style={{ color: "crimson" }}>{error}</div>}
            </div>
        </div>
    );
}
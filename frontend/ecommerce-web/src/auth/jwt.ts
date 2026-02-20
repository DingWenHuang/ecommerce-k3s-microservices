/**
 * JWT 工具：
 * - 解析 payload（不驗簽，只用於前端顯示/判斷角色）
 * - 注意：權限最終仍以後端為準，前端只是 UI 控制
 */

export type JwtPayload = {
    sub?: string;
    userId?: number | string;
    roles?: string; // gateway 目前傳的是單一 role（USER/ADMIN）
    exp?: number;
    iat?: number;
};

function base64UrlToJson(base64Url: string): unknown {
    const base64 = base64Url.replace(/-/g, "+").replace(/_/g, "/");
    const padded = base64.padEnd(base64.length + ((4 - (base64.length % 4)) % 4), "=");
    const decoded = atob(padded);
    return JSON.parse(decoded);
}

export function parseJwt(token: string): JwtPayload | null {
    const parts = token.split(".");
    if (parts.length !== 3) return null;
    try {
        return base64UrlToJson(parts[1]) as JwtPayload;
    } catch {
        return null;
    }
}
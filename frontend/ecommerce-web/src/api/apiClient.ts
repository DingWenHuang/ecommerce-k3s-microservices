import axios, { AxiosError } from "axios";

/**
 * 統一的 API Client：
 * - 自動帶入 baseURL
 * - 統一在 request 時加入 Authorization header（若有 token）
 * - 統一處理錯誤（轉成可讀訊息）
 */

const baseURL = import.meta.env.VITE_API_BASE_URL;

if (!baseURL) {
    throw new Error("VITE_API_BASE_URL 未設定");
}

export const apiClient = axios.create({
    baseURL,
    timeout: 15000,
});

export function setAccessToken(token: string | null): void {
    if (token) {
        apiClient.defaults.headers.common.Authorization = `Bearer ${token}`;
    } else {
        delete apiClient.defaults.headers.common.Authorization;
    }
}

export function toErrorMessage(error: unknown): string {
    if (axios.isAxiosError(error)) {
        const axiosError = error as AxiosError<{ message?: string }>;
        const status = axiosError.response?.status;
        const serverMessage = axiosError.response?.data?.message;
        return serverMessage ?? (status ? `API 錯誤（HTTP ${status}）` : "API 連線失敗");
    }
    if (error instanceof Error) {
        return error.message;
    }
    return "未知錯誤";
}
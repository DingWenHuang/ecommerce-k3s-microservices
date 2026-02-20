import { apiClient } from "./apiClient";
import type { LoginResponse } from "../auth/authTypes";

export async function registerUser(username: string, password: string): Promise<void> {
    await apiClient.post("/auth/register", { username, password });
}

export async function loginUser(username: string, password: string): Promise<LoginResponse> {
    const response = await apiClient.post<LoginResponse>("/auth/login", { username, password });
    return response.data;
}

export type MeResponse = { userId: number; username: string; role: string };

export async function fetchMe(): Promise<MeResponse> {
    const response = await apiClient.get<MeResponse>("/auth/me");
    return response.data;
}
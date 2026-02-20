export type UserRole = "USER" | "ADMIN";

export type AuthState = {
    accessToken: string | null;
    role: UserRole | null;
    userId: string | null;
};

export type LoginResponse = {
    accessToken: string;
    refreshToken: string;
    accessExpiresInSeconds: number;
};
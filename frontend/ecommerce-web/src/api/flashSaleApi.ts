import { apiClient } from "./apiClient";

export type JoinFlashSaleResponse = {
    ticketId: string;
};

export type FlashSaleTicketStatusResponse = {
    ticketId: string;
    productId: number | null;
    status: "QUEUED" | "PROCESSING" | "SUCCESS" | "SOLD_OUT" | "EXPIRED";
    position: number | null;
    orderId: number | null;
};

export async function joinFlashSale(productId: number): Promise<JoinFlashSaleResponse> {
    const response = await apiClient.post<JoinFlashSaleResponse>(`/flashsale/products/${productId}/join`, {});
    return response.data;
}

export async function getFlashSaleTicketStatus(ticketId: string): Promise<FlashSaleTicketStatusResponse> {
    const response = await apiClient.get<FlashSaleTicketStatusResponse>(`/flashsale/tickets/${ticketId}`);
    return response.data;
}
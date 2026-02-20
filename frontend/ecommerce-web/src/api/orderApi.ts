import { apiClient } from "./apiClient";

export type OrderItem = {
    productId: number;
    quantity: number;
    unitPrice: number;
    lineAmount: number;
};

export type Order = {
    orderId: number;
    totalAmount: number;
    status: string;
    items: OrderItem[];
};

export async function createOrder(productId: number, quantity: number, unitPrice: number): Promise<Order> {
    const response = await apiClient.post<Order>("/orders", {
        productId,
        quantity,
        unitPrice,
    });
    return response.data;
}

export async function fetchMyOrders(): Promise<Order[]> {
    const response = await apiClient.get<Order[]>("/orders");
    return response.data;
}
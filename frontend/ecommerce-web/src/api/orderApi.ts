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

export type CreateNormalOrderItem = {
    productId: number;
    quantity: number;
};

export type CreateNormalOrderRequest = {
    items: CreateNormalOrderItem[];
};

// TODO 這個方法會在下一步被修改為搶購邏輯，這邊先不改動
export async function createOrder(productId: number, quantity: number, unitPrice: number): Promise<Order> {
    const response = await apiClient.post<Order>("/orders/flash", {
        productId,
        quantity,
        unitPrice,
    });
    return response.data;
}

export async function createNormalOrder(request: CreateNormalOrderRequest): Promise<Order> {
    const response = await apiClient.post<Order>("/orders/normal", request);
    return response.data;
}

export async function fetchMyOrders(): Promise<Order[]> {
    const response = await apiClient.get<Order[]>("/orders");
    return response.data;
}
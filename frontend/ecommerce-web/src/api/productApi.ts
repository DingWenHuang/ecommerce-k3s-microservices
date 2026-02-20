import { apiClient } from "./apiClient";

export type Product = {
    id: number;
    name: string;
    price: number; // 前端顯示用 number；後端計算仍用 BigDecimal
    stock: number;
};

export async function fetchProducts(): Promise<Product[]> {
    const response = await apiClient.get<Product[]>("/products");
    return response.data;
}

export async function adminCreateProduct(name: string, price: number, stock: number): Promise<Product> {
    const response = await apiClient.post<Product>("/admin/products", { name, price, stock });
    return response.data;
}

export async function adminRestock(productId: number, amount: number): Promise<Product> {
    const response = await apiClient.post<Product>(`/admin/products/${productId}/restock`, { amount });
    return response.data;
}
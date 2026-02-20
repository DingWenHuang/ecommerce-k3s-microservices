import { apiClient } from "./apiClient";

export type ProductType = "NORMAL" | "FLASH_SALE";

export type Product = {
    id: number;
    name: string;
    price: number; // 前端顯示用 number；後端計算仍用 BigDecimal
    stock: number;
    productType: ProductType;
};

export async function fetchProductsByType(productType: ProductType): Promise<Product[]> {
    const response = await apiClient.get<Product[]>("/products", {
        params: { type: productType },
    });
    return response.data;
}

export async function fetchProductsAll(): Promise<Product[]> {
    // 一次拿全部商品資料，目前先用兩次查詢處理
    const [normal, flash] = await Promise.all([
        fetchProductsByType("NORMAL"),
        fetchProductsByType("FLASH_SALE"),
    ]);
    return [...normal, ...flash];
}

export async function adminCreateProduct(name: string, price: number, stock: number, productType: ProductType): Promise<Product> {
    const response = await apiClient.post<Product>("/admin/products", { name, price, stock, productType });
    return response.data;
}

export async function adminRestock(productId: number, amount: number): Promise<Product> {
    const response = await apiClient.post<Product>(`/admin/products/${productId}/restock`, { amount });
    return response.data;
}
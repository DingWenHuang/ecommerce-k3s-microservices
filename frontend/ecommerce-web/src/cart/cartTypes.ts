import type { Product } from "../api/productApi";

export type CartItem = {
    product: Product;
    quantity: number;
};

export type CartState = {
    items: CartItem[];
};
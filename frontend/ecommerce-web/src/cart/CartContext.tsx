import React, { createContext, useContext, useMemo, useReducer } from "react";
import type { CartState } from "./cartTypes";
import type { Product } from "../api/productApi";

type CartAction =
    | { type: "ADD"; product: Product }
    | { type: "REMOVE"; productId: number }
    | { type: "SET_QTY"; productId: number; quantity: number }
    | { type: "CLEAR" };

const initialState: CartState = { items: [] };

function reducer(state: CartState, action: CartAction): CartState {
    switch (action.type) {
        case "ADD": {
            const existing = state.items.find((i) => i.product.id === action.product.id);
            if (existing) {
                return {
                    items: state.items.map((i) =>
                        i.product.id === action.product.id ? { ...i, quantity: i.quantity + 1 } : i
                    ),
                };
            }
            return { items: [...state.items, { product: action.product, quantity: 1 }] };
        }
        case "REMOVE":
            return { items: state.items.filter((i) => i.product.id !== action.productId) };
        case "SET_QTY":
            return {
                items: state.items
                    .map((i) => (i.product.id === action.productId ? { ...i, quantity: action.quantity } : i))
                    .filter((i) => i.quantity > 0),
            };
        case "CLEAR":
            return initialState;
        default:
            return state;
    }
}

type CartContextValue = CartState & {
    addToCart: (product: Product) => void;
    removeFromCart: (productId: number) => void;
    setQuantity: (productId: number, quantity: number) => void;
    clearCart: () => void;
    totalAmount: number;
};

const CartContext = createContext<CartContextValue | null>(null);

export function CartProvider({ children }: { children: React.ReactNode }) {
    const [state, dispatch] = useReducer(reducer, initialState);

    const totalAmount = state.items.reduce((sum, item) => sum + item.product.price * item.quantity, 0);

    const value = useMemo<CartContextValue>(() => ({
        ...state,
        addToCart: (product) => {
            if (product.productType === "FLASH_SALE") {
                // 搶購商品不進購物車，走排隊流程
                return;
            }
            dispatch({ type: "ADD", product })
        },
        removeFromCart: (productId) => dispatch({ type: "REMOVE", productId }),
        setQuantity: (productId, quantity) => dispatch({ type: "SET_QTY", productId, quantity }),
        clearCart: () => dispatch({ type: "CLEAR" }),
        totalAmount,
    }), [state, totalAmount]);

    return <CartContext.Provider value={value}>{children}</CartContext.Provider>;
}

export function useCart(): CartContextValue {
    const ctx = useContext(CartContext);
    if (!ctx) throw new Error("useCart 必須在 CartProvider 內使用");
    return ctx;
}
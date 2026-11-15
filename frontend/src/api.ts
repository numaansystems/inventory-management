// Typed client for the backend API. Shapes mirror the Java records in the backend.

export type Unit = "GRAM" | "KILOGRAM" | "MILLILITRE" | "LITRE" | "PIECE";
export type MovementType = "RECEIVE" | "SELL" | "ADJUST";

export const UNITS: Unit[] = ["GRAM", "KILOGRAM", "MILLILITRE", "LITRE", "PIECE"];

export interface Category {
  code: string;
  label: string;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string | null;
  category: string | null;
  unit: Unit;
  packSize: number;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ProductInput {
  sku: string;
  name: string;
  description: string | null;
  category: string | null;
  unit: Unit;
  packSize: number;
  active: boolean;
}

export interface StockLevel {
  productId: number;
  sku: string;
  name: string;
  category: string | null;
  active: boolean;
  quantity: number;
  unit: Unit;
  packSize: number;
  totalAmount: number;
  updatedAt: string;
}

export interface StockMovement {
  id: number;
  productId: number;
  type: MovementType;
  quantityChange: number;
  balanceAfter: number;
  reference: string | null;
  note: string | null;
  occurredAt: string;
}

export interface MovementInput {
  type: MovementType;
  quantity: number;
  reference: string | null;
  note: string | null;
}

export interface Page<T> {
  content: T[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface FieldError {
  field: string;
  message: string;
}

/** An error response from the API (RFC 9457 problem+json) or a network failure. */
export class ApiError extends Error {
  readonly status: number;
  readonly code: string;
  readonly fieldErrors: FieldError[];

  constructor(status: number, code: string, message: string, fieldErrors: FieldError[] = []) {
    super(message);
    this.name = "ApiError";
    this.status = status;
    this.code = code;
    this.fieldErrors = fieldErrors;
  }

  /** The message for one form field, if the server reported one. */
  fieldMessage(field: string): string | undefined {
    return this.fieldErrors.find((error) => error.field === field)?.message;
  }
}

async function request<T>(path: string, init?: RequestInit): Promise<T> {
  let response: Response;
  try {
    response = await fetch(path, {
      ...init,
      headers: init?.body ? { "Content-Type": "application/json" } : undefined,
    });
  } catch {
    throw new ApiError(0, "NETWORK", "Cannot reach the server. Is the backend running on port 8080?");
  }
  if (response.ok) {
    return response.status === 204 ? (undefined as T) : ((await response.json()) as T);
  }
  let problem: { code?: string; detail?: string; title?: string; errors?: FieldError[] } = {};
  try {
    problem = await response.json();
  } catch {
    // Not JSON (for example a proxy error page); fall through to the generic message.
  }
  throw new ApiError(
    response.status,
    problem.code ?? "ERROR",
    problem.detail ?? problem.title ?? `Request failed (${response.status})`,
    problem.errors ?? [],
  );
}

// The catalogue is small, so lists are loaded in one page and filtered in the browser.
const ALL = "size=1000";

export const api = {
  categories: () => request<Category[]>("/api/products/categories"),

  products: () => request<Page<Product>>(`/api/products?${ALL}&sort=sku`).then((page) => page.content),

  createProduct: (input: ProductInput) =>
    request<Product>("/api/products", { method: "POST", body: JSON.stringify(input) }),

  updateProduct: (id: number, input: ProductInput) =>
    request<Product>(`/api/products/${id}`, { method: "PUT", body: JSON.stringify(input) }),

  deleteProduct: (id: number) => request<void>(`/api/products/${id}`, { method: "DELETE" }),

  stock: () => request<Page<StockLevel>>(`/api/stock?${ALL}&sort=product.sku`).then((page) => page.content),

  recordMovement: (productId: number, input: MovementInput) =>
    request<StockMovement>(`/api/products/${productId}/stock/movements`, {
      method: "POST",
      body: JSON.stringify(input),
    }),

  movements: (productId: number) =>
    request<Page<StockMovement>>(`/api/products/${productId}/stock/movements?size=100`).then(
      (page) => page.content,
    ),
};

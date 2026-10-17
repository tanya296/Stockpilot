// TypeScript mirrors of the backend DTOs. If a backend field changes, the compiler
// points at every place in the UI that needs updating.

export type Role = 'STAFF' | 'MANAGER' | 'ADMIN'

export interface AuthResponse {
  token: string
  email: string
  fullName: string
  role: Role
}

export interface Page<T> {
  content: T[]
  page: number
  size: number
  totalElements: number
  totalPages: number
}

export interface Product {
  id: number
  sku: string
  name: string
  category: string
  unitPrice: number
  reorderLevel: number
  totalQuantity: number
  lowStock: boolean
}

export interface ProductInput {
  sku: string
  name: string
  category: string
  unitPrice: number
  reorderLevel: number
}

export interface StockLevel {
  warehouseId: number
  warehouseName: string
  city: string
  quantity: number
  version: number
}

export interface ProductDetail {
  product: Product
  stock: StockLevel[]
}

export type MovementType = 'IN' | 'OUT' | 'TRANSFER_IN' | 'TRANSFER_OUT' | 'PO_RECEIPT'

export interface Movement {
  id: number
  type: MovementType
  quantity: number
  warehouseId: number
  warehouseName: string
  reason: string | null
  createdBy: string
  createdAt: string
}

export interface Warehouse {
  id: number
  name: string
  city: string
}

export interface Supplier {
  id: number
  name: string
  contact: string | null
}

export type PurchaseOrderStatus = 'DRAFT' | 'ORDERED' | 'RECEIVED' | 'CANCELLED'

export interface PurchaseOrderItem {
  productId: number
  sku: string
  productName: string
  quantity: number
  unitCost: number
  lineTotal: number
}

export interface PurchaseOrder {
  id: number
  supplierId: number
  supplierName: string
  warehouseId: number
  warehouseName: string
  status: PurchaseOrderStatus
  createdBy: string
  createdAt: string
  updatedAt: string
  totalCost: number
  items: PurchaseOrderItem[]
}

export interface DashboardSummary {
  totalProducts: number
  lowStockCount: number
  openPurchaseOrders: number
  stockByWarehouse: { warehouseName: string; totalUnits: number }[]
}

export interface LowStockItem {
  productId: number
  sku: string
  name: string
  totalQuantity: number
  reorderLevel: number
  shortfall: number
}

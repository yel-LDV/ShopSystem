export interface User {
  userId: number
  email: string
  fullName: string
  role: string
}

export interface LoginResponse {
  token: string
  refreshToken: string
  role: string
  email: string
  fullName: string
  userId: number
}

export interface CreateBatchRequest {
  quantity: number
  expirationYear: number
  expirationMonth: number
  expirationDay: number
  purchasePrice?: number
}

export interface Batch {
  id: number
  quantity: number
  availableQuantity: number
  expirationDate: string
  purchasePrice: number
  createdAt: string
  productId: number
}

export interface Product {
  id: number
  code: string | null
  name: string
  basePrice: number
  minStock: number
  maxStock: number
  supplierId: number
  supplierName: string
  unitId: number | null
  unitName: string
  unitAbbreviation: string
  totalStock: number
  creationDate: string
  expirationDate: string | null
  lastUpdated?: string
  batches?: CreateBatchRequest[]
}

export interface SaleItem {
  id?: number
  productId: number
  productName: string
  productCode: string | null
  quantity: number
  unitPrice: number
  subtotal: number
}

export interface Sale {
  id: number
  storeName: string
  saleDate: string
  total: number
  itemCount: number
  items: SaleItem[]
}

export interface CreateSaleRequest {
  items: { productId: number; quantity: number }[]
}

export interface OrderItem {
  id: number
  productName: string
  quantity: number
  unitPrice: number
}

export interface Order {
  id: number
  storeName: string
  supplierName: string
  supplierId: number
  status: string
  isAutomatic: boolean
  createdAt: string
  respondedAt: string | null
  rejectionReason: string | null
  itemCount: number
  total: number
  items?: OrderItem[]
  ticketId?: number | null
}

export interface Ticket {
  id: number
  orderId: number
  storeName: string
  supplierName: string
  status: string
  createdAt: string
  votingEndDate: string | null
  finalResolution: string | null
  discountPercentage: number | null
  storeOwnerVote: string | null
  supplierVote: string | null
  adminVote: string | null
  proposedPrice: number | null
  priceProposedBy: string | null
  negotiationStatus: string | null
  messages: Message[]
}

export interface Message {
  id: number
  ticketId: number
  senderId: number
  senderRole: string
  senderName?: string
  content: string
  createdAt: string
}

export interface Notification {
  id: number
  message: string
  read: boolean
  createdAt: string
  type: string
  referenceId: number | null
}

export interface InventoryItem {
  id: number
  supplierProduct: Product
  quantity: number
  minStock: number
  maxStock: number
  lastUpdated: string
}

export interface RegistrationRequest {
  id: number
  email: string
  fullName: string
  role: string
  storeName: string | null
  companyName: string | null
  status: string
  createdAt: string
}

export interface AuditLogEntry {
  id: number
  username: string
  action: string
  entityType: string
  entityId: number | null
  oldValue: string | null
  newValue: string | null
  createdAt: string
}

export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

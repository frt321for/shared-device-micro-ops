import { api } from './client';

export interface ISite { id: number; name: string; address?: string; building?: string; serviceLevel: string; status: string; contactName?: string; contactPhone?: string; latitude?: number; longitude?: number; createdAt: string; }
export interface ISiteCreate { name: string; address?: string; building?: string; serviceLevel?: string; status?: string; contactName?: string; contactPhone?: string; }
export interface IDevice { id: number; deviceCode: string; deviceTypeId: number; name: string; siteId?: number; status: string; capacity?: number; createdAt: string; }
export interface IDeviceCreate { deviceCode: string; deviceTypeId: number; name: string; siteId?: number; status?: string; }
export interface IDeviceType { id: number; code: string; name: string; category: string; }
export interface ISku { id: number; code: string; name: string; category?: string; unit?: string; costPrice?: number; sellingPrice?: number; }
export interface IWorkOrder { id: number; orderNo: string; type: string; status: string; priority: number; title: string; deviceId?: number; siteId?: number; skuId?: number; expectedQty?: number; actualQty?: number; assigneeId?: number; description?: string; createdAt: string; updatedAt?: string; }
export interface IDeviceStock { id: number; deviceId: number; skuId: number; quantity: number; minThreshold: number; maxCapacity: number; status: string; }
export interface IWarehouseStock { id: number; skuId: number; quantity: number; batchNo?: string; }
export interface IRoute { id: number; name: string; status: string; assigneeId?: number; totalDistance?: number; estimatedMinutes?: number; }
export interface ISiteRevenue { siteId: number; siteName: string; totalOrders: number; totalRevenue: number; totalCost: number; grossProfit: number; }
export interface IWeeklyReport { id: number; siteId: number; title: string; content?: string; status: string; periodStart: string; periodEnd: string; }
export interface IDashboardOverview { totalSites: number; totalDevices: number; onlineDevices: number; faultDevices: number; lowStockCount: number; pendingWorkOrders: number; activeRoutes: number; }

export const fetchDashboardOverview = () => api.get<IDashboardOverview>('/dashboard/overview');
export const fetchDashboardAlerts = () => api.get<Record<string,unknown>[]>('/dashboard/alerts');
export const fetchDashboardDeviceStats = () => api.get<Record<string,unknown>>('/dashboard/device-stats');

export const fetchSites = (params?: Record<string,unknown>) => api.get<{content:ISite[];totalElements:number}>('/sites', params);
export const createSite = (data: ISiteCreate) => api.post<ISite>('/sites', data);
export const updateSite = (id: number, data: Partial<ISite>) => api.put<ISite>(`/sites/${id}`, data);
export const deleteSite = (id: number) => api.del(`/sites/${id}`);

export const fetchDevices = (params?: Record<string,unknown>) => api.get<{content:IDevice[];totalElements:number}>('/devices', params);
export const createDevice = (data: IDeviceCreate) => api.post<IDevice>('/devices', data);
export const updateDevice = (id: number, data: Partial<IDevice>) => api.put<IDevice>(`/devices/${id}`, data);
export const deleteDevice = (id: number) => api.del(`/devices/${id}`);

export const fetchDeviceTypes = () => api.get<IDeviceType[]>('/device-types');

export const fetchWorkOrders = (params?: Record<string,unknown>) => api.get<{content:IWorkOrder[];totalElements:number;totalPages:number;number:number}>('/work-orders', params);
export const createWorkOrder = (data: Partial<IWorkOrder>) => api.post<IWorkOrder>('/work-orders', data);
export const assignWorkOrder = (id: number, data: {assigneeId:number}) => api.put<IWorkOrder>(`/work-orders/${id}/assign`, data);
export const arriveWorkOrder = (id: number) => api.put<IWorkOrder>(`/work-orders/${id}/arrive`);
export const processWorkOrder = (id: number) => api.put<IWorkOrder>(`/work-orders/${id}/process`);
export const completeWorkOrder = (id: number, data: {actualQty:number}) => api.put<IWorkOrder>(`/work-orders/${id}/complete`, data);
export const reviewWorkOrder = (id: number, data: {reviewResult:string;remark?:string}) => api.put<IWorkOrder>(`/work-orders/${id}/review`, data);

export const fetchSkus = () => api.get<{content:ISku[]}>('/skus');
export const createSku = (data: Partial<ISku>) => api.post<ISku>('/skus', data);
export const updateSku = (id: number, data: Partial<ISku>) => api.put<ISku>(`/skus/${id}`, data);
export const deleteSku = (id: number) => api.del(`/skus/${id}`);

export const fetchDeviceStock = (deviceId?: number) => api.get<IDeviceStock[]>('/device-stock', deviceId ? {deviceId} : undefined);
export const fetchWarehouseStock = () => api.get<IWarehouseStock[]>('/warehouse/stock');

export interface IWarehouseInbound { skuId: number; quantity: number; batchNo?: string; }
export interface IWarehouseOutbound { skuId: number; quantity: number; referenceType?: string; referenceId?: number; }
export interface IStockCheck { skuId: number; quantity: number; }
export interface ILossRecord { id: number; deviceId?: number; skuId: number; quantity: number; reason?: string; createdAt: string; }
export interface ILossCreate { deviceId?: number; skuId: number; quantity: number; reason?: string; }

export const warehouseInbound = (data: IWarehouseInbound) => api.post('/warehouse/inbound', data);
export const warehouseOutbound = (data: IWarehouseOutbound) => api.post('/warehouse/outbound', data);
export const warehouseCheck = (data: IStockCheck) => api.post('/warehouse/check', data);
export const fetchLossRecords = (params?: Record<string,unknown>) => api.get<{content:ILossRecord[]}>('/stock/loss', params);
export const createLossRecord = (data: ILossCreate) => api.post<ILossRecord>('/stock/loss', data);

export const fetchRoutes = () => api.get<{content:IRoute[]}>('/dispatch/routes/active');
export const createRoute = (data: {workOrderIds:number[];assigneeId:number}) => api.post<IRoute>('/dispatch/routes', data);

export const fetchRevenueOverview = () => api.get<Record<string,unknown>>('/revenue/overview');
export const fetchRevenueSites = () => api.get<ISiteRevenue[]>('/revenue/sites');
export const fetchRevenueDevices = () => api.get<Record<string,unknown>[]>('/revenue/devices');

export const fetchAiReportSites = () => api.get<ISite[]>('/sites');
export const fetchAiReportsBySite = (siteId: number) => api.get<IWeeklyReport[]>('/ai/weekly-reports', {siteId});
export const generateAiReport = (data: {siteId:number;periodStart:string;periodEnd:string}) => api.post<IWeeklyReport>('/ai/weekly-report', data);
export const fetchAiReportDetail = (id: number) => api.get<IWeeklyReport>(`/ai/weekly-reports/${id}`);

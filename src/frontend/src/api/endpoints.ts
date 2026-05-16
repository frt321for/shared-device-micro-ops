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

export const fetchRevenueOverview = (params?: Record<string,unknown>) => api.get<Record<string,unknown>>('/revenue/overview', params);
export const fetchRevenueSites = (params?: Record<string,unknown>) => api.get<ISiteRevenue[]>('/revenue/sites', params);
export const fetchRevenueDevices = (params?: Record<string,unknown>) => api.get<Record<string,unknown>[]>('/revenue/devices', params);

export const fetchAiReportSites = () => api.get<ISite[]>('/sites');
export const fetchAiReportsBySite = (siteId: number) => api.get<IWeeklyReport[]>('/ai/weekly-reports', {siteId});
export const generateAiReport = (data: {siteId:number;periodStart:string;periodEnd:string}) => api.post<IWeeklyReport>('/ai/weekly-report', data);
export const fetchAiReportDetail = (id: number) => api.get<IWeeklyReport>(`/ai/weekly-reports/${id}`);

export const fetchDeviceTelemetry = (deviceId: number, metric: string) =>
  api.get<unknown[]>(`/devices/${deviceId}/telemetry`, { metric });
export const fetchDeviceEventsList = (deviceId: number) =>
  api.get<unknown[]>(`/devices/${deviceId}/events`);
export const fetchDeviceEventSummary = (deviceId: number) =>
  api.get<Record<string, number>>(`/devices/${deviceId}/events/summary`);
export const sendDeviceCommand = (deviceId: number, command: string, params?: Record<string, unknown>) =>
  api.post<Record<string, unknown>>(`/devices/${deviceId}/command`, { command, params });

export const cancelWorkOrder = (id: number) => api.put<IWorkOrder>(`/work-orders/${id}/cancel`);
export const fetchWorkOrderAudit = (id: number) => api.get<Record<string, unknown>[]>(`/work-orders/${id}/audit`);

export const fetchDeviceGroups = (siteId?: number) => api.get<unknown[]>('/device-groups', siteId ? { siteId } : undefined);
export const createDeviceGroup = (data: { name: string; siteId: number; description?: string }) => api.post<unknown>('/device-groups', data);
export const updateDeviceGroup = (id: number, data: { name: string; description?: string }) => api.put<unknown>(`/device-groups/${id}`, data);
export const deleteDeviceGroup = (id: number) => api.del(`/device-groups/${id}`);
export const fetchDeviceGroupMembers = (id: number) => api.get<unknown[]>(`/device-groups/${id}/members`);
export const addDeviceGroupMember = (id: number, deviceId: number) => api.post(`/device-groups/${id}/members`, { deviceId });
export const removeDeviceGroupMember = (id: number, deviceId: number) => api.del(`/device-groups/${id}/members/${deviceId}`);

export const fetchRouteById = (id: number) => api.get<IRoute>(`/dispatch/routes/${id}`);
export const updateRoute = (id: number, data: Partial<IRoute>) => api.put<IRoute>(`/dispatch/routes/${id}`, data);
export const fetchDispatchPriorities = () => api.get<unknown[]>('/dispatch/priorities');
export const updateRouteStopOrder = (routeId: number, stopIds: number[]) => api.put(`/dispatch/routes/${routeId}/stops/order`, { stopIds });

export const correctDeviceStock = (id: number, quantity: number, reason: string) =>
  api.put<IDeviceStock>(`/device-stock/${id}/correct`, null, { params: { quantity, reason, operator: 'manual' } });
export const fetchStockPredictions = () => api.get<IDeviceStock[]>('/stock/predictions');
export const fetchWarehouseStockBySku = (skuId: number) => api.get<IWarehouseStock[]>(`/warehouse/stock/${skuId}`);

export const fetchRevenueSkuAnalysis = (params?: Record<string, unknown>) =>
  api.get<Record<string, unknown>[]>('/revenue/skus', params);

export const fetchSlaOverview = () => api.get<Record<string, unknown>>('/sla/overview');
export const fetchTodayTasks = () => api.get<unknown[]>('/dashboard/today-tasks');

export const generateReplenishmentNote = (data: { siteId: number; skuIds?: number[] }) =>
  api.post<{ content: string }>('/ai/replenishment-note', data);
export const generateFaultAnalysis = (data: { deviceId: number; faultCode?: string; description?: string }) =>
  api.post<{ content: string }>('/ai/fault-analysis', data);

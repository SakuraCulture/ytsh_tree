import request from '@/config/axios'

export interface EleTrafficTodayStatsRespVO {
  totalRequests?: number
  totalRequestBytes?: number
  totalResponseBytes?: number
  avgRequestBytes?: number
  avgResponseBytes?: number
  successRate?: number
  avgDurationMs?: number
  maxRequestBytes?: number
  maxResponseBytes?: number
  successRequests?: number
  failedRequests?: number
}

export interface EleTrafficHourlyStatsRespVO {
  hour?: string
  requests?: number
  requestBytes?: number
  responseBytes?: number
  avgDurationMs?: number
  successCount?: number
  failedCount?: number
}

export interface EleTrafficRecordVO {
  apiCode?: string
  traceId?: string
  requestBytes?: number
  responseBytes?: number
  durationMs?: number
  success?: boolean
  timestamp?: string
}

export const getTodayStats = async () => {
  return await request.get<EleTrafficTodayStatsRespVO>({ url: '/ele/traffic/today-stats' })
}

export const getStatsByDate = async (date: string) => {
  return await request.get<EleTrafficTodayStatsRespVO>({ url: '/ele/traffic/stats', params: { date } })
}

export const getHourlyStats = async () => {
  return await request.get<EleTrafficHourlyStatsRespVO[]>({ url: '/ele/traffic/hourly-stats' })
}

export const getHourlyStatsByDate = async (date: string) => {
  return await request.get<EleTrafficHourlyStatsRespVO[]>({ url: '/ele/traffic/hourly-stats-by-date', params: { date } })
}

export const getSingleRecord = async (traceId: string) => {
  return await request.get<EleTrafficRecordVO>({ url: `/ele/traffic/record/${traceId}` })
}

export const resetStats = async () => {
  return await request.post<boolean>({ url: '/ele/traffic/reset' })
}

export interface EleSyncConfigRespVO {
  syncIntervalMinutes: number
  syncIntervalMs: number
  syncCron: string
}

export const getSyncConfig = async () => {
  return await request.get<EleSyncConfigRespVO>({ url: '/ele/order/sync/config' })
}

export interface EleTrafficApiRpsRespVO {
  apiCode?: string
  apiName?: string
  apiPath?: string
  currentRps?: number
  avgRps5s?: number
  maxRps60s?: number
  totalRequests?: number
  timestamp?: number
  rpsHistory?: number[]
}

export interface EleApiRateLimitApiStatusRespVO {
  apiName?: string
  apiCode?: string
  displayName?: string
  qps?: number
  waitingCount?: number
  hasBacklog?: boolean
}

export interface EleApiRateLimitStatusRespVO {
  globalQps?: number
  hasBacklog?: boolean
  waitingCount?: number
  localWaitingCount?: number
  queueAlert?: boolean
  message?: string
  timestamp?: number
  apis?: EleApiRateLimitApiStatusRespVO[]
}

export const getApiRateLimitStatus = async () => {
  return await request.get<EleApiRateLimitStatusRespVO>({ url: '/ele/order/rate-limit/status' })
}

export const getApiRps = async () => {
  return await request.get<EleTrafficApiRpsRespVO[]>({ url: '/ele/traffic/api-rps' })
}

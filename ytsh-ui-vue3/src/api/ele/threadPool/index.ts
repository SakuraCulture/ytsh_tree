import request from '@/config/axios'

export interface ThreadPoolStatusRespVO {
  poolName?: string
  threadNamePrefix?: string
  corePoolSize?: number
  maxPoolSize?: number
  poolSize?: number
  activeCount?: number
  activePercent?: number
  queueSize?: number
  queueCapacity?: number
  queueUsagePercent?: number
  completedTaskCount?: number
  taskCount?: number
  rejectedPolicy?: string
  healthStatus?: string
  healthMessage?: string
}

export interface PoolAlarmConfig {
  poolName: string
  queueThresholdPercent: number
  activeThresholdPercent: number
  enabled: boolean
}

export interface AlarmThresholdReqVO {
  poolName: string
  queueThresholdPercent?: number
  activeThresholdPercent?: number
  enabled?: boolean
}

export const getAllPoolStatus = async () => {
  return await request.get<ThreadPoolStatusRespVO[]>({ url: '/ele/thread-pool/status' })
}

export const getPoolStatus = async (poolName: string) => {
  return await request.get<ThreadPoolStatusRespVO>({ url: `/ele/thread-pool/status/${poolName}` })
}

export const getHealthCheck = async () => {
  return await request.get({ url: '/ele/thread-pool/health' })
}

export const getAllAlarmThresholds = async () => {
  return await request.get<Record<string, PoolAlarmConfig>>({ url: '/ele/thread-pool/alarm-threshold' })
}

export const setAlarmThreshold = async (data: AlarmThresholdReqVO) => {
  return await request.put<boolean>({ url: '/ele/thread-pool/alarm-threshold', data })
}

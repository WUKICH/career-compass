import request from '@/utils/request'

/** 前端特性开关（与后端 app.full-version-temporarily-closed 等对齐） */
export const fetchClientFeatures = () => request.get('/config/features')

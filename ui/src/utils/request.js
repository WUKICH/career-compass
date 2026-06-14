import axios from "axios";
import { ElMessage } from 'element-plus'
import router from '../router'
import { encrypt, decrypt } from './encrypt'

// 是否启用前端加/解密，默认开启。调试时可在 .env.development 中设置 VITE_ENABLE_ENCRYPT=false
const ENABLE_ENCRYPT = import.meta.env.VITE_ENABLE_ENCRYPT !== 'false';

/** 后端 @Idempotent(required=true) 的 POST，需在请求头携带 Idempotency-Key（见 IdempotencyAspect） */
const IDEMPOTENCY_REQUIRED_URL_PARTS = [
  "/chat/collect/confirm",
  "/chat/start-skill",
  "/chat/skill/answer",
  "/chat/career/answer",
  "/chat/feedback",
  "/full/chat/skill/answer",
  "/full/chat/career/answer",
];

function urlNeedsIdempotencyKey(url) {
  if (!url || typeof url !== "string") return false;
  return IDEMPOTENCY_REQUIRED_URL_PARTS.some((p) => url.includes(p));
}

function newIdempotencyKey() {
  if (typeof crypto !== "undefined" && typeof crypto.randomUUID === "function") {
    return crypto.randomUUID();
  }
  return `${Date.now()}-${Math.random().toString(36).slice(2, 12)}`;
}

//创建axios实例对象
const request = axios.create({
  baseURL: "/ai/match/api",
  timeout: 600000
});

//axios的响应response拦截器
request.interceptors.response.use(
    (response) => {//成功回调
            // 检查响应数据是否存在
            if (!response.data) {
            ElMessage.warning('接口返回数据为空');
            return Promise.reject(new Error('Empty response'));
            }
            
            // 判断是否是加密响应（EncryptedResult格式），且前端加密开关已开启
            if (ENABLE_ENCRYPT && response.data.code !== undefined && response.data.data && typeof response.data.data === 'string') {
                // 这是加密响应，需要解密
                try {
                    const encryptedData = response.data.data;
                    
                    // 验证Base64格式（简单检查）
                    if (!encryptedData.match(/^[A-Za-z0-9+/=]+$/)) {
                        console.error('加密数据格式不正确，不是有效的Base64字符串');
                        throw new Error('加密数据格式错误');
                    }
                    
                    // 解密数据
                    const decryptedStr = decrypt(encryptedData);
                    
                    // 验证解密后的数据不为空
                    if (!decryptedStr || decryptedStr.trim().length === 0) {
                        console.error('解密后的数据为空');
                        throw new Error('解密后的数据为空');
                    }
                    
                    // 验证解密后的数据是否为有效的JSON格式（简单检查）
                    const trimmed = decryptedStr.trim();
                    if (!trimmed.startsWith('{') && !trimmed.startsWith('[')) {
                        console.error('解密后的数据不是有效的JSON格式:', trimmed.substring(0, 100));
                        throw new Error('解密后的数据格式错误');
                    }
                    
                    // 解析JSON
                    const decryptedData = JSON.parse(decryptedStr);
                    
                    // 验证解析后的数据格式
                    if (decryptedData && decryptedData.code !== undefined) {
                        response.data = decryptedData;
                    } else {
                        console.error('解密后的数据格式不正确，缺少code字段');
                        throw new Error('解密后的数据格式错误');
                    }
                } catch (error) {
                    console.error('响应解密失败:', {
                        error: error.message,
                        errorType: error.constructor.name,
                        encryptedDataLength: response.data.data?.length,
                        encryptedDataPreview: response.data.data?.substring(0, 100)
                    });
                    ElMessage.error('数据解密失败: ' + error.message);
                    return Promise.reject(new Error('数据解密失败: ' + error.message));
                }
            }
            
            // 根据你的业务约定处理（假设成功code为 1）
            if (response.data.code === 1) {
            return response.data; // 返回业务数据
        } else {
            // 业务逻辑错误（如code=500等）
            const errMsg = response.data.msg || '接口请求失败';
            ElMessage.error(errMsg);
            return Promise.reject(new Error(errMsg));
        }
    },
    (error) => {
        // 统一错误处理
        let errorMessage = '接口访问异常';
        
        if (error.response) {
        // 有响应但状态码非2xx
        switch (error.response.status) {
            case 401:
            errorMessage = '登录失效，请重新登录';
            router.push('/login');
            break;
            case 403:
            errorMessage = '没有权限访问';
            break;
            case 404:
            errorMessage = '请求资源不存在';
            break;
            case 413: {
            errorMessage = '简历文件不能超过 5MB，请压缩或更换文件后重试';
            const body = error.response.data;
            try {
              if (typeof body === 'string' && body.trim().startsWith('{')) {
                const parsed = JSON.parse(body);
                if (parsed?.message) errorMessage = parsed.message;
              } else if (body?.message) {
                errorMessage = body.message;
              }
            } catch (_) { /* use default */ }
            break;
            }
            case 500:
            errorMessage = '服务器内部错误';
            break;
        }
        } else if (error.request) {
            // 请求已发出但没有收到响应
            errorMessage = '网络异常，请检查网络连接';
        } else {
            // 请求配置出错
            errorMessage = '请求配置错误';
            }
            
            ElMessage.error(errorMessage);
            return Promise.reject(error);
    }
);

//axios的请求 request 拦截器, 每次请求获取localStorage中的loginUser或userInfo, 从中获取到token, 在请求头token中携带到服务端
// 同时加密请求体数据
request.interceptors.request.use(
  (config) => {
    // 安全获取token，优先从loginUser获取，其次从userInfo获取
    try {
      let token = null;
      
      // 首先尝试从loginUser获取
      const loginUser = JSON.parse(localStorage.getItem('loginUser') || 'null');
      if (loginUser?.token) {
        token = loginUser.token;
      } else {
        // 如果没有，尝试从userInfo获取
        const userInfo = JSON.parse(localStorage.getItem('userInfo') || 'null');
        if (userInfo?.token) {
          token = userInfo.token;
        }
      }
      
      // 如果找到token，添加到请求头
      if (token) {
        config.headers.token = token;
      }
    } catch (e) {
      console.error('解析token失败', e);
    }

    const method = String(config.method || "get").toLowerCase();
    const reqUrl = config.url || "";
    if (
      method === "post" &&
      urlNeedsIdempotencyKey(reqUrl) &&
      !config.headers["Idempotency-Key"] &&
      !config.headers["idempotency-key"]
    ) {
      config.headers["Idempotency-Key"] = newIdempotencyKey();
    }
    
    // 加密请求体数据（排除不需要加密的接口），仅在开启加密时处理
    if (ENABLE_ENCRYPT && config.data && config.method !== 'get' && config.method !== 'GET') {
      // 文件上传（FormData）不走文本加密，避免破坏 multipart 格式
      if (typeof FormData !== 'undefined' && config.data instanceof FormData) {
        return config;
      }
      const url = config.url || '';
      // 排除不需要加密的路径
      if (!url.includes('/swagger-ui') && 
          !url.includes('/v3/api-docs') && 
          !url.includes('/doc.html') &&
          !url.includes('/chat/test') &&
          !url.includes('/chat/stream')) {
        try {
          // 将请求体对象转换为JSON字符串，然后加密
          const jsonStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data);
          const encryptedData = encrypt(jsonStr);
          config.data = encryptedData;
          // 设置Content-Type
          config.headers['Content-Type'] = 'text/plain';
        } catch (error) {
          console.error('请求数据加密失败:', error);
          // 加密失败，使用原始数据
        }
      }
    }
    
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

export default request

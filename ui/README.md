# 前端工程（Vue3 + Vite）

本目录为项目完整前端源代码，技术栈为 Vue 3 + Vite + Element Plus + Axios + Pinia。

## 1. 运行环境

- Node.js: `^20.19.0 || >=22.12.0`
- npm: 建议 `>=10`

## 2. 安装依赖

```bash
npm install
```

## 3. 本地开发启动

```bash
npm run dev
```

默认会启动 Vite 开发服务（通常是 `http://localhost:5173`）。

## 4. 打包与预览

```bash
# 生产构建
npm run build

# 本地预览构建产物
npm run preview
```

## 5. 后端接口对接说明（开发环境）

- 前端请求基路径配置在 `src/utils/request.js`：`/ai/match/api`
- 建议通过 Vite 代理或网关将 `/ai/match/api` 转发到后端服务
- 请求默认会在拦截器中携带 `token` 请求头（若本地存储中存在）
- 默认启用请求加密；如需关闭前端加密，可在 `.env.development` 中设置：

```env
VITE_ENABLE_ENCRYPT=false
```

## 6. 目录结构（核心）

```text
ui/
  ├─ src/
  │  ├─ api/            # 接口封装（chat.js, fullChat.js）
  │  ├─ views/          # 页面（表单、聊天、确认、报告等）
  │  ├─ router/         # 路由
  │  ├─ stores/         # 状态管理
  │  └─ utils/          # request、加解密等工具
  ├─ index.html
  ├─ vite.config.js
  └─ package.json
```

## 7. 主要脚本

- `npm run dev`：开发模式
- `npm run build`：生产构建
- `npm run preview`：构建结果预览

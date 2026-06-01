# OCR 服务接口文档

本文档面向 Vue3 + TypeScript 前端项目，用于对接当前 Spring Boot OCR 后端服务。

后端默认端口：

```text
8080
```

默认基础地址：

```text
http://localhost:8080
```

项目未配置 `server.servlet.context-path`，因此接口路径从根路径 `/` 开始。

---

## 1. 通用响应结构

所有业务接口统一返回 `ApiResponse<T>`：

```ts
interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}
```

成功示例：

```json
{
  "success": true,
  "message": "ok",
  "data": {}
}
```

失败示例：

```json
{
  "success": false,
  "message": "错误信息",
  "data": null
}
```

前端注意：

- 不应只依赖 HTTP 状态码，也应检查 `success` 字段。
- 错误提示信息在 `message` 字段。
- 参数错误通常返回 HTTP `400`。
- OCR 处理异常通常返回 HTTP `500`。

---

## 2. 服务入口信息

### `GET /`

### 作用

返回后端服务基础入口信息，前端可用于检测后端是否可访问。

### 请求参数

无。

### 响应示例

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "service": "school-orc",
    "ocrApi": "/api/ocr",
    "swaggerUi": "/swagger-ui.html",
    "openApi": "/v3/api-docs",
    "health": "/actuator/health"
  }
}
```

### 前端用途

- 检查后端服务是否在线。
- 获取 OCR API、Swagger、OpenAPI、健康检查路径。

---

## 3. 同步 OCR 识别：文件上传

### `POST /api/ocr/recognize`

### 作用

同步识别上传的图片或 PDF 文件，返回 OCR 文本结果。

适合：

- 小图片识别。
- 页数较少的 PDF。
- 前端可以接受等待 OCR 完成后再返回结果的场景。

### Content-Type

```text
multipart/form-data
```

### form-data 参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `file` | File | 是 | 图片或 PDF 文件 |
| `engine` | string | 否 | OCR 引擎：`tess4j`、`paddle`、`auto` |
| `language` | string | 否 | 识别语言，例如 `chi_sim+eng` |
| `psm` | number | 否 | Tesseract PSM，范围 `0-13` |
| `minConfidence` | number | 否 | 最低置信度，范围 `0-100` |
| `preprocess` | boolean | 否 | 是否启用图片预处理 |

### 支持文件类型

后端通过文件头判断文件类型，不只依赖扩展名。

支持：

```text
PDF / PNG / JPG / JPEG / BMP
```

### 响应类型

```ts
interface OcrTextBlock {
  text: string
  x: number
  y: number
  width: number
  height: number
  confidence: number
  page: number
}

interface OcrResult {
  text: string
  blocks: OcrTextBlock[]
  page: number
  language: string
  durationMillis: number
  engine: string
}
```

### 响应示例

```json
{
  "success": true,
  "message": "ok",
  "data": [
    {
      "text": "识别出的整页文本",
      "blocks": [
        {
          "text": "文本块内容",
          "x": 0,
          "y": 0,
          "width": 100,
          "height": 20,
          "confidence": 95.5,
          "page": 1
        }
      ],
      "page": 1,
      "language": "chi_sim+eng",
      "durationMillis": 1234,
      "engine": "tess4j"
    }
  ]
}
```

### 前端 Vue3 + TypeScript 调用示例

```ts
export async function recognizeFile(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('engine', 'tess4j')
  formData.append('language', 'chi_sim+eng')
  formData.append('psm', '3')
  formData.append('preprocess', 'true')

  const res = await fetch('/api/ocr/recognize', {
    method: 'POST',
    body: formData
  })

  return await res.json()
}
```

### 前端注意事项

- 文件字段名必须是 `file`。
- 上传大小默认限制为 `20MB`。
- PDF 默认最大页数为 `20`。
- 同步接口可能耗时较长，前端应显示 loading。
- 大文件或多页 PDF 建议使用异步接口。

---

## 4. 同步 OCR 识别：Base64

### `POST /api/ocr/recognize/base64`

### 作用

同步识别 Base64 编码的图片或 PDF。

### Content-Type

```text
application/json
```

### 请求体类型

```ts
interface Base64OcrRequest {
  data: string
  options?: OcrOptions
}

interface OcrOptions {
  engine?: 'tess4j' | 'paddle' | 'auto'
  language?: string
  psm?: number
  minConfidence?: number
  preprocess?: boolean
}
```

### 请求示例

```json
{
  "data": "data:image/png;base64,iVBORw0KGgo...",
  "options": {
    "engine": "tess4j",
    "language": "chi_sim+eng",
    "psm": 3,
    "minConfidence": 0,
    "preprocess": true
  }
}
```

`data` 支持两种格式：

```text
iVBORw0KGgo...
```

或：

```text
data:image/png;base64,iVBORw0KGgo...
```

### 响应结构

同 `POST /api/ocr/recognize`：

```ts
ApiResponse<OcrResult[]>
```

### 前端调用示例

```ts
export async function recognizeBase64(base64: string) {
  const res = await fetch('/api/ocr/recognize/base64', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json'
    },
    body: JSON.stringify({
      data: base64,
      options: {
        engine: 'tess4j',
        language: 'chi_sim+eng',
        psm: 3,
        preprocess: true
      }
    })
  })

  return await res.json()
}
```

### 前端注意事项

- `data` 不能为空。
- Base64 会比原文件体积更大。
- 大文件不建议使用 Base64，建议用 multipart 上传。

---

## 5. 错题提取：文件上传

### `POST /api/ocr/wrong-questions`

### 作用

上传图片或 PDF，后端先进行 OCR 识别，再从识别文本中提取错题信息。

适合前端展示：

- 错题列表。
- 题号。
- 学生答案。
- 正确答案。
- 错误类型。
- 知识点。
- 原始识别文本。

### Content-Type

```text
multipart/form-data
```

### form-data 参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `file` | File | 是 | 图片或 PDF 文件 |
| `engine` | string | 否 | OCR 引擎：`tess4j`、`paddle`、`auto` |
| `language` | string | 否 | 识别语言 |
| `psm` | number | 否 | 范围 `0-13` |
| `minConfidence` | number | 否 | 范围 `0-100` |
| `preprocess` | boolean | 否 | 是否启用预处理 |

### 响应类型

```ts
interface WrongQuestionItem {
  questionId: string
  type: string
  content: string
  studentAnswer: string
  correctAnswer: string
  wrong: boolean
  errorType: string
  knowledgePoint: string
  imagePath: string | null
  markingStatus: string
  page: number
  confidence: number
  rawText: string
}

interface WrongQuestionResult {
  questions: WrongQuestionItem[]
  rawText: string
  pageCount: number
}
```

### 响应示例

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "questions": [
      {
        "questionId": "1",
        "type": "选择题",
        "content": "题目内容",
        "studentAnswer": "A",
        "correctAnswer": "B",
        "wrong": true,
        "errorType": "计算错误",
        "knowledgePoint": "分数运算",
        "imagePath": null,
        "markingStatus": "unknown",
        "page": 1,
        "confidence": 90.2,
        "rawText": "原始识别文本"
      }
    ],
    "rawText": "完整 OCR 文本",
    "pageCount": 1
  }
}
```

### 前端调用示例

```ts
export async function extractWrongQuestions(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('language', 'chi_sim+eng')
  formData.append('preprocess', 'true')

  const res = await fetch('/api/ocr/wrong-questions', {
    method: 'POST',
    body: formData
  })

  return await res.json()
}
```

### 前端注意事项

- 该接口基于 OCR 文本和规则提取，不是大模型语义分析。
- 结果质量依赖图片清晰度和错题格式。
- `imagePath` 当前通常为 `null`。
- `markingStatus` 当前默认是 `unknown`。
- 如果 OCR 文本中没有明确题号、学生答案、正确答案，部分字段可能为空。
- `wrong` 只有在识别到学生答案和正确答案时才会自动判断。

---

## 6. 错题提取：Base64

### `POST /api/ocr/wrong-questions/base64`

### 作用

识别 Base64 图片或 PDF，并提取错题结构化信息。

### Content-Type

```text
application/json
```

### 请求体示例

```json
{
  "data": "data:image/jpeg;base64,/9j/4AAQSkZJRg...",
  "options": {
    "engine": "tess4j",
    "language": "chi_sim+eng",
    "psm": 3,
    "minConfidence": 0,
    "preprocess": true
  }
}
```

### 响应结构

同 `POST /api/ocr/wrong-questions`：

```ts
ApiResponse<WrongQuestionResult>
```

### 前端注意事项

- 大文件建议使用 multipart。
- Base64 支持 Data URL 前缀。
- 错题提取结果依赖 OCR 识别质量和文本格式。

---

## 7. 异步 OCR 提交：文件上传

### `POST /api/ocr/recognize-async`

### 作用

提交 OCR 异步任务，立即返回任务 ID。前端后续通过任务查询接口轮询结果。

适合：

- 多页 PDF。
- 大图片。
- OCR 耗时较长的场景。
- 前端需要更好用户体验的场景。

### Content-Type

```text
multipart/form-data
```

### form-data 参数

| 字段 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `file` | File | 是 | 图片或 PDF 文件 |
| `engine` | string | 否 | OCR 引擎：`tess4j`、`paddle`、`auto` |
| `language` | string | 否 | 识别语言 |
| `psm` | number | 否 | 范围 `0-13` |
| `minConfidence` | number | 否 | 范围 `0-100` |
| `preprocess` | boolean | 否 | 是否启用预处理 |

### 响应类型

```ts
type OcrTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

interface OcrTaskResponse {
  taskId: string
  status: OcrTaskStatus
}
```

### 响应示例

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "8f2a3c55-2c51-4d91-8c2a-123456789abc",
    "status": "PENDING"
  }
}
```

### 前端调用示例

```ts
export async function submitOcrTask(file: File) {
  const formData = new FormData()
  formData.append('file', file)
  formData.append('language', 'chi_sim+eng')
  formData.append('preprocess', 'true')

  const res = await fetch('/api/ocr/recognize-async', {
    method: 'POST',
    body: formData
  })

  return await res.json()
}
```

### 前端注意事项

- 返回 `PENDING` 只代表任务已提交，不代表已经开始执行。
- 前端需要调用 `GET /api/ocr/tasks/{taskId}` 查询状态。
- 异步任务存储在后端内存中，服务重启后任务会丢失。

---

## 8. 异步 OCR 提交：Base64

### `POST /api/ocr/recognize-async/base64`

### 作用

提交 Base64 图片或 PDF 的异步 OCR 任务。

### Content-Type

```text
application/json
```

### 请求体示例

```json
{
  "data": "data:application/pdf;base64,JVBERi0xLjQK...",
  "options": {
    "engine": "tess4j",
    "language": "chi_sim+eng",
    "psm": 3,
    "minConfidence": 0,
    "preprocess": true
  }
}
```

### 响应结构

```ts
ApiResponse<OcrTaskResponse>
```

### 响应示例

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "8f2a3c55-2c51-4d91-8c2a-123456789abc",
    "status": "PENDING"
  }
}
```

### 前端注意事项

- 适合不方便使用 multipart 的场景。
- 大文件不推荐 Base64。
- 提交后仍然通过任务查询接口轮询。

---

## 9. 查询异步 OCR 任务

### `GET /api/ocr/tasks/{taskId}`

### 作用

查询异步 OCR 任务状态、结果或失败原因。

### Path 参数

| 参数 | 类型 | 必填 | 说明 |
|---|---|---:|---|
| `taskId` | string | 是 | 异步提交接口返回的任务 ID |

### 响应类型

```ts
interface OcrTaskStatusResponse {
  taskId: string
  status: 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'
  results: OcrResult[] | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}
```

### 响应示例：执行中

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "8f2a3c55-2c51-4d91-8c2a-123456789abc",
    "status": "RUNNING",
    "results": null,
    "errorMessage": null,
    "createdAt": "2026-06-01T00:00:00Z",
    "updatedAt": "2026-06-01T00:00:03Z"
  }
}
```

### 响应示例：成功

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "8f2a3c55-2c51-4d91-8c2a-123456789abc",
    "status": "SUCCEEDED",
    "results": [
      {
        "text": "识别文本",
        "blocks": [],
        "page": 1,
        "language": "chi_sim+eng",
        "durationMillis": 1234,
        "engine": "tess4j"
      }
    ],
    "errorMessage": null,
    "createdAt": "2026-06-01T00:00:00Z",
    "updatedAt": "2026-06-01T00:00:10Z"
  }
}
```

### 响应示例：失败

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "taskId": "8f2a3c55-2c51-4d91-8c2a-123456789abc",
    "status": "FAILED",
    "results": null,
    "errorMessage": "OCR processing failed",
    "createdAt": "2026-06-01T00:00:00Z",
    "updatedAt": "2026-06-01T00:00:10Z"
  }
}
```

### taskId 不存在

通常返回 HTTP `400`：

```json
{
  "success": false,
  "message": "OCR task not found",
  "data": null
}
```

### 前端轮询示例

```ts
export async function getOcrTask(taskId: string) {
  const res = await fetch(`/api/ocr/tasks/${taskId}`)
  return await res.json()
}

export async function pollOcrTask(
  taskId: string,
  onProgress?: (status: string) => void
) {
  while (true) {
    const response = await getOcrTask(taskId)

    if (!response.success) {
      throw new Error(response.message)
    }

    const task = response.data
    onProgress?.(task.status)

    if (task.status === 'SUCCEEDED') {
      return task.results
    }

    if (task.status === 'FAILED') {
      throw new Error(task.errorMessage || 'OCR task failed')
    }

    await new Promise(resolve => setTimeout(resolve, 1500))
  }
}
```

### 前端状态处理建议

| 状态 | 前端处理 |
|---|---|
| `PENDING` | 显示“等待处理” |
| `RUNNING` | 显示“识别中” |
| `SUCCEEDED` | 展示 `results` |
| `FAILED` | 展示 `errorMessage` |

---

## 10. 查询 OCR 引擎信息

### `GET /api/ocr/engines`

### 作用

返回当前后端支持的 OCR 引擎、默认语言、语言配置、模型路径状态等。

前端可用于：

- 初始化 OCR 参数下拉框。
- 判断 Paddle 是否启用。
- 展示默认语言。
- 检查后端 tessdata 是否存在。

### 请求参数

无。

### 响应示例

```json
{
  "success": true,
  "message": "ok",
  "data": {
    "defaultEngine": "tess4j",
    "availableEngines": ["paddle", "tess4j"],
    "defaultLanguage": "chi_sim+eng",
    "configuredLanguages": ["chi_sim", "eng"],
    "tessdataPathExists": true,
    "paddleEnabled": false,
    "paddleConfigured": false
  }
}
```

### 字段说明

| 字段 | 类型 | 说明 |
|---|---|---|
| `defaultEngine` | string | 默认 OCR 引擎 |
| `availableEngines` | string[] | 后端可用引擎列表 |
| `defaultLanguage` | string | 默认识别语言 |
| `configuredLanguages` | string[] | 当前配置语言 |
| `tessdataPathExists` | boolean | tessdata 路径是否存在 |
| `paddleEnabled` | boolean | Paddle 配置开关是否开启 |
| `paddleConfigured` | boolean | Paddle 是否实际配置可用 |

### 前端调用示例

```ts
export async function getOcrEngines() {
  const res = await fetch('/api/ocr/engines')
  return await res.json()
}
```

### 前端注意事项

- `availableEngines` 表示 Spring 中存在的引擎 Bean。
- 判断 Paddle 是否真正可用时，不应只看 `availableEngines`，还要看 `paddleEnabled` 和 `paddleConfigured`。
- `defaultLanguage` 可作为前端默认选项。

---

## 11. Swagger / OpenAPI

### `GET /swagger-ui.html`

### 作用

打开 Swagger UI 页面，用于接口调试。

访问地址：

```text
http://localhost:8080/swagger-ui.html
```

---

### `GET /v3/api-docs`

### 作用

返回 OpenAPI JSON，可用于前端生成 API 类型或接口 SDK。

访问地址：

```text
http://localhost:8080/v3/api-docs
```

---

## 12. Actuator 运维接口

这些接口由 Spring Boot Actuator 提供，主要用于健康检查和监控。

### `GET /actuator/health`

健康检查接口。前端一般不需要频繁调用，更多用于部署、监控、负载均衡。

### `GET /actuator/info`

返回服务 info 信息。当前项目未看到额外 info 配置，可能返回空对象或基础信息。

### `GET /actuator/metrics`

返回可用指标名称。

### `GET /actuator/metrics/{metric.name}`

查询具体指标详情。

示例：

```text
GET /actuator/metrics/jvm.memory.used
```

---

## 13. 前端建议封装类型

```ts
export interface ApiResponse<T> {
  success: boolean
  message: string
  data: T
}

export interface OcrOptions {
  engine?: 'tess4j' | 'paddle' | 'auto'
  language?: string
  psm?: number
  minConfidence?: number
  preprocess?: boolean
}

export interface OcrTextBlock {
  text: string
  x: number
  y: number
  width: number
  height: number
  confidence: number
  page: number
}

export interface OcrResult {
  text: string
  blocks: OcrTextBlock[]
  page: number
  language: string
  durationMillis: number
  engine: string
}

export interface WrongQuestionItem {
  questionId: string
  type: string
  content: string
  studentAnswer: string
  correctAnswer: string
  wrong: boolean
  errorType: string
  knowledgePoint: string
  imagePath: string | null
  markingStatus: string
  page: number
  confidence: number
  rawText: string
}

export interface WrongQuestionResult {
  questions: WrongQuestionItem[]
  rawText: string
  pageCount: number
}

export type OcrTaskStatus = 'PENDING' | 'RUNNING' | 'SUCCEEDED' | 'FAILED'

export interface OcrTaskResponse {
  taskId: string
  status: OcrTaskStatus
}

export interface OcrTaskStatusResponse {
  taskId: string
  status: OcrTaskStatus
  results: OcrResult[] | null
  errorMessage: string | null
  createdAt: string
  updatedAt: string
}

export interface OcrEngineInfo {
  defaultEngine: string
  availableEngines: string[]
  defaultLanguage: string
  configuredLanguages: string[]
  tessdataPathExists: boolean
  paddleEnabled: boolean
  paddleConfigured: boolean
}
```

---

## 14. 前端页面功能建议

### OCR 文件上传区

- 支持拖拽上传图片/PDF。
- 展示文件名、大小、类型。
- 支持选择同步或异步识别。

### OCR 参数区

- 引擎：从 `/api/ocr/engines` 获取。
- 语言：默认 `chi_sim+eng`。
- PSM：默认 `3`。
- 是否预处理：默认开启。

### 识别结果展示区

按页展示 `OcrResult[]`：

- 页码。
- 识别文本。
- 识别耗时。
- 引擎。
- 文本块列表。

### 错题展示区

表格展示 `WrongQuestionItem[]`，推荐列：

- 题号。
- 题型。
- 题目内容。
- 学生答案。
- 正确答案。
- 是否错误。
- 错误类型。
- 知识点。
- 页码。
- 置信度。

### 异步任务状态区

- 显示 `PENDING` / `RUNNING` / `SUCCEEDED` / `FAILED`。
- 成功后展示识别结果。
- 失败后展示错误信息。

---

## 15. 联调注意事项

### 15.1 跨域问题

当前项目未看到显式 CORS 配置。

如果 Vue 前端运行在 Vite 默认端口 `5173`，直接请求后端 `8080` 可能遇到跨域问题。

开发环境建议使用 Vite 代理：

```ts
// vite.config.ts
export default {
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/actuator': {
        target: 'http://localhost:8080',
        changeOrigin: true
      },
      '/v3': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  }
}
```

使用代理后，前端请求可以写成：

```ts
fetch('/api/ocr/recognize', {
  method: 'POST',
  body: formData
})
```

不需要写死：

```text
http://localhost:8080/api/ocr/recognize
```

### 15.2 Tesseract tessdata 依赖

OCR 依赖本地 Tesseract tessdata。

默认需要：

```text
./tessdata/chi_sim.traineddata
./tessdata/eng.traineddata
```

如果后端没有正确配置 tessdata，接口可能能启动，但实际 OCR 会失败。

### 15.3 默认 OCR 配置

默认配置包括：

| 配置 | 默认值 |
|---|---|
| engine | `tess4j` |
| language | `chi_sim+eng` |
| psm | `3` |
| timeout | `30` 秒 |
| pool-size | `4` |
| max-file-size | `20MB` |
| max-pdf-pages | `20` |
| max-image-width | `2500` |
| max-image-height | `2500` |

---

## 16. 推荐前端调用顺序

### 初始化页面

1. 调用 `GET /api/ocr/engines` 获取引擎和语言信息。
2. 初始化上传组件和参数表单。

### 同步识别流程

1. 用户选择文件。
2. 调用 `POST /api/ocr/recognize`。
3. 展示 loading。
4. 成功后展示 `data` 中的 OCR 结果。
5. 失败后展示 `message`。

### 异步识别流程

1. 用户选择文件。
2. 调用 `POST /api/ocr/recognize-async`。
3. 获取 `taskId`。
4. 每隔 1-2 秒调用 `GET /api/ocr/tasks/{taskId}`。
5. 状态为 `SUCCEEDED` 时展示 `results`。
6. 状态为 `FAILED` 时展示 `errorMessage`。

### 错题提取流程

1. 用户选择文件。
2. 调用 `POST /api/ocr/wrong-questions`。
3. 展示 loading。
4. 成功后以表格方式展示 `questions`。
5. 同时保留 `rawText` 用于查看 OCR 原始文本。
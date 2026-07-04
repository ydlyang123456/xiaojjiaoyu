# 小J教育

一个基于 AI 的学习检测系统，包含一个整合的 Android 应用（支持学生和家长两种角色）和 Flask 后端服务。

## 项目结构

```
StudyCheck/
├── backend/              # Flask 后端服务
│   ├── app.py            # 主应用入口
│   ├── models.py         # 数据库模型
│   ├── config.py         # 配置文件
│   ├── requirements.txt  # Python 依赖
│   ├── routes/           # API路由
│   │   ├── auth.py       # 用户认证
│   │   ├── question.py   # 题目管理
│   │   ├── search.py     # 搜题功能
│   │   ├── analysis.py   # AI分析
│   │   └── upload.py     # 文件上传
│   ├── services/         # 业务逻辑
│   │   └── ai_service.py # AI服务
│   └── uploads/          # 上传图片存储目录
└── student-app/          # Android 应用（学生端 + 家长端整合）
```

## 功能说明

应用支持两种角色，登录时自动切换 UI：

### 学生模式
- 用户注册/登录
- 拍照上传题目
- AI 搜题解题
- 搜题历史记录
- 知识词典
- 宠物系统
- 排行榜
- 个人中心

### 家长模式
- 用户注册/登录
- 绑定学生账号
- 查看学生搜题记录
- AI 学习分析报告（图表 + 文字）
- 历史分析记录
- 个人中心

## 后端部署（Linux 服务器）

### 1. 环境准备

```bash
# 更新系统
yum update -y

# 安装 Python3 和 pip
yum install -y python3 python3-pip

# 安装 Nginx（可选，用于反向代理）
yum install -y nginx
```

### 2. 上传代码

将 `backend` 目录上传到服务器，例如 `/opt/studycheck/backend`

```bash
mkdir -p /opt/studycheck
cd /opt/studycheck
# 上传 backend 目录到这里
```

### 3. 创建虚拟环境并安装依赖

```bash
cd /opt/studycheck/backend

python3 -m venv venv
source venv/bin/activate

pip install -r requirements.txt
```

### 4. 配置环境变量（可选）

```bash
export SECRET_KEY='your-secret-key'
export JWT_SECRET_KEY='your-jwt-secret-key'
export AI_API_KEY='your-ai-api-key'
export AI_API_URL='https://api.openai.com/v1/chat/completions'
export AI_MODEL='gpt-3.5-turbo'
```

### 5. 启动服务

#### 方式一：直接运行（开发测试用）

```bash
cd /opt/studycheck/backend
source venv/bin/activate
python app.py
```

#### 方式二：使用 Gunicorn（生产环境推荐）

```bash
pip install gunicorn

cd /opt/studycheck/backend
source venv/bin/activate

gunicorn -w 4 -b 0.0.0.0:5000 app:app
```

#### 方式三：使用 systemd 服务（推荐）

创建 `/etc/systemd/system/studycheck.service`：

```ini
[Unit]
Description=StudyCheck Flask App
After=network.target

[Service]
User=root
WorkingDirectory=/opt/studycheck/backend
Environment="PATH=/opt/studycheck/backend/venv/bin"
ExecStart=/opt/studycheck/backend/venv/bin/gunicorn -w 4 -b 0.0.0.0:5000 app:app
Restart=always

[Install]
WantedBy=multi-user.target
```

启动服务：

```bash
systemctl daemon-reload
systemctl start studycheck
systemctl enable studycheck
systemctl status studycheck
```

### 6. 配置 Nginx 反向代理（可选）

创建 `/etc/nginx/conf.d/studycheck.conf`：

```nginx
server {
    listen 80;
    server_name your-domain.com;

    client_max_body_size 20M;

    location / {
        proxy_pass http://127.0.0.1:5000;
        proxy_set_header Host $host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
    }

    location /api/uploads/ {
        alias /opt/studycheck/backend/uploads/;
    }
}
```

重启 Nginx：

```bash
nginx -t
systemctl restart nginx
```

## API 接口文档

### 基础地址
`http://47.107.109.101:5000`

### 认证接口

#### 注册
- POST `/api/auth/register`
- 参数：username, password, role (student/parent), nickname?, grade?

#### 登录
- POST `/api/auth/login`
- 参数：username, password, role?

#### 获取个人信息
- GET `/api/auth/profile`
- Header: Authorization: Bearer {token}

#### 绑定学生（家长端）
- POST `/api/auth/bind-student`
- 参数：student_username, relation_type?

#### 获取我的学生（家长端）
- GET `/api/auth/my-students`

### 题目接口

#### 上传题目
- POST `/api/questions/upload`
- form-data: image (file), subject?, description?

#### 获取题目列表
- GET `/api/questions/?page=1&per_page=20&student_id=`

#### 获取题目详情
- GET `/api/questions/{id}`

### 搜题接口

#### 搜题
- POST `/api/search/question`
- form-data: image?, query_text?, subject?, question_id?

#### 搜题记录
- GET `/api/search/records?page=1&per_page=20&student_id=&subject=`

#### 搜题详情
- GET `/api/search/{id}`

### AI分析接口

#### 生成分析报告
- POST `/api/analysis/generate`
- 参数：student_id, days, analysis_type

#### 分析历史
- GET `/api/analysis/history?page=1&per_page=10&student_id=`

#### 分析详情
- GET `/api/analysis/{id}`

## Android 应用开发

- 打开 Android Studio
- 导入 `student-app` 目录
- 修改 `ApiClient.kt` 中的 BASE_URL 为你的服务器地址
- 构建并安装到手机
- 登录时选择"学生"或"家长"角色，应用自动切换对应 UI

## AI 服务配置

系统通过环境变量配置 AI 服务，**请勿将 API Key 硬编码到代码中或提交到仓库**。

### 配置方式

在服务器上设置环境变量：

```bash
export AI_API_KEY='your-api-key'
export AI_API_URL='https://api.example.com/v1/chat/completions'
export AI_MODEL='your-model-name'
export AI_PROVIDER='your-provider'
```

### 支持的功能
1. **AI 解题**：输入题目文字或上传图片，AI 自动解答，包含：
   - 详细答案
   - 解题过程
   - 知识点总结
   - 难度等级评估

2. **AI 学习分析**：根据学生的搜题记录，生成完整的学习报告，包含：
   - 整体评估
   - 学科分布分析（饼图）
   - 薄弱环节
   - 优势方面
   - 学习建议
   - 学习习惯分析

### 切换 AI 平台
系统支持任何兼容 OpenAI 格式的 API。如需更换，设置环境变量：

```bash
export AI_API_KEY='your-api-key'
export AI_API_URL='https://api.openai.com/v1/chat/completions'
export AI_MODEL='gpt-3.5-turbo'
export AI_PROVIDER='openai'
```

支持的平台：
- DeepSeek（默认）
- OpenAI GPT
- 通义千问（兼容模式）
- 文心一言（兼容模式）
- 任何兼容 OpenAI Chat Completions 格式的 API

### 模拟模式
如需使用模拟数据（不消耗 API 额度），将 `AI_API_KEY` 设为空即可：
```bash
export AI_API_KEY=''
```

## 数据库

默认使用 SQLite，数据库文件为 `studycheck.db`。

如需使用 MySQL，请修改 `config.py` 中的 `SQLALCHEMY_DATABASE_URI`：

```python
SQLALCHEMY_DATABASE_URI = 'mysql+pymysql://user:password@localhost/studycheck'
```

并安装依赖：
```bash
pip install pymysql
```

## 快速测试

### 1. 启动后端

```bash
cd backend
pip install -r requirements.txt
python app.py
```

### 2. 测试接口

```bash
# 健康检查
curl http://localhost:5000/api/health

# 学生注册
curl -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"student1","password":"123456","role":"student","nickname":"小明"}'

# 家长注册
curl -X POST http://localhost:5000/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"parent1","password":"123456","role":"parent","nickname":"爸爸"}'
```

## 注意事项

1. 生产环境请修改默认的 SECRET_KEY 和 JWT_SECRET_KEY
2. 建议配置 HTTPS
3. 上传目录需要有写入权限
4. 建议定期备份数据库
5. 注意图片上传大小限制（默认 16MB）
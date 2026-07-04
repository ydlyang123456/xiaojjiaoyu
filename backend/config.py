import os
from datetime import timedelta

class Config:
    SECRET_KEY = os.environ.get('SECRET_KEY') or 'studycheck-secret-key-2024'
    SQLALCHEMY_DATABASE_URI = os.environ.get('DATABASE_URL') or 'sqlite:///studycheck.db'
    SQLALCHEMY_TRACK_MODIFICATIONS = False
    UPLOAD_FOLDER = os.path.join(os.path.dirname(os.path.abspath(__file__)), 'uploads')
    MAX_CONTENT_LENGTH = 16 * 1024 * 1024
    ALLOWED_EXTENSIONS = {'png', 'jpg', 'jpeg', 'gif', 'bmp', 'webp'}
    
    JWT_SECRET_KEY = os.environ.get('JWT_SECRET_KEY') or 'studycheck-jwt-secret-2024'
    JWT_ACCESS_TOKEN_EXPIRES = timedelta(days=7)
    
    AI_API_KEY = os.environ.get('AI_API_KEY') or 'sk-cn3wkq21glm555oqkxovi7lan3mk5gd2a7m0nsqh55fszkgk'
    AI_API_URL = os.environ.get('AI_API_URL') or 'https://api.xiaomimimo.com/v1/chat/completions'
    AI_MODEL = os.environ.get('AI_MODEL') or 'mimo-v2.5'
    AI_PROVIDER = os.environ.get('AI_PROVIDER') or 'xiaomi'
    
    APP_VERSION_CODE = 19
    APP_VERSION_NAME = '3.9.0'
    APP_CHANGELOG = 'v3.9.0 更新内容：\n1. 启动页Logo弹簧缩放动画，品牌更有质感\n2. 首页卡片依次入场动画，从上到下渐次出现\n3. 底部导航阴影增强，切换更流畅\n4. 所有列表RecyclerView添加上滑入场动画\n5. Fragment切换淡入上移动画，过渡更自然\n6. 首页搜索/宠物/排行榜按钮弹性触摸反馈'
    APK_DOWNLOAD_URL = '/static/apk/studycheck.apk'

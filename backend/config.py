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
    
    AI_API_KEY = os.environ.get('AI_API_KEY', '')
    AI_API_URL = os.environ.get('AI_API_URL', '')
    AI_MODEL = os.environ.get('AI_MODEL', '')
    AI_PROVIDER = os.environ.get('AI_PROVIDER', '')

    APP_VERSION_CODE = 21
    APP_VERSION_NAME = '4.1.0'
    APP_CHANGELOG = 'v4.1.0 更新内容：\n1. 新增用户反馈系统，可提交意见反馈\n2. 新增管理员账号，可查看所有反馈并回复\n3. 个人中心新增意见反馈入口\n4. 家长端支持管理员模式（不绑定学生）'
    APK_DOWNLOAD_URL = '/static/apk/studycheck.apk'

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

    APP_VERSION_CODE = 20
    APP_VERSION_NAME = '4.0.0'
    APP_CHANGELOG = 'v4.0.0 更新内容：\n1. 修复家长模式崩溃问题\n2. 修复家长模式AI分析无法打开的问题\n3. 整合项目结构，学生端和家长端合并为一个应用\n4. 登录时选择角色自动切换对应界面'
    APK_DOWNLOAD_URL = '/static/apk/studycheck.apk'

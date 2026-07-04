"""创建管理员账号脚本

用法：在服务器后端目录下执行：
    python create_admin.py

将创建/重置用户名为 admin 的管理员账号（角色 admin，不绑定学生）。
"""
from app import create_app
from models import db, User

ADMIN_USERNAME = 'admin'
ADMIN_PASSWORD = 'ydl028123456'
ADMIN_ROLE = 'admin'
ADMIN_NICKNAME = '管理员'


def create_or_reset_admin():
    app = create_app()
    with app.app_context():
        existing = User.query.filter_by(username=ADMIN_USERNAME).first()
        if existing:
            existing.set_password(ADMIN_PASSWORD)
            existing.role = ADMIN_ROLE
            existing.nickname = ADMIN_NICKNAME
            db.session.commit()
            print(f'[OK] 管理员账号已重置：{ADMIN_USERNAME}（id={existing.id}）')
        else:
            admin = User(
                username=ADMIN_USERNAME,
                role=ADMIN_ROLE,
                nickname=ADMIN_NICKNAME,
            )
            admin.set_password(ADMIN_PASSWORD)
            db.session.add(admin)
            db.session.commit()
            print(f'[OK] 管理员账号已创建：{ADMIN_USERNAME}（id={admin.id}）')

        print('登录信息：')
        print(f'  用户名：{ADMIN_USERNAME}')
        print(f'  密码：{ADMIN_PASSWORD}')
        print(f'  角色：{ADMIN_ROLE}')
        print('提示：管理员登录时请选择"家长"入口，进入后可查看所有用户反馈并回复。')


if __name__ == '__main__':
    create_or_reset_admin()

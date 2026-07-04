from flask import Blueprint, request, jsonify
from flask_jwt_extended import create_access_token, jwt_required, get_jwt_identity
from models import db, User, ParentStudentRelation

auth_bp = Blueprint('auth', __name__, url_prefix='/api/auth')


@auth_bp.route('/register', methods=['POST'])
def register():
    data = request.get_json()
    
    if not data or not data.get('username') or not data.get('password'):
        return jsonify({'code': 400, 'msg': '用户名和密码不能为空'}), 400
    
    role = data.get('role', 'student')
    if role not in ['student', 'parent', 'admin']:
        return jsonify({'code': 400, 'msg': '角色类型无效'}), 400
    
    if User.query.filter_by(username=data['username']).first():
        return jsonify({'code': 400, 'msg': '用户名已存在'}), 400
    
    user = User(
        username=data['username'],
        role=role,
        nickname=data.get('nickname', data['username']),
        grade=data.get('grade') if role == 'student' else None
    )
    user.set_password(data['password'])
    
    db.session.add(user)
    db.session.commit()
    
    access_token = create_access_token(identity=str(user.id))
    
    return jsonify({
        'code': 200,
        'msg': '注册成功',
        'data': {
            'token': access_token,
            'user': user.to_dict()
        }
    }), 200


@auth_bp.route('/login', methods=['POST'])
def login():
    data = request.get_json()
    
    if not data or not data.get('username') or not data.get('password'):
        return jsonify({'code': 400, 'msg': '用户名和密码不能为空'}), 400
    
    user = User.query.filter_by(username=data['username']).first()
    
    if not user or not user.check_password(data['password']):
        return jsonify({'code': 401, 'msg': '用户名或密码错误'}), 401
    
    role = data.get('role')
    if role and user.role != role:
        return jsonify({'code': 401, 'msg': f'该账号不是{role}账号'}), 401
    
    access_token = create_access_token(identity=str(user.id))
    
    return jsonify({
        'code': 200,
        'msg': '登录成功',
        'data': {
            'token': access_token,
            'user': user.to_dict()
        }
    }), 200


@auth_bp.route('/profile', methods=['GET'])
@jwt_required()
def profile():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': user.to_dict()
    }), 200


@auth_bp.route('/profile', methods=['PUT'])
@jwt_required()
def update_profile():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404
    
    data = request.get_json()
    
    if data.get('nickname'):
        user.nickname = data['nickname']
    if data.get('avatar'):
        user.avatar = data['avatar']
    if data.get('grade') and user.role == 'student':
        user.grade = data['grade']
    
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '更新成功',
        'data': user.to_dict()
    }), 200


@auth_bp.route('/bind-student', methods=['POST'])
@jwt_required()
def bind_student():
    parent_id = int(get_jwt_identity())
    parent = User.query.get(parent_id)
    
    if not parent or parent.role != 'parent':
        return jsonify({'code': 403, 'msg': '只有家长账号可以绑定学生'}), 403
    
    data = request.get_json()
    student_username = data.get('student_username')
    
    if not student_username:
        return jsonify({'code': 400, 'msg': '学生用户名不能为空'}), 400
    
    student = User.query.filter_by(username=student_username, role='student').first()
    
    if not student:
        return jsonify({'code': 404, 'msg': '学生用户不存在'}), 404
    
    existing = ParentStudentRelation.query.filter_by(
        parent_id=parent_id,
        student_id=student.id
    ).first()
    
    if existing:
        return jsonify({'code': 400, 'msg': '已经绑定该学生'}), 400
    
    relation = ParentStudentRelation(
        parent_id=parent_id,
        student_id=student.id,
        relation_type=data.get('relation_type', 'parent')
    )
    
    db.session.add(relation)
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '绑定成功',
        'data': relation.to_dict()
    }), 200


@auth_bp.route('/my-students', methods=['GET'])
@jwt_required()
def my_students():
    parent_id = int(get_jwt_identity())
    parent = User.query.get(parent_id)
    
    if not parent or parent.role != 'parent':
        return jsonify({'code': 403, 'msg': '只有家长账号可以查看'}), 403
    
    relations = ParentStudentRelation.query.filter_by(parent_id=parent_id).all()
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': [r.to_dict() for r in relations]
    }), 200

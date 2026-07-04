from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models import db, User, ParentStudentRelation, SearchRecord
from datetime import datetime, timedelta
import random
import string

parent_bp = Blueprint('parent', __name__, url_prefix='/api/parent')


@parent_bp.route('/students', methods=['GET'])
@jwt_required()
def get_students():
    parent_id = int(get_jwt_identity())
    parent = User.query.get(parent_id)
    
    if not parent or parent.role != 'parent':
        return jsonify({'code': 403, 'msg': '只有家长账号可以查看'}), 403
    
    relations = ParentStudentRelation.query.filter_by(parent_id=parent_id).all()
    
    students = [r.student.to_dict() for r in relations if r.student]
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': students
    }), 200


@parent_bp.route('/generate_unbind_code', methods=['POST'])
@jwt_required()
def generate_unbind_code():
    """家长生成6位解绑码"""
    parent_id = int(get_jwt_identity())
    parent = User.query.get(parent_id)
    
    if not parent or parent.role != 'parent':
        return jsonify({'code': 403, 'msg': '只有家长账号可以生成解绑码'}), 403
    
    unbind_code = ''.join(random.choices(string.digits, k=6))
    expires = datetime.utcnow() + timedelta(hours=24)
    
    parent.unbind_code = unbind_code
    parent.unbind_code_expires = expires
    
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '解绑码生成成功',
        'data': {
            'unbind_code': unbind_code,
            'expires_at': expires.isoformat(),
            'expires_in_hours': 24
        }
    }), 200


@parent_bp.route('/unbind', methods=['POST'])
@jwt_required()
def unbind_student():
    """解绑学生（需要解绑码）"""
    parent_id = int(get_jwt_identity())
    parent = User.query.get(parent_id)
    
    if not parent or parent.role != 'parent':
        return jsonify({'code': 403, 'msg': '只有家长账号可以解绑学生'}), 403
    
    data = request.get_json()
    student_id = data.get('student_id')
    unbind_code = data.get('unbind_code')
    
    if not unbind_code:
        return jsonify({'code': 400, 'msg': '解绑码不能为空'}), 400
    
    if parent.unbind_code != unbind_code:
        return jsonify({'code': 400, 'msg': '解绑码错误'}), 400
    
    if parent.unbind_code_expires and parent.unbind_code_expires < datetime.utcnow():
        return jsonify({'code': 400, 'msg': '解绑码已过期，请重新生成'}), 400
    
    if not student_id:
        return jsonify({'code': 400, 'msg': '学生ID不能为空'}), 400
    
    relation = ParentStudentRelation.query.filter_by(
        parent_id=parent_id,
        student_id=student_id
    ).first()
    
    if not relation:
        return jsonify({'code': 404, 'msg': '绑定关系不存在'}), 404
    
    db.session.delete(relation)
    parent.unbind_code = None
    parent.unbind_code_expires = None
    
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '解绑成功'
    }), 200


@parent_bp.route('/bind_with_code', methods=['POST'])
def bind_student_with_code():
    """学生使用解绑码绑定家长"""
    data = request.get_json()
    parent_username = data.get('parent_username')
    unbind_code = data.get('unbind_code')
    student_username = data.get('student_username')
    
    if not all([parent_username, unbind_code, student_username]):
        return jsonify({'code': 400, 'msg': '参数不完整'}), 400
    
    parent = User.query.filter_by(username=parent_username, role='parent').first()
    
    if not parent:
        return jsonify({'code': 404, 'msg': '家长用户不存在'}), 404
    
    if parent.unbind_code != unbind_code:
        return jsonify({'code': 400, 'msg': '解绑码错误'}), 400
    
    if parent.unbind_code_expires and parent.unbind_code_expires < datetime.utcnow():
        return jsonify({'code': 400, 'msg': '解绑码已过期，请让家长重新生成'}), 400
    
    student = User.query.filter_by(username=student_username, role='student').first()
    
    if not student:
        return jsonify({'code': 404, 'msg': '学生用户不存在'}), 404
    
    existing = ParentStudentRelation.query.filter_by(
        parent_id=parent.id,
        student_id=student.id
    ).first()
    
    if existing:
        return jsonify({'code': 400, 'msg': '已经绑定该家长'}), 400
    
    relation = ParentStudentRelation(
        parent_id=parent.id,
        student_id=student.id,
        relation_type=data.get('relation_type', 'parent')
    )
    
    db.session.add(relation)
    parent.unbind_code = None
    parent.unbind_code_expires = None
    
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '绑定成功',
        'data': relation.to_dict()
    }), 200


@parent_bp.route('/student/<int:student_id>/records', methods=['GET'])
@jwt_required()
def get_student_records(student_id):
    parent_id = int(get_jwt_identity())
    parent = User.query.get(parent_id)
    
    if not parent or parent.role != 'parent':
        return jsonify({'code': 403, 'msg': '只有家长账号可以查看'}), 403
    
    relation = ParentStudentRelation.query.filter_by(
        parent_id=parent_id,
        student_id=student_id
    ).first()
    
    if not relation:
        return jsonify({'code': 403, 'msg': '无权查看该学生记录'}), 403
    
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    
    query = SearchRecord.query.filter_by(student_id=student_id)
    query = query.order_by(SearchRecord.created_at.desc())
    pagination = query.paginate(page=page, per_page=per_page, error_out=False)
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': {
            'items': [r.to_dict() for r in pagination.items],
            'total': pagination.total,
            'page': page,
            'per_page': per_page,
            'pages': pagination.pages
        }
    }), 200


@parent_bp.route('/my/parent', methods=['GET'])
@jwt_required()
def get_my_parent():
    """学生获取已绑定的家长"""
    from flask_jwt_extended import jwt_required, get_jwt_identity
    
    student_id = int(get_jwt_identity())
    student = User.query.get(student_id)
    
    if not student or student.role != 'student':
        return jsonify({'code': 403, 'msg': '只有学生账号可以查看'}), 403
    
    relations = ParentStudentRelation.query.filter_by(student_id=student_id).all()
    
    parents = []
    for r in relations:
        if r.parent:
            parents.append({
                'id': r.parent.id,
                'username': r.parent.username,
                'nickname': r.parent.nickname,
                'relation_type': r.relation_type
            })
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': parents
    }), 200


@parent_bp.route('/my/unbind', methods=['POST'])
@jwt_required()
def student_unbind():
    """学生使用解绑码解绑家长"""
    from flask_jwt_extended import jwt_required, get_jwt_identity
    
    student_id = int(get_jwt_identity())
    student = User.query.get(student_id)
    
    if not student or student.role != 'student':
        return jsonify({'code': 403, 'msg': '只有学生账号可以解绑'}), 403
    
    data = request.get_json()
    parent_id = data.get('parent_id')
    unbind_code = data.get('unbind_code')
    
    if not unbind_code:
        return jsonify({'code': 400, 'msg': '解绑码不能为空'}), 400
    
    parent = User.query.get(parent_id)
    
    if not parent or parent.role != 'parent':
        return jsonify({'code': 404, 'msg': '家长用户不存在'}), 404
    
    if parent.unbind_code != unbind_code:
        return jsonify({'code': 400, 'msg': '解绑码错误'}), 400
    
    if parent.unbind_code_expires and parent.unbind_code_expires < datetime.utcnow():
        return jsonify({'code': 400, 'msg': '解绑码已过期，请让家长重新生成'}), 400
    
    relation = ParentStudentRelation.query.filter_by(
        parent_id=parent_id,
        student_id=student_id
    ).first()
    
    if not relation:
        return jsonify({'code': 404, 'msg': '绑定关系不存在'}), 404
    
    db.session.delete(relation)
    parent.unbind_code = None
    parent.unbind_code_expires = None
    
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '解绑成功'
    }), 200

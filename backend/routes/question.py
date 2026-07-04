import os
import uuid
from flask import Blueprint, request, jsonify, current_app, send_from_directory
from flask_jwt_extended import jwt_required, get_jwt_identity
from werkzeug.utils import secure_filename
from models import db, Question, User
from config import Config

question_bp = Blueprint('question', __name__, url_prefix='/api/questions')


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in Config.ALLOWED_EXTENSIONS


@question_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_question():
    student_id = int(get_jwt_identity())
    student = User.query.get(student_id)
    
    if not student or student.role != 'student':
        return jsonify({'code': 403, 'msg': '只有学生可以上传题目'}), 403
    
    if 'image' not in request.files:
        return jsonify({'code': 400, 'msg': '没有上传图片'}), 400
    
    file = request.files['image']
    
    if file.filename == '':
        return jsonify({'code': 400, 'msg': '没有选择文件'}), 400
    
    if file and allowed_file(file.filename):
        ext = file.filename.rsplit('.', 1)[1].lower()
        filename = f"{uuid.uuid4().hex}.{ext}"
        filepath = os.path.join(current_app.config['UPLOAD_FOLDER'], filename)
        file.save(filepath)
        
        image_url = f"/api/uploads/{filename}"
        
        question = Question(
            student_id=student_id,
            image_url=image_url,
            subject=request.form.get('subject'),
            description=request.form.get('description'),
            status='pending'
        )
        
        db.session.add(question)
        db.session.commit()
        
        return jsonify({
            'code': 200,
            'msg': '上传成功',
            'data': question.to_dict()
        }), 200
    
    return jsonify({'code': 400, 'msg': '不支持的文件格式'}), 400


@question_bp.route('/', methods=['GET'])
@jwt_required()
def list_questions():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404
    
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    student_id = request.args.get('student_id', type=int)
    
    if user.role == 'student':
        query = Question.query.filter_by(student_id=user_id)
    elif user.role == 'parent':
        if not student_id:
            return jsonify({'code': 400, 'msg': '请指定学生ID'}), 400
        
        from models import ParentStudentRelation
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=student_id
        ).first()
        
        if not relation:
            return jsonify({'code': 403, 'msg': '无权查看该学生的题目'}), 403
        
        query = Question.query.filter_by(student_id=student_id)
    else:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    query = query.order_by(Question.created_at.desc())
    pagination = query.paginate(page=page, per_page=per_page, error_out=False)
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': {
            'items': [q.to_dict(include_search_records=True) for q in pagination.items],
            'total': pagination.total,
            'page': page,
            'per_page': per_page,
            'pages': pagination.pages
        }
    }), 200


@question_bp.route('/<int:question_id>', methods=['GET'])
@jwt_required()
def get_question(question_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    question = Question.query.get(question_id)
    
    if not question:
        return jsonify({'code': 404, 'msg': '题目不存在'}), 404
    
    if user.role == 'student' and question.student_id != user_id:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    if user.role == 'parent':
        from models import ParentStudentRelation
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=question.student_id
        ).first()
        if not relation:
            return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': question.to_dict(include_search_records=True)
    }), 200


@question_bp.route('/<int:question_id>', methods=['DELETE'])
@jwt_required()
def delete_question(question_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    question = Question.query.get(question_id)
    
    if not question:
        return jsonify({'code': 404, 'msg': '题目不存在'}), 404
    
    if user.role == 'student' and question.student_id != user_id:
        return jsonify({'code': 403, 'msg': '无权删除'}), 403
    
    if question.image_url and question.image_url.startswith('/api/uploads/'):
        filename = question.image_url.replace('/api/uploads/', '')
        filepath = os.path.join(current_app.config['UPLOAD_FOLDER'], filename)
        if os.path.exists(filepath):
            os.remove(filepath)
    
    db.session.delete(question)
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '删除成功'
    }), 200

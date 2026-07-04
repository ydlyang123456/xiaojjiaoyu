import os
import uuid
from flask import Blueprint, request, jsonify, current_app
from flask_jwt_extended import jwt_required, get_jwt_identity
from werkzeug.utils import secure_filename
from models import db, SearchRecord, Question, User
from services.ai_service import ai_service
from config import Config

search_bp = Blueprint('search', __name__, url_prefix='/api/search')


def allowed_file(filename):
    return '.' in filename and \
           filename.rsplit('.', 1)[1].lower() in Config.ALLOWED_EXTENSIONS


@search_bp.route('/question', methods=['POST'])
@jwt_required()
def search_question():
    student_id = int(get_jwt_identity())
    student = User.query.get(student_id)
    
    if not student or student.role != 'student':
        return jsonify({'code': 403, 'msg': '只有学生可以搜题'}), 403
    
    query_text = request.form.get('query_text', '')
    subject = request.form.get('subject')
    question_id = request.form.get('question_id', type=int)
    query_image_url = None
    
    if 'image' in request.files:
        file = request.files['image']
        if file.filename != '' and allowed_file(file.filename):
            ext = file.filename.rsplit('.', 1)[1].lower()
            filename = f"search_{uuid.uuid4().hex}.{ext}"
            filepath = os.path.join(current_app.config['UPLOAD_FOLDER'], filename)
            file.save(filepath)
            query_image_url = f"/api/uploads/{filename}"
    
    ai_result = ai_service.solve_question(
        question_text=query_text,
        image_url=query_image_url,
        subject=subject
    )
    
    record = SearchRecord(
        student_id=student_id,
        question_id=question_id,
        query_text=query_text,
        query_image_url=query_image_url,
        subject=subject,
        ai_answer=ai_result['ai_answer'],
        ai_solution=ai_result['ai_solution'],
        knowledge_points=ai_result['knowledge_points'],
        difficulty=ai_result['difficulty']
    )
    
    db.session.add(record)
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '搜题成功',
        'data': record.to_dict()
    }), 200


@search_bp.route('/records', methods=['GET'])
@jwt_required()
def search_records():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404
    
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 20, type=int)
    student_id = request.args.get('student_id', type=int)
    subject = request.args.get('subject')
    
    if user.role == 'student':
        query = SearchRecord.query.filter_by(student_id=user_id)
    elif user.role == 'parent':
        if not student_id:
            return jsonify({'code': 400, 'msg': '请指定学生ID'}), 400
        
        from models import ParentStudentRelation
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=student_id
        ).first()
        
        if not relation:
            return jsonify({'code': 403, 'msg': '无权查看该学生的记录'}), 403
        
        query = SearchRecord.query.filter_by(student_id=student_id)
    else:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    if subject:
        query = query.filter_by(subject=subject)
    
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


@search_bp.route('/<int:record_id>', methods=['GET'])
@jwt_required()
def get_record(record_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    record = SearchRecord.query.get(record_id)
    
    if not record:
        return jsonify({'code': 404, 'msg': '记录不存在'}), 404
    
    if user.role == 'student' and record.student_id != user_id:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    if user.role == 'parent':
        from models import ParentStudentRelation
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=record.student_id
        ).first()
        if not relation:
            return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': record.to_dict()
    }), 200

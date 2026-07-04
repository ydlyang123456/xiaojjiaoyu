from datetime import datetime, timedelta
from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models import db, AIAnalysis, SearchRecord, User, ParentStudentRelation
from services.ai_service import ai_service

analysis_bp = Blueprint('analysis', __name__, url_prefix='/api/analysis')


@analysis_bp.route('/generate', methods=['POST'])
@jwt_required()
def generate_analysis():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404
    
    data = request.get_json() or {}
    student_id = data.get('student_id')
    days = data.get('days', 30)
    analysis_type = data.get('analysis_type', 'comprehensive')
    
    if user.role == 'parent':
        if not student_id:
            return jsonify({'code': 400, 'msg': '请指定学生ID'}), 400
        
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=student_id
        ).first()
        
        if not relation:
            return jsonify({'code': 403, 'msg': '无权分析该学生'}), 403
    elif user.role == 'student':
        student_id = user_id
    else:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    end_date = datetime.utcnow()
    start_date = end_date - timedelta(days=days)
    
    query = SearchRecord.query.filter(
        SearchRecord.student_id == student_id,
        SearchRecord.created_at >= start_date,
        SearchRecord.created_at <= end_date
    )
    records = query.order_by(SearchRecord.created_at.asc()).all()
    
    ai_result = ai_service.analyze_study(
        student_id=student_id,
        search_records=records,
        start_date=start_date,
        end_date=end_date
    )
    
    analysis = AIAnalysis(
        student_id=student_id,
        parent_id=user_id if user.role == 'parent' else None,
        analysis_type=analysis_type,
        overall_evaluation=ai_result['overall_evaluation'],
        subject_analysis=ai_result['subject_analysis'],
        weak_points=ai_result['weak_points'],
        strong_points=ai_result['strong_points'],
        suggestions=ai_result['suggestions'],
        study_habits=ai_result['study_habits'],
        statistics_data=ai_result['statistics_data'],
        start_date=start_date,
        end_date=end_date
    )
    
    db.session.add(analysis)
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '分析成功',
        'data': analysis.to_dict()
    }), 200


@analysis_bp.route('/student/<int:student_id>', methods=['POST'])
@jwt_required()
def generate_analysis_for_student(student_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404
    
    if user.role == 'parent':
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=student_id
        ).first()
        
        if not relation:
            return jsonify({'code': 403, 'msg': '无权分析该学生'}), 403
    elif user.role == 'student':
        if student_id != user_id:
            return jsonify({'code': 403, 'msg': '无权访问'}), 403
    else:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    days = request.args.get('days', 30, type=int)
    analysis_type = request.args.get('analysis_type', 'comprehensive')
    
    end_date = datetime.utcnow()
    start_date = end_date - timedelta(days=days)
    
    query = SearchRecord.query.filter(
        SearchRecord.student_id == student_id,
        SearchRecord.created_at >= start_date,
        SearchRecord.created_at <= end_date
    )
    records = query.order_by(SearchRecord.created_at.asc()).all()
    
    ai_result = ai_service.analyze_study(
        student_id=student_id,
        search_records=records,
        start_date=start_date,
        end_date=end_date
    )
    
    analysis = AIAnalysis(
        student_id=student_id,
        parent_id=user_id if user.role == 'parent' else None,
        analysis_type=analysis_type,
        overall_evaluation=ai_result['overall_evaluation'],
        subject_analysis=ai_result['subject_analysis'],
        weak_points=ai_result['weak_points'],
        strong_points=ai_result['strong_points'],
        suggestions=ai_result['suggestions'],
        study_habits=ai_result['study_habits'],
        statistics_data=ai_result['statistics_data'],
        start_date=start_date,
        end_date=end_date
    )
    
    db.session.add(analysis)
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '分析成功',
        'data': analysis.to_dict()
    }), 200


@analysis_bp.route('/history', methods=['GET'])
@jwt_required()
def analysis_history():
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404
    
    page = request.args.get('page', 1, type=int)
    per_page = request.args.get('per_page', 10, type=int)
    student_id = request.args.get('student_id', type=int)
    
    if user.role == 'parent':
        if not student_id:
            return jsonify({'code': 400, 'msg': '请指定学生ID'}), 400
        
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=student_id
        ).first()
        
        if not relation:
            return jsonify({'code': 403, 'msg': '无权查看该学生的分析'}), 403
        
        query = AIAnalysis.query.filter_by(student_id=student_id)
    elif user.role == 'student':
        query = AIAnalysis.query.filter_by(student_id=user_id)
    else:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    query = query.order_by(AIAnalysis.created_at.desc())
    pagination = query.paginate(page=page, per_page=per_page, error_out=False)
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': {
            'items': [a.to_dict() for a in pagination.items],
            'total': pagination.total,
            'page': page,
            'per_page': per_page,
            'pages': pagination.pages
        }
    }), 200


@analysis_bp.route('/<int:analysis_id>', methods=['GET'])
@jwt_required()
def get_analysis(analysis_id):
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)
    
    analysis = AIAnalysis.query.get(analysis_id)
    
    if not analysis:
        return jsonify({'code': 404, 'msg': '分析记录不存在'}), 404
    
    if user.role == 'student' and analysis.student_id != user_id:
        return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    if user.role == 'parent':
        relation = ParentStudentRelation.query.filter_by(
            parent_id=user_id,
            student_id=analysis.student_id
        ).first()
        if not relation:
            return jsonify({'code': 403, 'msg': '无权访问'}), 403
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': analysis.to_dict()
    }), 200

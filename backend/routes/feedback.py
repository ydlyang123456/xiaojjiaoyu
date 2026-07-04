from datetime import datetime
from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models import db, Feedback, User

feedback_bp = Blueprint('feedback', __name__, url_prefix='/api/feedback')


@feedback_bp.route('/', methods=['POST'])
@jwt_required()
def submit_feedback():
    """提交反馈"""
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)

    if not user:
        return jsonify({'code': 404, 'msg': '用户不存在'}), 404

    data = request.get_json()
    if not data or not data.get('content'):
        return jsonify({'code': 400, 'msg': '反馈内容不能为空'}), 400

    category = data.get('category', '其他')
    content = data.get('content', '').strip()
    contact = data.get('contact', '').strip()

    if len(content) > 1000:
        return jsonify({'code': 400, 'msg': '反馈内容不能超过1000字'}), 400

    feedback = Feedback(
        user_id=user_id,
        category=category,
        content=content,
        contact=contact
    )

    db.session.add(feedback)
    db.session.commit()

    return jsonify({
        'code': 200,
        'msg': '反馈提交成功，感谢您的意见！',
        'data': feedback.to_dict()
    }), 200


@feedback_bp.route('/my', methods=['GET'])
@jwt_required()
def my_feedback():
    """查看自己的反馈"""
    user_id = int(get_jwt_identity())

    feedbacks = Feedback.query.filter_by(user_id=user_id).order_by(Feedback.created_at.desc()).all()

    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': [f.to_dict() for f in feedbacks]
    }), 200


@feedback_bp.route('/all', methods=['GET'])
@jwt_required()
def all_feedback():
    """管理员查看所有反馈"""
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)

    if not user or user.role != 'admin':
        return jsonify({'code': 403, 'msg': '无权限'}), 403

    feedbacks = Feedback.query.order_by(Feedback.created_at.desc()).all()

    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': [f.to_dict(include_user=True) for f in feedbacks]
    }), 200


@feedback_bp.route('/<int:feedback_id>/reply', methods=['POST'])
@jwt_required()
def reply_feedback(feedback_id):
    """管理员回复反馈"""
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)

    if not user or user.role != 'admin':
        return jsonify({'code': 403, 'msg': '无权限'}), 403

    feedback = Feedback.query.get(feedback_id)
    if not feedback:
        return jsonify({'code': 404, 'msg': '反馈不存在'}), 404

    data = request.get_json()
    reply = data.get('reply', '').strip()

    if not reply:
        return jsonify({'code': 400, 'msg': '回复内容不能为空'}), 400

    feedback.admin_reply = reply
    feedback.status = 'replied'
    feedback.replied_at = datetime.utcnow()

    db.session.commit()

    return jsonify({
        'code': 200,
        'msg': '回复成功',
        'data': feedback.to_dict(include_user=True)
    }), 200


@feedback_bp.route('/<int:feedback_id>/status', methods=['PUT'])
@jwt_required()
def update_status(feedback_id):
    """管理员更新反馈状态"""
    user_id = int(get_jwt_identity())
    user = User.query.get(user_id)

    if not user or user.role != 'admin':
        return jsonify({'code': 403, 'msg': '无权限'}), 403

    feedback = Feedback.query.get(feedback_id)
    if not feedback:
        return jsonify({'code': 404, 'msg': '反馈不存在'}), 404

    data = request.get_json()
    status = data.get('status', 'pending')

    if status not in ['pending', 'processing', 'resolved', 'replied']:
        return jsonify({'code': 400, 'msg': '状态无效'}), 400

    feedback.status = status
    db.session.commit()

    return jsonify({
        'code': 200,
        'msg': '状态更新成功',
        'data': feedback.to_dict(include_user=True)
    }), 200

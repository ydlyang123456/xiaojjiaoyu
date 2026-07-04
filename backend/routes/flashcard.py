from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models import db, Flashcard
from datetime import datetime, date, timedelta

flashcard_bp = Blueprint('flashcard', __name__, url_prefix='/api/flashcard')

# 艾宾浩斯遗忘曲线复习间隔（天）
EBBINGHAUS_INTERVALS = [1, 2, 4, 7, 15, 30]
MAX_FREE_CARDS = 5


@flashcard_bp.route('/list', methods=['GET'])
@jwt_required()
def list_cards():
    student_id = int(get_jwt_identity())
    cards = Flashcard.query.filter_by(student_id=student_id).order_by(
        Flashcard.next_review_date.asc().nullsfirst(),
        Flashcard.created_at.desc()
    ).all()
    return jsonify({'code': 200, 'data': [c.to_dict() for c in cards]})


@flashcard_bp.route('/today', methods=['GET'])
@jwt_required()
def today_reviews():
    """获取今天需要复习的卡片"""
    student_id = int(get_jwt_identity())
    today = date.today()
    cards = Flashcard.query.filter(
        Flashcard.student_id == student_id,
        Flashcard.review_stage < 7,
        Flashcard.next_review_date <= today
    ).order_by(Flashcard.next_review_date.asc()).all()
    return jsonify({'code': 200, 'data': [c.to_dict() for c in cards]})


@flashcard_bp.route('/create', methods=['POST'])
@jwt_required()
def create_card():
    student_id = int(get_jwt_identity())
    data = request.get_json()
    question = data.get('question', '').strip()
    answer = data.get('answer', '').strip()

    if not question or not answer:
        return jsonify({'code': 400, 'msg': '问题和答案不能为空'}), 400
    if len(question) > 500:
        return jsonify({'code': 400, 'msg': '问题不能超过500字'}), 400

    # 免费5张卡片限制
    count = Flashcard.query.filter_by(student_id=student_id).count()
    if count >= MAX_FREE_CARDS:
        return jsonify({'code': 400, 'msg': f'免费用户最多创建{MAX_FREE_CARDS}张卡片'}), 400

    card = Flashcard(
        student_id=student_id,
        question=question,
        answer=answer,
        mastery=0,
        review_count=0,
        review_stage=0,
        next_review_date=date.today()
    )
    db.session.add(card)
    db.session.commit()
    return jsonify({'code': 200, 'msg': '创建成功', 'data': card.to_dict()})


@flashcard_bp.route('/<int:card_id>/review', methods=['POST'])
@jwt_required()
def review_card(card_id):
    student_id = int(get_jwt_identity())
    card = Flashcard.query.filter_by(id=card_id, student_id=student_id).first()
    if not card:
        return jsonify({'code': 404, 'msg': '卡片不存在'}), 404

    data = request.get_json() or {}
    remembered = data.get('remembered', True)  # 是否记住

    card.review_count += 1
    card.last_reviewed = datetime.utcnow()

    if remembered:
        # 答对：进入下一阶段
        if card.review_stage < len(EBBINGHAUS_INTERVALS):
            card.review_stage += 1
            interval = EBBINGHAUS_INTERVALS[card.review_stage - 1]
            card.next_review_date = date.today() + timedelta(days=interval)
        else:
            card.review_stage = 7  # 已掌握
            card.next_review_date = None

        card.mastery = min(100, card.mastery + 15)
    else:
        # 答错：回退一阶段
        if card.review_stage > 0:
            card.review_stage = max(0, card.review_stage - 1)
        card.next_review_date = date.today() + timedelta(days=1)
        card.mastery = max(0, card.mastery - 10)

    db.session.commit()
    return jsonify({'code': 200, 'msg': '复习完成', 'data': card.to_dict()})


@flashcard_bp.route('/<int:card_id>', methods=['DELETE'])
@jwt_required()
def delete_card(card_id):
    student_id = int(get_jwt_identity())
    card = Flashcard.query.filter_by(id=card_id, student_id=student_id).first()
    if not card:
        return jsonify({'code': 404, 'msg': '卡片不存在'}), 404
    db.session.delete(card)
    db.session.commit()
    return jsonify({'code': 200, 'msg': '删除成功'})


@flashcard_bp.route('/export', methods=['GET'])
@jwt_required()
def export_cards():
    """导出记忆卡片为HTML格式，方便离线查看"""
    student_id = int(get_jwt_identity())
    cards = Flashcard.query.filter_by(student_id=student_id).order_by(Flashcard.created_at.desc()).all()

    if not cards:
        return jsonify({'code': 400, 'msg': '没有可导出的卡片'}), 400

    html = '''<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>我的记忆卡片</title>
<style>
* { margin:0; padding:0; box-sizing:border-box; }
body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', sans-serif; background:#f2f2f7; padding:16px; }
h1 { text-align:center; color:#1d1d1f; margin:16px 0; font-size:22px; }
.card { background:#fff; border-radius:16px; padding:20px; margin-bottom:16px; box-shadow:0 2px 8px rgba(0,0,0,0.08); }
.card .q { font-size:16px; font-weight:600; color:#007aff; margin-bottom:10px; display:flex; align-items:flex-start; }
.card .q .num { background:#007aff; color:#fff; border-radius:50%; width:24px; height:24px; display:inline-flex; align-items:center; justify-content:center; font-size:12px; margin-right:10px; flex-shrink:0; }
.card .a { font-size:14px; color:#3c3c43; line-height:1.6; padding:12px; background:#f7f7fa; border-radius:10px; border-left:3px solid #007aff; }
.card .meta { display:flex; justify-content:space-between; margin-top:10px; font-size:11px; color:#8e8e93; }
.mastery-bar { height:4px; background:#e5e5ea; border-radius:2px; margin-top:8px; overflow:hidden; }
.mastery-fill { height:100%; background:#34c759; border-radius:2px; transition:width 0.3s; }
.stats { text-align:center; margin:16px 0; color:#8e8e93; font-size:13px; }
@media print { body { background:#fff; } .card { break-inside:avoid; box-shadow:none; border:1px solid #e5e5ea; } }
</style>
</head>
<body>
<h1>📝 我的记忆卡片</h1>
<div class="stats">共 ''' + str(len(cards)) + ''' 张卡片 | 导出时间：''' + datetime.now().strftime('%Y-%m-%d %H:%M') + '''</div>
'''
    for i, card in enumerate(cards, 1):
        stage_names = ['新学', '1天后', '2天后', '4天后', '7天后', '15天后', '30天后', '已掌握']
        stage = stage_names[min(card.review_stage, 7)]
        html += f'''
<div class="card">
    <div class="q"><span class="num">{i}</span>{card.question}</div>
    <div class="a">{card.answer}</div>
    <div class="mastery-bar"><div class="mastery-fill" style="width:{card.mastery}%"></div></div>
    <div class="meta">
        <span>熟练度 {card.mastery}%</span>
        <span>{stage} · 复习{card.review_count}次</span>
    </div>
</div>
'''
    html += '''
</body>
</html>'''
    return html, 200, {'Content-Type': 'text/html; charset=utf-8'}
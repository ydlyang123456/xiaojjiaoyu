from flask import Blueprint, request, jsonify, send_file, current_app
from flask_jwt_extended import jwt_required, get_jwt_identity
from services.ai_service import ai_service, query_vocabulary, synthesize_speech
import io
import hashlib
import os

knowledge_bp = Blueprint('knowledge', __name__, url_prefix='/api/knowledge')


@knowledge_bp.route('/categories', methods=['GET'])
def get_categories():
    categories = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治']
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': categories
    }), 200


@knowledge_bp.route('/list', methods=['GET'])
def get_knowledge_list():
    subject = request.args.get('subject', '')
    keyword = request.args.get('keyword', '')
    
    items = []
    if keyword:
        items = [
            {
                'id': 'ai_query',
                'title': f'查询：{keyword}',
                'subject': subject or 'AI智能解答',
                'summary': '点击使用AI智能查询详细解释'
            }
        ]
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': {
            'items': items,
            'total': len(items)
        }
    }), 200


@knowledge_bp.route('/<knowledge_id>', methods=['GET'])
def get_knowledge_detail(knowledge_id):
    if knowledge_id == 'ai_query':
        return jsonify({
            'code': 400,
            'msg': '请使用搜索功能查询'
        }), 400
    
    return jsonify({
        'code': 404,
        'msg': '知识条目不存在，请使用AI搜索功能查询'
    }), 404


@knowledge_bp.route('/query', methods=['POST'])
@jwt_required(optional=True)
def query_knowledge():
    data = request.get_json(silent=True) or {}
    query = data.get('query', '').strip() or request.form.get('query', '').strip()
    
    if not query:
        return jsonify({
            'code': 400,
            'msg': '查询内容不能为空'
        }), 400
    
    if len(query) > 200:
        return jsonify({
            'code': 400,
            'msg': '查询内容不能超过200字'
        }), 400
    
    result = ai_service.knowledge_query(query)
    
    if result.get('blocked'):
        return jsonify({
            'code': 403,
            'msg': result.get('reason', '抱歉，知识词典仅支持学科知识查询，请输入学科相关问题')
        }), 403
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': result
    }), 200


@knowledge_bp.route('/vocabulary', methods=['POST'])
@jwt_required(optional=True)
def vocabulary_query():
    data = request.get_json(silent=True) or {}
    word = data.get('word', '').strip() or request.form.get('word', '').strip()
    
    if not word:
        return jsonify({
            'code': 400,
            'msg': '请输入要查询的单词'
        }), 400
    
    result = query_vocabulary(word)
    
    if result.get('error'):
        return jsonify({
            'code': 400,
            'msg': result['error']
        }), 400
    
    return jsonify({
        'code': 200,
        'msg': '成功',
        'data': result
    }), 200


@knowledge_bp.route('/tts', methods=['GET'])
@jwt_required(optional=True)
def text_to_speech():
    text = request.args.get('text', '').strip()
    voice = request.args.get('voice', 'Mia')
    word_hash = request.args.get('hash', '')
    
    if not text:
        return jsonify({
            'code': 400,
            'msg': '缺少文本参数'
        }), 400
    
    if not word_hash:
        word_hash = hashlib.md5(f"{text}_{voice}".encode()).hexdigest()[:12]
    
    cache_dir = os.path.join(current_app.root_path, '..', 'static', 'tts_cache')
    os.makedirs(cache_dir, exist_ok=True)
    cache_path = os.path.join(cache_dir, f"{word_hash}.wav")
    
    if os.path.exists(cache_path):
        return send_file(cache_path, mimetype='audio/wav')
    
    audio_bytes = synthesize_speech(text, voice=voice, format='wav')
    
    if audio_bytes:
        with open(cache_path, 'wb') as f:
            f.write(audio_bytes)
        return send_file(io.BytesIO(audio_bytes), mimetype='audio/wav')
    else:
        return jsonify({
            'code': 500,
            'msg': '语音合成失败'
        }), 500

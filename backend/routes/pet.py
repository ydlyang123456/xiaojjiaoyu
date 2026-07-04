from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models import db, Pet
from datetime import datetime, date

pet_bp = Blueprint('pet', __name__, url_prefix='/api/pet')

PET_SKINS = {
    'default': {'name': '经典', 'icon': '🐱', 'cost': 0},
    'dog': {'name': '柴犬', 'icon': '🐶', 'cost': 0},
    'bunny': {'name': '兔子', 'icon': '🐰', 'cost': 0},
    'fox': {'name': '狐狸', 'icon': '🦊', 'cost': 50},
    'panda': {'name': '熊猫', 'icon': '🐼', 'cost': 50},
    'unicorn': {'name': '独角兽', 'icon': '🦄', 'cost': 100},
    'dragon': {'name': '龙', 'icon': '🐲', 'cost': 100},
    'alien': {'name': '外星人', 'icon': '👽', 'cost': 200}
}

@pet_bp.route('/info', methods=['GET'])
@jwt_required()
def get_pet_info():
    student_id = get_jwt_identity()
    pet = Pet.query.filter_by(student_id=int(student_id)).first()
    
    if not pet:
        pet = Pet(student_id=int(student_id))
        db.session.add(pet)
        db.session.commit()
    
    pet_data = pet.to_dict()
    pet_data['skins'] = PET_SKINS
    
    today = datetime.utcnow().date()
    if pet.last_feed_date != today:
        pet.hunger = max(0, pet.hunger - 10)
        pet.happiness = max(0, pet.happiness - 5)
        db.session.commit()
    
    return jsonify({'code': 200, 'data': pet_data})


@pet_bp.route('/feed', methods=['POST'])
@jwt_required()
def feed_pet():
    student_id = get_jwt_identity()
    pet = Pet.query.filter_by(student_id=int(student_id)).first()
    
    if not pet:
        return jsonify({'code': 404, 'msg': '宠物不存在'}), 404
    
    today = datetime.utcnow().date()
    if pet.last_feed_date == today:
        return jsonify({'code': 400, 'msg': '今天已经喂过了，明天再来吧'}), 400
    
    pet.hunger = min(100, pet.hunger + 30)
    pet.happiness = min(100, pet.happiness + 15)
    pet.exp += 10
    pet.last_feed_date = today
    
    while pet.exp >= pet.max_exp:
        pet.exp -= pet.max_exp
        pet.level += 1
        pet.max_exp = int(pet.max_exp * 1.3)
    
    db.session.commit()
    return jsonify({'code': 200, 'msg': '喂食成功！', 'data': pet.to_dict()})


@pet_bp.route('/change-skin', methods=['POST'])
@jwt_required()
def change_skin():
    student_id = get_jwt_identity()
    data = request.get_json()
    skin_key = data.get('skin')
    
    if skin_key not in PET_SKINS:
        return jsonify({'code': 400, 'msg': '皮肤不存在'}), 400
    
    pet = Pet.query.filter_by(student_id=int(student_id)).first()
    if not pet:
        return jsonify({'code': 404, 'msg': '宠物不存在'}), 404
    
    skin_info = PET_SKINS[skin_key]
    if pet.level * 10 < skin_info['cost']:
        return jsonify({'code': 400, 'msg': f'等级不够，需要{skin_info["cost"]}级'}), 400
    
    pet.skin = skin_key
    db.session.commit()
    return jsonify({'code': 200, 'msg': '换装成功！', 'data': pet.to_dict()})


@pet_bp.route('/rename', methods=['POST'])
@jwt_required()
def rename_pet():
    student_id = get_jwt_identity()
    data = request.get_json()
    new_name = data.get('name', '').strip()
    
    if not new_name or len(new_name) > 20:
        return jsonify({'code': 400, 'msg': '名字长度为1-20个字符'}), 400
    
    pet = Pet.query.filter_by(student_id=int(student_id)).first()
    if not pet:
        return jsonify({'code': 404, 'msg': '宠物不存在'}), 404
    
    pet.name = new_name
    db.session.commit()
    return jsonify({'code': 200, 'msg': '改名成功！', 'data': pet.to_dict()})


@pet_bp.route('/play', methods=['POST'])
@jwt_required()
def play_with_pet():
    student_id = get_jwt_identity()
    pet = Pet.query.filter_by(student_id=int(student_id)).first()
    
    if not pet:
        return jsonify({'code': 404, 'msg': '宠物不存在'}), 404
    
    pet.happiness = min(100, pet.happiness + 20)
    pet.hunger = max(0, pet.hunger - 10)
    pet.exp += 5
    
    while pet.exp >= pet.max_exp:
        pet.exp -= pet.max_exp
        pet.level += 1
        pet.max_exp = int(pet.max_exp * 1.3)
    
    db.session.commit()
    return jsonify({'code': 200, 'msg': '陪玩成功！', 'data': pet.to_dict()})


@pet_bp.route('/leaderboard', methods=['GET'])
@jwt_required()
def leaderboard():
    pets = Pet.query.order_by(Pet.level.desc(), Pet.exp.desc()).limit(50).all()
    
    result = []
    for i, pet in enumerate(pets):
        student = pet.student
        result.append({
            'rank': i + 1,
            'pet_name': pet.name,
            'pet_skin': pet.skin,
            'level': pet.level,
            'exp': pet.exp,
            'total_corrected': pet.total_corrected,
            'student_name': student.nickname or student.username if student else '未知'
        })
    
    return jsonify({'code': 200, 'data': result})

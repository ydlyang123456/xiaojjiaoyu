from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models import db, Correction, SearchRecord, Pet
from datetime import datetime

correction_bp = Blueprint('correction', __name__, url_prefix='/api/correction')

@correction_bp.route('/upload', methods=['POST'])
@jwt_required()
def upload_correction():
    student_id = get_jwt_identity()
    data = request.form if request.form else request.get_json()
    
    record_id = data.get('search_record_id')
    corrected_text = data.get('corrected_text', '')
    is_correct = data.get('is_correct', 'false') == 'true'
    
    if not record_id:
        return jsonify({'code': 400, 'msg': '缺少搜题记录ID'}), 400
    
    record = SearchRecord.query.get(record_id)
    if not record:
        return jsonify({'code': 404, 'msg': '记录不存在'}), 404
    
    corrected_image_url = None
    if 'image' in request.files:
        image_file = request.files['image']
        if image_file.filename:
            import os
            from flask import current_app
            ext = image_file.filename.rsplit('.', 1)[-1].lower() if '.' in image_file.filename else 'jpg'
            filename = f"correction_{student_id}_{int(datetime.utcnow().timestamp())}.{ext}"
            upload_dir = current_app.config.get('UPLOAD_FOLDER', 'uploads')
            os.makedirs(upload_dir, exist_ok=True)
            image_file.save(os.path.join(upload_dir, filename))
            corrected_image_url = f"/api/uploads/{filename}"
    
    correction = Correction(
        student_id=int(student_id),
        search_record_id=int(record_id),
        corrected_image_url=corrected_image_url,
        corrected_text=corrected_text,
        is_correct=is_correct
    )
    db.session.add(correction)
    
    if is_correct:
        pet = Pet.query.filter_by(student_id=int(student_id)).first()
        if not pet:
            pet = Pet(student_id=int(student_id))
            db.session.add(pet)
        
        pet.total_corrected += 1
        exp_gain = 20
        if record.difficulty == '困难':
            exp_gain = 50
        elif record.difficulty == '中等':
            exp_gain = 30
        
        pet.exp += exp_gain
        while pet.exp >= pet.max_exp:
            pet.exp -= pet.max_exp
            pet.level += 1
            pet.max_exp = int(pet.max_exp * 1.3)
        
        pet.hunger = max(0, pet.hunger - 5)
        pet.happiness = min(100, pet.happiness + 10)
        
        today = datetime.utcnow().date()
        if pet.last_feed_date != today:
            if pet.last_feed_date and (today - pet.last_feed_date).days == 1:
                pet.streak_days += 1
            elif pet.last_feed_date != today:
                pet.streak_days = 1
            pet.last_feed_date = today
    
    db.session.commit()
    
    return jsonify({
        'code': 200,
        'msg': '订正提交成功' + ('，宠物获得了经验！' if is_correct else ''),
        'data': correction.to_dict()
    })


@correction_bp.route('/list/<int:record_id>', methods=['GET'])
@jwt_required()
def get_corrections(record_id):
    student_id = get_jwt_identity()
    corrections = Correction.query.filter_by(
        student_id=int(student_id),
        search_record_id=record_id
    ).order_by(Correction.created_at.desc()).all()
    
    return jsonify({
        'code': 200,
        'data': [c.to_dict() for c in corrections]
    })

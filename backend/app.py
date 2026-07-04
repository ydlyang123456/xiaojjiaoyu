import os
from flask import Flask, jsonify, render_template, send_from_directory
from flask_cors import CORS
from flask_jwt_extended import JWTManager

from config import Config
from models import db
from services.ai_service import ai_service

from routes.auth import auth_bp
from routes.question import question_bp
from routes.search import search_bp
from routes.analysis import analysis_bp
from routes.upload import upload_bp
from routes.parent import parent_bp
from routes.knowledge import knowledge_bp
from routes.correction import correction_bp
from routes.pet import pet_bp
from routes.flashcard import flashcard_bp
from routes.battle import battle_bp
from routes.feedback import feedback_bp


def create_app():
    app = Flask(__name__)
    app.config.from_object(Config)

    os.makedirs(app.config['UPLOAD_FOLDER'], exist_ok=True)

    CORS(app, resources={r"/api/*": {"origins": "*"}})

    db.init_app(app)
    JWTManager(app)
    ai_service.init_app(app)

    app.register_blueprint(auth_bp)
    app.register_blueprint(question_bp)
    app.register_blueprint(search_bp)
    app.register_blueprint(analysis_bp)
    app.register_blueprint(upload_bp)
    app.register_blueprint(parent_bp)
    app.register_blueprint(knowledge_bp)
    app.register_blueprint(correction_bp)
    app.register_blueprint(pet_bp)
    app.register_blueprint(flashcard_bp)
    app.register_blueprint(battle_bp)
    app.register_blueprint(feedback_bp)

    with app.app_context():
        db.create_all()

    @app.route('/')
    def index():
        return render_template('index.html')

    @app.route('/student')
    def student_page():
        return render_template('student.html')

    @app.route('/parent')
    def parent_page():
        return render_template('parent.html')

    @app.route('/manifest.json')
    def manifest():
        return send_from_directory(app.config.get('STATIC_FOLDER', os.path.join(os.path.dirname(__file__), 'static')), 'manifest.json')

    @app.route('/sw.js')
    def service_worker():
        return send_from_directory(app.config.get('STATIC_FOLDER', os.path.join(os.path.dirname(__file__), 'static')), 'sw.js')

    @app.route('/api/health')
    def health_check():
        return jsonify({
            'code': 200,
            'msg': 'StudyCheck API is running',
            'data': {
                'status': 'ok',
                'version': '1.0.0'
            }
        })

    @app.route('/api/app/version')
    def app_version():
        return jsonify({
            'code': 200,
            'msg': '成功',
            'data': {
                'version_code': app.config.get('APP_VERSION_CODE', 1),
                'version_name': app.config.get('APP_VERSION_NAME', '1.0.0'),
                'changelog': app.config.get('APP_CHANGELOG', ''),
                'download_url': app.config.get('APK_DOWNLOAD_URL', '/static/apk/studycheck.apk'),
                'force_update': False
            }
        })

    @app.errorhandler(404)
    def not_found(error):
        return jsonify({'code': 404, 'msg': '接口不存在'}), 404

    @app.errorhandler(500)
    def internal_error(error):
        return jsonify({'code': 500, 'msg': '服务器内部错误'}), 500

    return app


if __name__ == '__main__':
    app = create_app()
    app.run(host='0.0.0.0', port=5000, debug=True)

application = create_app()

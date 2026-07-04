from flask import Blueprint, send_from_directory, current_app
import os

upload_bp = Blueprint('upload', __name__, url_prefix='/api/uploads')


@upload_bp.route('/<filename>')
def uploaded_file(filename):
    return send_from_directory(current_app.config['UPLOAD_FOLDER'], filename)

from datetime import datetime
from flask_sqlalchemy import SQLAlchemy
from werkzeug.security import generate_password_hash, check_password_hash

db = SQLAlchemy()


class User(db.Model):
    __tablename__ = 'users'

    id = db.Column(db.Integer, primary_key=True)
    username = db.Column(db.String(80), unique=True, nullable=False)
    password_hash = db.Column(db.String(256), nullable=False)
    role = db.Column(db.String(20), nullable=False)
    nickname = db.Column(db.String(80))
    avatar = db.Column(db.String(256))
    grade = db.Column(db.String(50))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    unbind_code = db.Column(db.String(10))
    unbind_code_expires = db.Column(db.DateTime)

    questions = db.relationship('Question', backref='student', lazy='dynamic',
                                foreign_keys='Question.student_id')
    search_records = db.relationship('SearchRecord', backref='student', lazy='dynamic',
                                     foreign_keys='SearchRecord.student_id')

    def set_password(self, password):
        self.password_hash = generate_password_hash(password)

    def check_password(self, password):
        return check_password_hash(self.password_hash, password)

    def to_dict(self):
        return {
            'id': self.id,
            'username': self.username,
            'role': self.role,
            'nickname': self.nickname,
            'avatar': self.avatar,
            'grade': self.grade,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }


class ParentStudentRelation(db.Model):
    __tablename__ = 'parent_student_relations'

    id = db.Column(db.Integer, primary_key=True)
    parent_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    relation_type = db.Column(db.String(20), default='parent')
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    parent = db.relationship('User', foreign_keys=[parent_id], backref='student_relations')
    student = db.relationship('User', foreign_keys=[student_id], backref='parent_relations')

    def to_dict(self):
        return {
            'id': self.id,
            'parent_id': self.parent_id,
            'student_id': self.student_id,
            'relation_type': self.relation_type,
            'student': self.student.to_dict() if self.student else None
        }


class Question(db.Model):
    __tablename__ = 'questions'

    id = db.Column(db.Integer, primary_key=True)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    image_url = db.Column(db.String(256), nullable=False)
    subject = db.Column(db.String(50))
    description = db.Column(db.Text)
    status = db.Column(db.String(20), default='pending')
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)

    search_records = db.relationship('SearchRecord', backref='question', lazy='dynamic')

    def to_dict(self, include_search_records=False):
        data = {
            'id': self.id,
            'student_id': self.student_id,
            'image_url': self.image_url,
            'subject': self.subject,
            'description': self.description,
            'status': self.status,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }
        if include_search_records:
            data['search_records'] = [r.to_dict() for r in self.search_records]
        return data


class SearchRecord(db.Model):
    __tablename__ = 'search_records'

    id = db.Column(db.Integer, primary_key=True)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    question_id = db.Column(db.Integer, db.ForeignKey('questions.id'))
    query_text = db.Column(db.Text)
    query_image_url = db.Column(db.String(256))
    subject = db.Column(db.String(50))
    ai_answer = db.Column(db.Text)
    ai_solution = db.Column(db.Text)
    knowledge_points = db.Column(db.Text)
    difficulty = db.Column(db.String(20))
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        return {
            'id': self.id,
            'student_id': self.student_id,
            'question_id': self.question_id,
            'query_text': self.query_text,
            'query_image_url': self.query_image_url,
            'subject': self.subject,
            'ai_answer': self.ai_answer,
            'ai_solution': self.ai_solution,
            'knowledge_points': self.knowledge_points,
            'difficulty': self.difficulty,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }


class AIAnalysis(db.Model):
    __tablename__ = 'ai_analyses'

    id = db.Column(db.Integer, primary_key=True)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    parent_id = db.Column(db.Integer, db.ForeignKey('users.id'))
    analysis_type = db.Column(db.String(50), default='comprehensive')
    overall_evaluation = db.Column(db.Text)
    subject_analysis = db.Column(db.Text)
    weak_points = db.Column(db.Text)
    strong_points = db.Column(db.Text)
    suggestions = db.Column(db.Text)
    study_habits = db.Column(db.Text)
    statistics_data = db.Column(db.Text)
    start_date = db.Column(db.DateTime)
    end_date = db.Column(db.DateTime)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)

    def to_dict(self):
        import json
        subject_dist = {}
        difficulty_dist = {}
        overall_score = 0
        
        if self.statistics_data:
            try:
                stats = json.loads(self.statistics_data)
                if isinstance(stats, dict):
                    if 'subject_stats' in stats:
                        subject_dist = stats['subject_stats']
                    elif 'subject_distribution' in stats:
                        subject_dist = stats['subject_distribution']
                    elif 'by_subject' in stats:
                        subject_dist = stats['by_subject']
                    
                    if 'difficulty_stats' in stats:
                        difficulty_dist = stats['difficulty_stats']
                    elif 'difficulty_distribution' in stats:
                        difficulty_dist = stats['difficulty_distribution']
                    elif 'by_difficulty' in stats:
                        difficulty_dist = stats['by_difficulty']
                    
                    overall_score = stats.get('overall_score', 0)
            except:
                pass
        
        if not overall_score and self.overall_evaluation:
            try:
                import re
                match = re.search(r'(\d+)\s*分', self.overall_evaluation)
                if match:
                    overall_score = int(match.group(1))
            except:
                pass
        
        return {
            'id': self.id,
            'student_id': self.student_id,
            'parent_id': self.parent_id,
            'analysis_type': self.analysis_type,
            'overall_evaluation': self.overall_evaluation,
            'overall_comment': self.overall_evaluation,
            'overall_score': overall_score,
            'subject_analysis': self.subject_analysis,
            'weak_points': self.weak_points,
            'strong_points': self.strong_points,
            'suggestions': self.suggestions,
            'study_habits': self.study_habits,
            'learning_habits': self.study_habits,
            'statistics_data': self.statistics_data,
            'subject_distribution': subject_dist,
            'difficulty_distribution': difficulty_dist,
            'start_date': self.start_date.isoformat() if self.start_date else None,
            'end_date': self.end_date.isoformat() if self.end_date else None,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }


class Correction(db.Model):
    __tablename__ = 'corrections'
    
    id = db.Column(db.Integer, primary_key=True)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    search_record_id = db.Column(db.Integer, db.ForeignKey('search_records.id'), nullable=False)
    corrected_image_url = db.Column(db.String(256))
    corrected_text = db.Column(db.Text)
    is_correct = db.Column(db.Boolean, default=False)
    feedback = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    student = db.relationship('User', backref='corrections')
    search_record = db.relationship('SearchRecord', backref='corrections')
    
    def to_dict(self):
        return {
            'id': self.id,
            'student_id': self.student_id,
            'search_record_id': self.search_record_id,
            'corrected_image_url': self.corrected_image_url,
            'corrected_text': self.corrected_text,
            'is_correct': self.is_correct,
            'feedback': self.feedback,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }


class Flashcard(db.Model):
    __tablename__ = 'flashcards'
    
    id = db.Column(db.Integer, primary_key=True)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    question = db.Column(db.String(500), nullable=False)
    answer = db.Column(db.Text, nullable=False)
    mastery = db.Column(db.Integer, default=0)  # 0-100
    review_count = db.Column(db.Integer, default=0)
    review_stage = db.Column(db.Integer, default=0)  # 0=新学, 1-6=艾宾浩斯阶段, 7=已掌握
    last_reviewed = db.Column(db.DateTime)
    next_review_date = db.Column(db.Date)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    
    student = db.relationship('User', backref='flashcards')
    
    def to_dict(self):
        return {
            'id': self.id,
            'student_id': self.student_id,
            'question': self.question,
            'answer': self.answer,
            'mastery': self.mastery,
            'review_count': self.review_count,
            'review_stage': self.review_stage,
            'last_reviewed': self.last_reviewed.isoformat() if self.last_reviewed else None,
            'next_review_date': self.next_review_date.isoformat() if self.next_review_date else None,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }


class FormulaMastery(db.Model):
    __tablename__ = 'formula_masteries'
    
    id = db.Column(db.Integer, primary_key=True)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    formula_id = db.Column(db.String(50), nullable=False)
    mastery = db.Column(db.Integer, default=0)  # 0-100
    correct_count = db.Column(db.Integer, default=0)
    total_attempts = db.Column(db.Integer, default=0)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    student = db.relationship('User', backref='formula_masteries')
    
    __table_args__ = (db.UniqueConstraint('student_id', 'formula_id'),)
    
    def to_dict(self):
        return {
            'id': self.id,
            'student_id': self.student_id,
            'formula_id': self.formula_id,
            'mastery': self.mastery,
            'correct_count': self.correct_count,
            'total_attempts': self.total_attempts
        }


class Feedback(db.Model):
    __tablename__ = 'feedbacks'

    id = db.Column(db.Integer, primary_key=True)
    user_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False)
    category = db.Column(db.String(50), nullable=False)
    content = db.Column(db.Text, nullable=False)
    contact = db.Column(db.String(100))
    status = db.Column(db.String(20), default='pending')
    admin_reply = db.Column(db.Text)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    replied_at = db.Column(db.DateTime)

    user = db.relationship('User', backref='feedbacks')

    def to_dict(self, include_user=False):
        data = {
            'id': self.id,
            'user_id': self.user_id,
            'category': self.category,
            'content': self.content,
            'contact': self.contact,
            'status': self.status,
            'admin_reply': self.admin_reply,
            'created_at': self.created_at.isoformat() if self.created_at else None,
            'replied_at': self.replied_at.isoformat() if self.replied_at else None
        }
        if include_user and self.user:
            data['username'] = self.user.username
            data['nickname'] = self.user.nickname
            data['user_role'] = self.user.role
        return data


class Pet(db.Model):
    __tablename__ = 'pets'
    
    id = db.Column(db.Integer, primary_key=True)
    student_id = db.Column(db.Integer, db.ForeignKey('users.id'), nullable=False, unique=True)
    name = db.Column(db.String(50), default='学习精灵')
    pet_type = db.Column(db.String(30), default='cat')
    skin = db.Column(db.String(30), default='default')
    level = db.Column(db.Integer, default=1)
    exp = db.Column(db.Integer, default=0)
    max_exp = db.Column(db.Integer, default=100)
    hunger = db.Column(db.Integer, default=100)
    happiness = db.Column(db.Integer, default=100)
    total_corrected = db.Column(db.Integer, default=0)
    streak_days = db.Column(db.Integer, default=0)
    last_feed_date = db.Column(db.Date)
    created_at = db.Column(db.DateTime, default=datetime.utcnow)
    updated_at = db.Column(db.DateTime, default=datetime.utcnow, onupdate=datetime.utcnow)
    
    student = db.relationship('User', backref='pet')
    
    def to_dict(self):
        return {
            'id': self.id,
            'student_id': self.student_id,
            'name': self.name,
            'pet_type': self.pet_type,
            'skin': self.skin,
            'level': self.level,
            'exp': self.exp,
            'max_exp': self.max_exp,
            'hunger': self.hunger,
            'happiness': self.happiness,
            'total_corrected': self.total_corrected,
            'streak_days': self.streak_days,
            'last_feed_date': self.last_feed_date.isoformat() if self.last_feed_date else None,
            'created_at': self.created_at.isoformat() if self.created_at else None
        }

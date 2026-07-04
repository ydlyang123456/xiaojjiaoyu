import json
import random
import re
from flask import Blueprint, request, jsonify
from flask_jwt_extended import jwt_required, get_jwt_identity
from models import db, Pet, FormulaMastery
from datetime import datetime

battle_bp = Blueprint('battle', __name__, url_prefix='/api/battle')

# 公式库：每个公式有名称、公式内容、学科、基础伤害、难度
FORMULA_LIBRARY = [
    {'id': 'f1', 'name': '勾股定理', 'formula': '$a^2 + b^2 = c^2$', 'subject': '数学', 'damage': 25, 'difficulty': '简单'},
    {'id': 'f2', 'name': '一元二次方程求根公式', 'formula': '$x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}$', 'subject': '数学', 'damage': 35, 'difficulty': '中等'},
    {'id': 'f3', 'name': '三角形面积公式', 'formula': '$S = \\frac{1}{2}ah$', 'subject': '数学', 'damage': 20, 'difficulty': '简单'},
    {'id': 'f4', 'name': '圆的面积公式', 'formula': '$S = \\pi r^2$', 'subject': '数学', 'damage': 20, 'difficulty': '简单'},
    {'id': 'f5', 'name': '等差数列求和', 'formula': '$S_n = \\frac{n(a_1+a_n)}{2}$', 'subject': '数学', 'damage': 30, 'difficulty': '中等'},
    {'id': 'f6', 'name': '牛顿第二定律', 'formula': '$F = ma$', 'subject': '物理', 'damage': 30, 'difficulty': '中等'},
    {'id': 'f7', 'name': '欧姆定律', 'formula': '$I = \\frac{U}{R}$', 'subject': '物理', 'damage': 25, 'difficulty': '简单'},
    {'id': 'f8', 'name': '电功率公式', 'formula': '$P = UI$', 'subject': '物理', 'damage': 25, 'difficulty': '简单'},
    {'id': 'f9', 'name': '动能公式', 'formula': '$E_k = \\frac{1}{2}mv^2$', 'subject': '物理', 'damage': 35, 'difficulty': '中等'},
    {'id': 'f10', 'name': '化学方程式配平', 'formula': '$2H_2 + O_2 \\rightarrow 2H_2O$', 'subject': '化学', 'damage': 30, 'difficulty': '中等'},
    {'id': 'f11', 'name': '摩尔质量公式', 'formula': '$n = \\frac{m}{M}$', 'subject': '化学', 'damage': 20, 'difficulty': '简单'},
    {'id': 'f12', 'name': 'pH值计算公式', 'formula': '$pH = -\\lg[H^+]$', 'subject': '化学', 'damage': 30, 'difficulty': '中等'},
    {'id': 'f13', 'name': '等比数列求和', 'formula': '$S_n = \\frac{a_1(1-q^n)}{1-q}$', 'subject': '数学', 'damage': 35, 'difficulty': '困难'},
    {'id': 'f14', 'name': '三角函数正弦定理', 'formula': '$\\frac{a}{\\sin A} = \\frac{b}{\\sin B} = \\frac{c}{\\sin C} = 2R$', 'subject': '数学', 'damage': 40, 'difficulty': '困难'},
    {'id': 'f15', 'name': '万有引力定律', 'formula': '$F = G\\frac{m_1m_2}{r^2}$', 'subject': '物理', 'damage': 40, 'difficulty': '困难'},
    {'id': 'f16', 'name': '动能定理', 'formula': '$W = \\Delta E_k$', 'subject': '物理', 'damage': 30, 'difficulty': '中等'},
    {'id': 'f17', 'name': '圆柱体积公式', 'formula': '$V = \\pi r^2 h$', 'subject': '数学', 'damage': 20, 'difficulty': '简单'},
    {'id': 'f18', 'name': '方差公式', 'formula': '$\\sigma^2 = \\frac{1}{n}\\sum(x_i-\\bar{x})^2$', 'subject': '数学', 'damage': 35, 'difficulty': '困难'},
    {'id': 'f19', 'name': '功的计算公式', 'formula': '$W = Fs\\cos\\theta$', 'subject': '物理', 'damage': 25, 'difficulty': '简单'},
    {'id': 'f20', 'name': '热力学第一定律', 'formula': '$\\Delta U = Q + W$', 'subject': '物理', 'damage': 40, 'difficulty': '困难'},
]

# 对手宠物名列表
OPPONENT_NAMES = ['暗影龙', '冰霜狼', '烈焰鸟', '岩石巨人', '闪电狐', '毒液蛇', '钢铁熊', '风暴鹰', '幽灵猫', '水晶龟']
OPPONENT_EMOJIS = ['🐲', '🐺', '🦅', '🪨', '🦊', '🐍', '🐻', '🦅', '👻', '🐢']


@battle_bp.route('/formulas', methods=['GET'])
@jwt_required()
def get_formulas():
    """获取公式列表及用户熟练度"""
    student_id = int(get_jwt_identity())
    masteries = {m.formula_id: m for m in FormulaMastery.query.filter_by(student_id=student_id).all()}

    formulas = []
    for f in FORMULA_LIBRARY:
        fm = masteries.get(f['id'])
        formulas.append({
            'id': f['id'],
            'name': f['name'],
            'formula': f['formula'],
            'subject': f['subject'],
            'damage': f['damage'],
            'difficulty': f['difficulty'],
            'mastery': fm.mastery if fm else 0,
            'correct_count': fm.correct_count if fm else 0,
            'total_attempts': fm.total_attempts if fm else 0
        })
    return jsonify({'code': 200, 'data': formulas})


@battle_bp.route('/start', methods=['POST'])
@jwt_required()
def start_battle():
    """开始一场对战，返回对手信息和双方状态"""
    student_id = int(get_jwt_identity())
    pet = Pet.query.filter_by(student_id=student_id).first()
    if not pet:
        return jsonify({'code': 404, 'msg': '请先养一只宠物'}), 404

    # 生成对手
    opp_idx = random.randint(0, len(OPPONENT_NAMES) - 1)
    opp_level = max(1, pet.level + random.randint(-2, 2))
    opp_hp = 100 + opp_level * 20

    opponent = {
        'name': OPPONENT_NAMES[opp_idx],
        'emoji': OPPONENT_EMOJIS[opp_idx],
        'level': opp_level,
        'hp': opp_hp,
        'max_hp': opp_hp
    }

    player_hp = 100 + pet.level * 20

    battle_state = {
        'player': {
            'pet_name': pet.name,
            'pet_skin': pet.skin,
            'level': pet.level,
            'hp': player_hp,
            'max_hp': player_hp
        },
        'opponent': opponent,
        'round': 0,
        'log': [f'⚔️ 对战开始！{pet.name} VS {opponent["name"]}']
    }

    return jsonify({'code': 200, 'data': battle_state})


@battle_bp.route('/attack', methods=['POST'])
@jwt_required()
def attack():
    """使用公式攻击：熟练度100%直接释放，否则需要解题"""
    student_id = int(get_jwt_identity())
    data = request.get_json()
    formula_id = data.get('formula_id')
    skip_solve = data.get('skip_solve', False)  # 熟练度100%时跳过解题

    formula = next((f for f in FORMULA_LIBRARY if f['id'] == formula_id), None)
    if not formula:
        return jsonify({'code': 400, 'msg': '公式不存在'}), 400

    mastery = FormulaMastery.query.filter_by(
        student_id=student_id, formula_id=formula_id
    ).first()
    mastery_val = mastery.mastery if mastery else 0

    if mastery_val >= 100 and skip_solve:
        # 熟练度100%，直接释放
        return jsonify({
            'code': 200,
            'data': {
                'auto': True,
                'formula': formula,
                'damage': formula['damage'],
                'msg': f'熟练度100%！{formula["name"]}自动释放，造成 {formula["damage"]} 点伤害！'
            }
        })
    else:
        # 需要解题
        problem = generate_problem(formula)
        return jsonify({
            'code': 200,
            'data': {
                'auto': False,
                'formula': formula,
                'problem': problem,
                'msg': f'请解答以下问题来释放 {formula["name"]}'
            }
        })


@battle_bp.route('/solve', methods=['POST'])
@jwt_required()
def solve():
    """提交解题答案"""
    student_id = int(get_jwt_identity())
    data = request.get_json()
    formula_id = data.get('formula_id')
    answer = data.get('answer', '').strip()
    problem = data.get('problem', {})

    formula = next((f for f in FORMULA_LIBRARY if f['id'] == formula_id), None)
    if not formula:
        return jsonify({'code': 400, 'msg': '公式不存在'}), 400

    expected = problem.get('answer', '')
    is_correct = answer.lower().replace(' ', '') == expected.lower().replace(' ', '')

    # 更新熟练度
    mastery = FormulaMastery.query.filter_by(
        student_id=student_id, formula_id=formula_id
    ).first()
    if not mastery:
        mastery = FormulaMastery(student_id=student_id, formula_id=formula_id)
        db.session.add(mastery)

    mastery.total_attempts += 1
    if is_correct:
        mastery.correct_count += 1
        mastery.mastery = min(100, mastery.mastery + 20)
        damage = formula['damage']
        msg = f'✅ 回答正确！{formula["name"]}释放，造成 {damage} 点伤害！'
    else:
        mastery.mastery = max(0, mastery.mastery - 5)
        damage = 0
        msg = f'❌ 回答错误！正确答案是 {expected}。{formula["name"]}释放失败！'

    db.session.commit()

    # 宠物获得经验
    pet = Pet.query.filter_by(student_id=student_id).first()
    pet_exp_gained = 0
    if pet and is_correct:
        exp_gain = 5 + formula['damage'] // 5
        pet.exp += exp_gain
        pet_exp_gained = exp_gain
        while pet.exp >= pet.max_exp:
            pet.exp -= pet.max_exp
            pet.level += 1
            pet.max_exp = int(pet.max_exp * 1.3)
        db.session.commit()

    return jsonify({
        'code': 200,
        'data': {
            'correct': is_correct,
            'formula': formula,
            'damage': damage,
            'expected': expected,
            'msg': msg,
            'mastery': mastery.mastery,
            'pet_exp': pet_exp_gained,
            'pet_level': pet.level if pet else 0
        }
    })


@battle_bp.route('/opponent-attack', methods=['POST'])
@jwt_required()
def opponent_attack():
    """对手随机攻击"""
    data = request.get_json() or {}
    opp_level = data.get('opponent_level', 1)
    opp_name = data.get('opponent_name', '对手')

    # 随机选一个公式
    formula = random.choice(FORMULA_LIBRARY)
    # 对手伤害 = 基础伤害 * (1 + 等级/10)
    damage = int(formula['damage'] * (1 + opp_level / 10))

    return jsonify({
        'code': 200,
        'data': {
            'formula_name': formula['name'],
            'formula': formula['formula'],
            'damage': damage,
            'msg': f'{opp_name} 使用 {formula["name"]} {formula["formula"]}，造成 {damage} 点伤害！'
        }
    })


def generate_problem(formula):
    """根据公式生成简单的练习题"""
    name = formula['name']
    subject = formula['subject']

    problems = {
        '勾股定理': {
            'question': '直角三角形的两条直角边分别为3和4，斜边是多少？',
            'answer': '5',
            'hint': '用 $a^2 + b^2 = c^2$ 计算'
        },
        '一元二次方程求根公式': {
            'question': '解方程 $x^2 - 5x + 6 = 0$，较小的根是多少？',
            'answer': '2',
            'hint': '用求根公式 $x = \\frac{-b \\pm \\sqrt{b^2-4ac}}{2a}$'
        },
        '三角形面积公式': {
            'question': '三角形的底为6cm，高为4cm，面积是多少平方厘米？',
            'answer': '12',
            'hint': '用 $S = \\frac{1}{2}ah$'
        },
        '圆的面积公式': {
            'question': '半径为5cm的圆，面积是多少平方厘米？(π取3.14)',
            'answer': '78.5',
            'hint': '用 $S = \\pi r^2$'
        },
        '等差数列求和': {
            'question': '等差数列 1, 3, 5, 7, 9 的和是多少？',
            'answer': '25',
            'hint': '用 $S_n = \\frac{n(a_1+a_n)}{2}$'
        },
        '牛顿第二定律': {
            'question': '质量为2kg的物体，加速度为3m/s²，受到的力是多少牛顿？',
            'answer': '6',
            'hint': '用 $F = ma$'
        },
        '欧姆定律': {
            'question': '电压为12V，电阻为4Ω，电流是多少安培？',
            'answer': '3',
            'hint': '用 $I = \\frac{U}{R}$'
        },
        '电功率公式': {
            'question': '电压为220V，电流为2A，电功率是多少瓦？',
            'answer': '440',
            'hint': '用 $P = UI$'
        },
        '动能公式': {
            'question': '质量为2kg的物体，速度为5m/s，动能是多少焦耳？',
            'answer': '25',
            'hint': '用 $E_k = \\frac{1}{2}mv^2$'
        },
        '摩尔质量公式': {
            'question': '18g水的物质的量是多少摩尔？(水的摩尔质量为18g/mol)',
            'answer': '1',
            'hint': '用 $n = \\frac{m}{M}$'
        },
        '化学方程式配平': {
            'question': '配平后的化学方程式 $2H_2 + O_2 \\rightarrow 2H_2O$ 中，左边共有几个氢原子？',
            'answer': '4',
            'hint': '数一下左边氢原子的个数'
        },
        'pH值计算公式': {
            'question': '某溶液中 $[H^+] = 10^{-3}$ mol/L，pH值是多少？',
            'answer': '3',
            'hint': '用 $pH = -\\lg[H^+]$'
        },
        '等比数列求和': {
            'question': '等比数列 2, 4, 8, 16 的前4项和是多少？',
            'answer': '30',
            'hint': '直接相加：2+4+8+16'
        },
        '圆柱体积公式': {
            'question': '圆柱底面半径为3cm，高为10cm，体积是多少立方厘米？(π取3.14)',
            'answer': '282.6',
            'hint': '用 $V = \\pi r^2 h$'
        },
        '万有引力定律': {
            'question': '万有引力定律中，引力与距离的几次方成反比？',
            'answer': '2',
            'hint': '公式 $F = G\\frac{m_1m_2}{r^2}$ 中看r的指数'
        },
        '动能定理': {
            'question': '合外力对物体做功为10J，物体动能变化了多少？',
            'answer': '10',
            'hint': '用 $W = \\Delta E_k$'
        },
        '功的计算公式': {
            'question': '力为10N，位移为5m，力与位移方向相同，做功多少焦耳？',
            'answer': '50',
            'hint': '用 $W = Fs$（cos0°=1）'
        },
        '三角函数正弦定理': {
            'question': '在三角形中，若 $\\sin A = 0.5$，则角A是多少度？',
            'answer': '30',
            'hint': 'sin30° = 0.5'
        },
        '方差公式': {
            'question': '数据 2, 4, 6 的平均数是多少？',
            'answer': '4',
            'hint': '(2+4+6)/3'
        },
        '热力学第一定律': {
            'question': '系统吸收热量50J，对外做功20J，内能变化多少焦耳？',
            'answer': '30',
            'hint': '用 $\\Delta U = Q + W$，对外做功W为负'
        },
    }

    p = problems.get(name, {
        'question': f'请默写{name}的公式',
        'answer': formula['formula'].replace('$', '').replace(' ', ''),
        'hint': f'回忆{name}的公式形式'
    })

    return {
        'question': p['question'],
        'answer': p['answer'],
        'hint': p['hint'],
        'formula_name': name,
        'formula': formula['formula']
    }
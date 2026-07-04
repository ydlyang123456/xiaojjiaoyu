import json
import random
import re
from datetime import datetime, timedelta
from flask import current_app


class AIService:
    def __init__(self):
        self.api_key = None
        self.api_url = None
        self.model = None
        self.provider = None

    def init_app(self, app):
        self.api_key = app.config.get('AI_API_KEY', '')
        self.api_url = app.config.get('AI_API_URL', '')
        self.model = app.config.get('AI_MODEL', '')
        self.provider = app.config.get('AI_PROVIDER', '')

    def _is_real_ai_available(self):
        return bool(self.api_key and self.api_url and self.model)

    def solve_question(self, question_text=None, image_url=None, subject=None):
        if self._is_real_ai_available():
            try:
                return self._call_real_ai(question_text, image_url, subject)
            except Exception as e:
                print(f"[DeepSeek] 解题调用失败: {e}，使用模拟数据")
                return self._mock_solve_question(question_text, image_url, subject)
        else:
            return self._mock_solve_question(question_text, image_url, subject)

    def _call_real_ai(self, question_text, image_url, subject):
        import requests
        headers = {
            'api-key': self.api_key,
            'Content-Type': 'application/json'
        }

        subject_desc = subject or '学科'
        question_desc = question_text or ''

        system_prompt = """你是一位专业的中小学学科辅导老师，擅长解答各类题目并给出详细讲解。
请严格按照以下格式输出答案，不要输出其他内容：

【答案】
这里写最终答案

【解题过程】
这里写详细的解题步骤，分点说明

【知识点】
1. 知识点1
2. 知识点2
3. 知识点3
（列出3-5个相关知识点）

【难度等级】
简单/中等/困难（三选一）

【格式要求】
- 所有数学公式必须用LaTeX格式：行内公式用$...$，独立公式用$$...$$
- 例如：$x^2 + y^2 = z^2$、$\frac{a}{b}$、$\sqrt{x}$
- 不要使用Unicode上标字符（如 ² ³），必须用LaTeX格式（如 ^2 ^3）
"""

        user_content = []
        
        if image_url:
            if image_url.startswith('/'):
                full_image_url = f"http://47.107.109.101:5000{image_url}"
            else:
                full_image_url = image_url
            
            user_content.append({
                'type': 'image_url',
                'image_url': {
                    'url': full_image_url
                }
            })
        
        text_content = f"请解答以下{subject_desc}题目：\n\n"
        if question_desc:
            text_content += f"题目内容：{question_desc}\n\n"
        text_content += "请按照指定格式输出完整解答。"
        
        user_content.append({
            'type': 'text',
            'text': text_content
        })

        payload = {
            'model': self.model,
            'messages': [
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_content}
            ],
            'temperature': 0.7,
            'max_completion_tokens': 4000,
            'thinking': {
                'type': 'disabled'
            }
        }

        response = requests.post(self.api_url, headers=headers, json=payload, timeout=120)

        if response.status_code == 200:
            result = response.json()
            ai_text = result['choices'][0]['message']['content']
            return self._parse_ai_answer(ai_text, subject)
        else:
            print(f"[DeepSeek] API返回错误: {response.status_code} - {response.text}")
            return self._mock_solve_question(question_text, image_url, subject)

    def _parse_ai_answer(self, ai_text, subject):
        ai_text = self._clean_latex(ai_text)
        
        knowledge_points = []
        difficulty = '中等'

        kp_match = re.search(r'【知识点】\s*\n(.*?)(?=\n【|$)', ai_text, re.DOTALL)
        if kp_match:
            kp_text = kp_match.group(1).strip()
            for line in kp_text.split('\n'):
                line = line.strip()
                if line and re.match(r'^[\d\.\-\·\s]+', line):
                    clean = re.sub(r'^[\d\.\-\·\s]+', '', line).strip()
                    if clean:
                        knowledge_points.append(clean)

        if not knowledge_points:
            knowledge_points = [
                f'{subject or "学科"}基础概念',
                f'{subject or "学科"}核心考点',
                f'{subject or "学科"}解题方法'
            ]

        diff_match = re.search(r'【难度等级】\s*\n\s*(简单|中等|困难)', ai_text)
        if diff_match:
            difficulty = diff_match.group(1)

        return {
            'ai_answer': ai_text,
            'ai_solution': ai_text,
            'knowledge_points': json.dumps(knowledge_points, ensure_ascii=False),
            'difficulty': difficulty
        }

    def _clean_latex(self, text):
        if not text:
            return text

        def fix_inline(m):
            formula = m.group(1)
            return '$' + self._fix_latex_formula(formula) + '$'

        def fix_display(m):
            formula = m.group(1)
            return '$$' + self._fix_latex_formula(formula) + '$$'

        text = re.sub(r'\$\$([\s\S]+?)\$\$', fix_display, text)
        text = re.sub(r'\$([^$\n]+?)\$', fix_inline, text)

        return text

    def _fix_latex_formula(self, formula):
        formula = formula.strip()

        formula = re.sub(r'(\))(\d+)', lambda m: m.group(1) + '^{' + m.group(2) + '}', formula)

        formula = re.sub(r'(\w)(\d{2,})', lambda m: m.group(1) + '^{' + m.group(2) + '}', formula)

        formula = re.sub(r'(\^)(\d+)(?!\})', lambda m: '^{' + m.group(2) + '}', formula)
        formula = re.sub(r'(_)(\d+)(?!\})', lambda m: '_{' + m.group(2) + '}', formula)

        return formula

    def _mock_solve_question(self, question_text, image_url, subject):
        subjects = ['数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治']
        actual_subject = subject or random.choice(subjects)

        difficulties = ['简单', '中等', '困难']
        difficulty = random.choice(difficulties)

        knowledge = [
            f'{actual_subject}基础概念',
            f'{actual_subject}核心公式',
            f'{actual_subject}解题技巧',
            f'{actual_subject}常见考点'
        ]

        answer = f"【{actual_subject}题目解答】\n\n"
        answer += f"题目：{question_text or '（图片题目）'}\n\n"
        answer += "【答案】\n"
        answer += "本题的正确答案需要根据具体题目内容分析得出。\n\n"
        answer += "【解题过程】\n"
        answer += "1. 首先仔细审题，明确题目要求和已知条件\n"
        answer += "2. 回忆相关知识点和解题方法\n"
        answer += "3. 逐步分析，建立解题思路\n"
        answer += "4. 认真计算，注意细节\n"
        answer += "5. 检查答案，确保正确\n\n"
        answer += "【知识点】\n"
        for i, k in enumerate(knowledge, 1):
            answer += f"{i}. {k}\n"
        answer += f"\n【难度等级】\n{difficulty}\n"

        return {
            'ai_answer': answer,
            'ai_solution': answer,
            'knowledge_points': json.dumps(knowledge, ensure_ascii=False),
            'difficulty': difficulty
        }

    def analyze_study(self, student_id, search_records, start_date=None, end_date=None):
        if self._is_real_ai_available():
            try:
                return self._call_real_analysis(student_id, search_records, start_date, end_date)
            except Exception as e:
                print(f"[DeepSeek] 学习分析调用失败: {e}，使用模拟数据")
                return self._mock_analyze_study(student_id, search_records, start_date, end_date)
        else:
            return self._mock_analyze_study(student_id, search_records, start_date, end_date)

    def _call_real_analysis(self, student_id, search_records, start_date, end_date):
        import requests
        headers = {
            'api-key': self.api_key,
            'Content-Type': 'application/json'
        }

        total_count = len(search_records)

        subject_stats = {}
        difficulty_stats = {'简单': 0, '中等': 0, '困难': 0}
        knowledge_all = []

        for record in search_records:
            subj = record.subject or '其他'
            subject_stats[subj] = subject_stats.get(subj, 0) + 1

            if record.difficulty:
                difficulty_stats[record.difficulty] = difficulty_stats.get(record.difficulty, 0) + 1

            if record.knowledge_points:
                try:
                    kps = json.loads(record.knowledge_points)
                    knowledge_all.extend(kps)
                except:
                    pass

        daily_stats = self._calculate_daily_stats(search_records, start_date, end_date)

        subject_summary = ''
        for subj, count in subject_stats.items():
            subject_summary += f'- {subj}: {count}题\n'

        difficulty_summary = ''
        for diff, count in difficulty_stats.items():
            difficulty_summary += f'- {diff}: {count}题\n'

        knowledge_summary = ''
        unique_knowledge = list(set(knowledge_all))[:15]
        for kp in unique_knowledge:
            knowledge_summary += f'- {kp}\n'

        days = (end_date - start_date).days if start_date and end_date else 30

        system_prompt = """你是一位专业的教育分析师，擅长根据学生的学习数据进行全面的学习分析。
请根据提供的学生搜题记录数据，生成一份详细的学习状态分析报告。

请严格按照以下JSON格式输出，不要输出其他内容：
{
    "overall_evaluation": "整体评估文本，包括总体评价、学习态度、学科分布等，300-500字",
    "weak_points": ["薄弱点1", "薄弱点2", "薄弱点3"],
    "strong_points": ["优势点1", "优势点2", "优势点3"],
    "suggestions": ["建议1", "建议2", "建议3", "建议4", "建议5"],
    "study_habits": "学习习惯分析文本，包括日均学习量、学习持续性、学科均衡度等"
}

注意：
- 输出必须是合法的JSON格式
- 用中文回答
- 分析要具体、有针对性，结合数据说话
"""

        user_prompt = f"""请分析以下学生的学习数据：

【基本信息】
- 分析时间段：{days}天
- 总搜题数：{total_count}题
- 涉及学科数：{len(subject_stats)}个

【学科分布】
{subject_summary if subject_summary else '暂无数据'}

【难度分布】
{difficulty_summary if difficulty_summary else '暂无数据'}

【涉及知识点】
{knowledge_summary if knowledge_summary else '暂无数据'}

【每日学习情况】
共 {len(daily_stats)} 天有学习记录

请根据以上数据，生成完整的学习分析报告。"""

        payload = {
            'model': self.model,
            'messages': [
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt}
            ],
            'temperature': 0.7,
            'max_completion_tokens': 6000,
            'thinking': {
                'type': 'disabled'
            }
        }

        response = requests.post(self.api_url, headers=headers, json=payload, timeout=120)

        if response.status_code == 200:
            result = response.json()
            ai_text = result['choices'][0]['message']['content']
            return self._parse_analysis_result(ai_text, search_records, subject_stats, difficulty_stats, daily_stats, start_date, end_date)
        else:
            print(f"[DeepSeek] 分析API返回错误: {response.status_code} - {response.text}")
            return self._mock_analyze_study(student_id, search_records, start_date, end_date)

    def _parse_analysis_result(self, ai_text, search_records, subject_stats, difficulty_stats, daily_stats, start_date, end_date):
        try:
            json_match = re.search(r'\{[\s\S]*\}', ai_text)
            if json_match:
                json_str = json_match.group(0)
                data = json.loads(json_str)

                total_count = len(search_records)
                subject_analysis_list = []
                for subj, count in subject_stats.items():
                    subject_analysis_list.append({
                        'subject': subj,
                        'count': count,
                        'percentage': round(count / max(total_count, 1) * 100, 1),
                        'evaluation': f'{subj}共搜索{count}题，占比{round(count/max(total_count,1)*100,1)}%'
                    })

                statistics = {
                    'total_count': total_count,
                    'subject_count': len(subject_stats),
                    'subject_stats': subject_stats,
                    'difficulty_stats': difficulty_stats,
                    'daily_stats': daily_stats,
                    'knowledge_points_count': len(set())
                }

                weak_points = data.get('weak_points', [])
                if isinstance(weak_points, list) and len(weak_points) == 0:
                    weak_points = [s[0] for s in sorted(subject_stats.items(), key=lambda x: x[1], reverse=True)[:3]]

                strong_points = data.get('strong_points', [])
                if isinstance(strong_points, list) and len(strong_points) == 0:
                    strong_points = [s[0] for s in sorted(subject_stats.items(), key=lambda x: x[1])[:3]]

                suggestions = data.get('suggestions', [])
                if isinstance(suggestions, list) and len(suggestions) == 0:
                    suggestions = [
                        '每天坚持学习，保持学习节奏',
                        '建立错题本，定期复习巩固',
                        '遇到难题先思考，再看答案',
                        '合理安排各科学习时间',
                        '注意劳逸结合，提高学习效率'
                    ]

                return {
                    'overall_evaluation': data.get('overall_evaluation', '暂无评估'),
                    'subject_analysis': json.dumps(subject_analysis_list, ensure_ascii=False),
                    'weak_points': json.dumps(weak_points, ensure_ascii=False),
                    'strong_points': json.dumps(strong_points, ensure_ascii=False),
                    'suggestions': json.dumps(suggestions, ensure_ascii=False),
                    'study_habits': data.get('study_habits', '暂无分析'),
                    'statistics_data': json.dumps(statistics, ensure_ascii=False)
                }
            else:
                print(f"[DeepSeek] 未找到JSON格式数据，原始输出: {ai_text[:200]}")
                return self._mock_analyze_study(0, search_records, start_date, end_date)
        except Exception as e:
            print(f"[DeepSeek] 解析分析结果失败: {e}")
            return self._mock_analyze_study(0, search_records, start_date, end_date)

    def _mock_analyze_study(self, student_id, search_records, start_date, end_date):
        total_count = len(search_records)

        subject_stats = {}
        difficulty_stats = {'简单': 0, '中等': 0, '困难': 0}
        knowledge_points = set()

        for record in search_records:
            subj = record.subject or '其他'
            subject_stats[subj] = subject_stats.get(subj, 0) + 1

            if record.difficulty:
                difficulty_stats[record.difficulty] = difficulty_stats.get(record.difficulty, 0) + 1

            if record.knowledge_points:
                try:
                    kps = json.loads(record.knowledge_points)
                    knowledge_points.update(kps)
                except:
                    pass

        daily_stats = self._calculate_daily_stats(search_records, start_date, end_date)

        if total_count == 0:
            overall = "该学生在此期间暂无搜题记录。"
        else:
            weak_subjects = sorted(subject_stats.items(), key=lambda x: x[1], reverse=True)[:3]
            strong_subjects = sorted(subject_stats.items(), key=lambda x: x[1])[:2]

            overall = f"""
【学习状态综合评估】

总体评价：
该学生在此期间共搜索了 {total_count} 道题目，学习态度积极，具有主动学习的精神。

学科分布分析：
涉及 {len(subject_stats)} 个学科，其中搜索最多的是：
"""
            for subj, count in list(subject_stats.items())[:5]:
                overall += f"  · {subj}：{count}题\n"

            overall += f"""
学习特点：
1. 学习主动性较强，遇到问题能够主动寻求解答
2. 涉及学科较广，表明各科学习都在稳步推进
3. 难题占比：困难题约 {round(difficulty_stats['困难']/max(total_count,1)*100, 1)}%，中等题约 {round(difficulty_stats['中等']/max(total_count,1)*100, 1)}%

薄弱环节：
{', '.join([s[0] for s in weak_subjects[:2]])} 等学科搜题较多，可能需要重点加强。

优势方面：
{', '.join([s[0] for s in strong_subjects[:2]]) if strong_subjects else '各学科均衡发展'} 相对掌握较好。

学习建议：
1. 建议针对薄弱学科制定专项练习计划
2. 对做错的题目建立错题本，定期复习
3. 合理安排学习时间，注意劳逸结合
4. 遇到难题先独立思考，再查看答案
            """

        subject_analysis_list = []
        for subj, count in subject_stats.items():
            subject_analysis_list.append({
                'subject': subj,
                'count': count,
                'percentage': round(count / max(total_count, 1) * 100, 1),
                'evaluation': f'{subj}共搜索{count}题，占比{round(count/max(total_count,1)*100,1)}%'
            })

        statistics = {
            'total_count': total_count,
            'subject_count': len(subject_stats),
            'subject_stats': subject_stats,
            'difficulty_stats': difficulty_stats,
            'daily_stats': daily_stats,
            'knowledge_points_count': len(knowledge_points)
        }

        weak = [s[0] for s in sorted(subject_stats.items(), key=lambda x: x[1], reverse=True)[:3]]
        strong = [s[0] for s in sorted(subject_stats.items(), key=lambda x: x[1])[:3]]

        suggestions = [
            '每天坚持学习，保持学习节奏',
            '建立错题本，定期复习巩固',
            '遇到难题先思考，再看答案',
            '合理安排各科学习时间',
            '注意劳逸结合，提高学习效率'
        ]

        study_habits = f"""
学习习惯分析：
- 日均搜题量：{round(total_count / max(len(daily_stats), 1), 1)} 题
- 学习持续性：{'较好' if len(daily_stats) > 7 else '一般'}
- 学科均衡度：{'均衡' if len(subject_stats) >= 5 else '偏科'}
        """

        return {
            'overall_evaluation': overall.strip(),
            'subject_analysis': json.dumps(subject_analysis_list, ensure_ascii=False),
            'weak_points': json.dumps(weak, ensure_ascii=False),
            'strong_points': json.dumps(strong, ensure_ascii=False),
            'suggestions': json.dumps(suggestions, ensure_ascii=False),
            'study_habits': study_habits.strip(),
            'statistics_data': json.dumps(statistics, ensure_ascii=False)
        }

    def _calculate_daily_stats(self, search_records, start_date, end_date):
        daily = {}
        for record in search_records:
            date_str = record.created_at.strftime('%Y-%m-%d') if record.created_at else 'unknown'
            daily[date_str] = daily.get(date_str, 0) + 1

        return daily


    def knowledge_query(self, query, subject=None, preset_content=None):
        if not self._is_subject_related(query):
            return {
                'query': query,
                'blocked': True,
                'reason': '抱歉，知识词典仅支持学科知识查询（数学、语文、英语、物理、化学、生物、历史、地理、政治等），请输入学科相关问题。'
            }
        
        if self._is_real_ai_available():
            try:
                return self._call_knowledge_with_ai(query, subject)
            except Exception as e:
                print(f"[MiMo] 知识查询调用失败: {e}，使用模拟数据")
                return self._mock_knowledge_query(query, subject)
        else:
            return self._mock_knowledge_query(query, subject)
    
    def _is_subject_related(self, query):
        subject_keywords = [
            '数学', '语文', '英语', '物理', '化学', '生物', '历史', '地理', '政治',
            '代数', '几何', '函数', '方程', '三角形', '圆', '概率', '统计',
            '文言文', '古诗', '作文', '阅读理解', '语法', '修辞', '成语',
            '单词', '语法', '时态', '句型', '词汇', '听力', '口语',
            '力学', '电学', '光学', '热学', '声学', '原子', '牛顿', '欧姆',
            '元素', '化学方程式', '反应', '原子', '分子', '离子', '酸碱',
            '细胞', '遗传', '进化', '生态', '植物', '动物', '人体',
            '朝代', '历史事件', '历史人物', '战争', '革命',
            '气候', '地形', '河流', '山脉', '城市', '国家', '地图',
            '政治', '经济', '哲学', '法律', '道德',
            '公式', '定理', '定律', '原理', '概念', '定义',
            '勾股', '一元二次', '圆周率', '牛顿', '欧姆', '元素周期',
            '比喻', '拟人', '排比', '夸张', '七大洲', '四大洋',
            '方程', '不等式', '分数', '小数', '百分数',
            '根号', '平方', '立方', '面积', '体积', '周长',
            '向量', '矩阵', '导数', '积分', '极限',
            '电路', '电流', '电压', '电阻', '磁场', '电磁',
            '化学反应', '氧化还原', '置换反应', '复分解',
            '光合作用', '呼吸作用', '细胞分裂', 'DNA', '基因',
            '秦始皇', '汉武帝', '唐朝', '宋朝', '明朝', '清朝',
            '地中海', '喜马拉雅', '亚马逊', '尼罗河',
            '马克思', '社会主义', '资本主义', '市场经济',
            '抛物线', '双曲线', '椭圆', '三角函数', '正弦', '余弦',
            '等差数列', '等比数列', '集合', '映射', '数列',
            '坐标系', '数轴', '平面直角', '空间几何',
            '动能', '势能', '机械能', '动量', '冲量',
            '凸透镜', '凹透镜', '平面镜', '折射', '反射',
            '化合价', '化学式', '化学价', '离子键', '共价键',
            '有丝分裂', '减数分裂', '染色体', '基因突变',
            '分封制', '郡县制', '科举制', '行省制',
            '季风气候', '温带大陆', '亚热带', '热带雨林',
            '主谓宾', '定状补', '文言文', '现代文',
            '过去式', '现在完成', '过去完成', '将来时',
            '被动语态', '虚拟语气', '定语从句', '状语从句',
            '宾语从句', '表语从句', '主语从句',
            '有理数', '无理数', '实数', '虚数', '复数',
            '因式分解', '整式', '分式', '二次根式',
            '一次函数', '二次函数', '反比例函数', '指数函数', '对数函数',
            '幂函数', '分段函数', '复合函数',
            '相似三角形', '全等三角形', '等腰三角形', '等边三角形',
            '直角三角形', '锐角三角形', '钝角三角形',
            '平行四边形', '矩形', '菱形', '正方形', '梯形',
            '圆的周长', '圆的面积', '扇形', '弓形',
            '棱柱', '棱锥', '圆柱', '圆锥', '球',
            '排列组合', '二项式定理',
            '加速度', '匀加速', '自由落体', '竖直上抛',
            '平抛运动', '圆周运动', '万有引力',
            '压强', '浮力', '阿基米德',
            '比热容', '热值', '热机',
            '串联', '并联', '电功率', '电功', '焦耳定律',
            '安培力', '洛伦兹力', '电磁感应', '法拉第',
            '化学键', '分子间作用力', '氢键',
            '物质的量', '摩尔', '摩尔质量', '气体摩尔体积',
            '化学平衡', '电离平衡', '水解平衡', '溶解平衡',
            '原电池', '电解池', '电镀',
            '有机化学', '无机化学', '烃', '醇', '醛', '酸', '酯',
            '蛋白质', '核酸', '糖类', '脂质',
            '细胞呼吸', '有氧呼吸', '无氧呼吸',
            '有丝分裂', '减数分裂', '细胞分化', '细胞衰老', '细胞凋亡',
            'DNA复制', '转录', '翻译', '中心法则',
            '基因突变', '基因重组', '染色体变异',
            '自然选择', '人工选择', '隔离', '物种形成',
            '种群', '群落', '生态系统', '食物链', '食物网',
            '能量流动', '物质循环', '信息传递',
            '禅让制', '世袭制', '宗法制', '礼乐制',
            '春秋五霸', '战国七雄', '百家争鸣',
            '文景之治', '贞观之治', '开元盛世', '康乾盛世',
            '丝绸之路', '郑和下西洋', '闭关锁国',
            '鸦片战争', '太平天国', '洋务运动', '戊戌变法', '辛亥革命',
            '五四运动', '抗日战争', '解放战争',
            '文艺复兴', '启蒙运动', '工业革命', '法国大革命',
            '经线', '纬线', '赤道', '本初子午线',
            '板块构造', '大陆漂移', '火山地震',
            '天气', '气候', '气温', '降水',
            '长江', '黄河', '珠江', '淮河',
            '青藏高原', '黄土高原', '云贵高原', '内蒙古高原',
            '东北平原', '华北平原', '长江中下游平原',
            '塔里木盆地', '准噶尔盆地', '柴达木盆地', '四川盆地',
            '商品', '货币', '价格', '价值', '供求',
            '市场经济', '计划经济', '宏观调控',
            '国体', '政体', '人民代表大会', '民族区域自治',
            '唯物论', '辩证法', '认识论', '历史唯物主义',
            '联系', '发展', '矛盾', '质量互变', '否定之否定',
            '是什么', '为什么', '怎么办', '解释', '讲解',
            '什么是', '怎么算', '如何理解', '详解',
        ]
        
        query_lower = query.lower()
        
        for keyword in subject_keywords:
            if keyword.lower() in query_lower:
                return True
        
        if len(query) <= 4:
            return True
        
        if self._is_real_ai_available():
            try:
                return self._check_subject_with_ai(query)
            except:
                pass
        
        return True
    
    def _check_subject_with_ai(self, query):
        import requests
        headers = {
            'api-key': self.api_key,
            'Content-Type': 'application/json'
        }
        
        system_prompt = """你是一个严格的内容审核员。请判断用户的问题是否属于中小学学科知识范畴（数学、语文、英语、物理、化学、生物、历史、地理、政治等）。

只回复一个字：是或否
- 如果是学科知识相关问题，回复：是
- 如果是其他内容（闲聊、娱乐、生活、编程、商业、成人内容、违法内容等），回复：否"""
        
        user_prompt = f"请判断以下问题是否属于中小学学科知识范畴：\n{query}"
        
        payload = {
            'model': self.model,
            'messages': [
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt}
            ],
            'temperature': 0,
            'max_completion_tokens': 20,
            'thinking': {
                'type': 'disabled'
            }
        }
        
        try:
            response = requests.post(self.api_url, headers=headers, json=payload, timeout=30)
            if response.status_code == 200:
                result = response.json()
                ai_text = result['choices'][0]['message']['content'].strip()
                return '是' in ai_text
        except Exception as e:
            print(f"[AI审核] 调用失败: {e}")
        
        return False

    def _call_knowledge_with_ai(self, query, subject, preset_content=None):
        import requests
        
        headers = {
            'api-key': self.api_key,
            'Content-Type': 'application/json'
        }
        
        system_prompt = """你是一位专业的中小学知识辅导老师。请用简洁清晰的语言解释学科知识，5秒内给出核心解答。

【输出格式】
【概念】1-2句话简明定义
【要点】3-5个核心要点，每条不超过20字
【公式/方法】如有公式或解题方法，直接列出（用LaTeX格式 $...$）
【例题】1道典型小题+简要解答

【要求】
- 语言精练，不啰嗦
- 准确实用，适合中小学生
- 公式用$...$包裹，如 $a^2+b^2=c^2$
- 总字数控制在300字以内
- 用中文回答"""
        
        user_prompt = f"解释：{query}"

        payload = {
            'model': self.model,
            'messages': [
                {'role': 'system', 'content': system_prompt},
                {'role': 'user', 'content': user_prompt}
            ],
            'temperature': 0.6,
            'max_completion_tokens': 1500,
            'thinking': {
                'type': 'disabled'
            }
        }

        response = requests.post(self.api_url, headers=headers, json=payload, timeout=30)

        if response.status_code == 200:
            result = response.json()
            ai_text = result['choices'][0]['message']['content']
            ai_text = self._clean_latex(ai_text)
            return {
                'query': query,
                'subject': subject,
                'content': ai_text,
                'source': 'ai'
            }
        else:
            print(f"[MiMo] API返回错误: {response.status_code} - {response.text}")
            return self._mock_knowledge_query(query, subject)

    def _search_with_brave(self, query):
        """使用Brave免费搜索API"""
        try:
            brave_api_key = current_app.config.get('BRAVE_API_KEY', '')
            if not brave_api_key:
                print("[Brave] 未配置API Key，跳过搜索")
                return None
            
            import requests
            headers = {
                'Accept': 'application/json',
                'X-Subscription-Token': brave_api_key
            }
            
            params = {
                'q': query,
                'count': 5,
                'language': 'zh-CN'
            }
            
            response = requests.get(
                'https://api.search.brave.com/res/v1/web/search',
                headers=headers,
                params=params,
                timeout=10
            )
            
            if response.status_code == 200:
                data = response.json()
                results = []
                
                web_results = data.get('web', {}).get('results', [])
                for item in web_results[:5]:
                    title = item.get('title', '')
                    description = item.get('description', '')
                    if title and description:
                        results.append(f"- {title}: {description}")
                
                if results:
                    return '\n'.join(results)
                return None
            else:
                print(f"[Brave] 搜索API返回错误: {response.status_code}")
                return None
                
        except Exception as e:
            print(f"[Brave] 搜索失败: {e}")
            return None

    def _mock_knowledge_query(self, query, subject):
        content = f"""【概念定义】
{query}是{subject or '知识领域'}中的一个重要概念，指的是与{query}相关的一系列知识和原理的总称。

【核心要点】
1. {query}的基本定义和内涵
2. {query}的主要特征和性质
3. {query}的核心原理和规律
4. {query}的常见表现形式
5. {query}的重要意义和作用
6. {query}的发展历程和演变

【应用场景】
1. 日常生活中的应用
2. 学习考试中的应用
3. 工作实践中的应用
4. 科学研究中的应用

【相关知识】
1. 与{query}相关的基础概念
2. {query}的延伸和拓展知识
3. {query}在各学科中的应用
4. 著名的相关理论和发现

【记忆口诀】
理解概念是基础，
核心要点要记牢，
多学多练多思考，
知识掌握更牢靠。
"""
        return {
            'query': query,
            'subject': subject,
            'content': content,
            'source': 'mock'
        }


ai_service = AIService()


def synthesize_speech(text, voice='Mia', format='wav'):
    """使用Mimo TTS生成语音"""
    try:
        import requests
        import base64
        
        api_key = current_app.config.get('AI_API_KEY', '')
        api_url = current_app.config.get('AI_API_URL', '')
        
        if not api_key or not api_url:
            return None
        
        headers = {
            'api-key': api_key,
            'Content-Type': 'application/json'
        }
        
        payload = {
            'model': 'mimo-v2.5-tts',
            'messages': [
                {
                    'role': 'user',
                    'content': 'Clear, natural pronunciation, moderate pace, suitable for vocabulary learning.'
                },
                {
                    'role': 'assistant',
                    'content': text
                }
            ],
            'audio': {
                'format': format,
                'voice': voice
            }
        }
        
        response = requests.post(api_url, headers=headers, json=payload, timeout=30)
        
        if response.status_code == 200:
            result = response.json()
            audio_data = result['choices'][0]['message']['audio']['data']
            return base64.b64decode(audio_data)
        else:
            print(f"[TTS] API返回错误: {response.status_code} - {response.text[:200]}")
            return None
            
    except Exception as e:
        print(f"[TTS] 调用失败: {e}")
        return None


def query_vocabulary(word):
    """查询英语词汇，返回释义、音标、例句等"""
    word = word.strip().lower()
    
    if not word or not re.match(r'^[a-zA-Z][a-zA-Z\s\-]*$', word):
        return {
            'word': word,
            'error': '请输入有效的英文单词'
        }
    
    if current_app and current_app.config.get('AI_API_KEY'):
        try:
            return _query_vocabulary_with_ai(word)
        except Exception as e:
            print(f"[词汇] AI查询失败: {e}，使用模拟数据")
            return _mock_vocabulary(word)
    else:
        return _mock_vocabulary(word)


def _query_vocabulary_with_ai(word):
    import requests
    
    api_key = current_app.config.get('AI_API_KEY', '')
    api_url = current_app.config.get('AI_API_URL', '')
    model = current_app.config.get('AI_MODEL', '')
    
    headers = {
        'api-key': api_key,
        'Content-Type': 'application/json'
    }
    
    system_prompt = """你是一位专业的英语词典。请查询给定的英语单词，输出标准的词典条目。

严格按以下JSON格式输出，不要输出其他内容：
{
    "word": "单词原形",
    "phonetic": "/音标/",
    "part_of_speech": "词性（如 n. v. adj.）",
    "meaning_cn": "中文释义",
    "meaning_en": "英文释义",
    "example_en": "英文例句",
    "example_cn": "例句中文翻译",
    "plural_form": "复数形式（如果是名词）",
    "past_tense": "过去式（如果是动词）",
    "past_participle": "过去分词（如果是动词）",
    "synonyms": ["同义词1", "同义词2"],
    "antonyms": ["反义词1", "反义词2"],
    "collocations": ["搭配1", "搭配2", "搭配3"]
}"""
    
    user_prompt = f"查询单词：{word}"
    
    payload = {
        'model': model,
        'messages': [
            {'role': 'system', 'content': system_prompt},
            {'role': 'user', 'content': user_prompt}
        ],
        'temperature': 0.3,
        'max_completion_tokens': 1500,
        'thinking': {
            'type': 'disabled'
        }
    }
    
    response = requests.post(api_url, headers=headers, json=payload, timeout=30)
    
    if response.status_code == 200:
        result = response.json()
        ai_text = result['choices'][0]['message']['content']
        
        try:
            json_match = re.search(r'\{[\s\S]*\}', ai_text)
            if json_match:
                data = json.loads(json_match.group(0))
                data['word'] = word
                return data
        except:
            pass
        
        return _mock_vocabulary(word)
    else:
        print(f"[词汇] API返回错误: {response.status_code}")
        return _mock_vocabulary(word)


def _mock_vocabulary(word):
    mock_data = {
        'abandon': {
            'word': 'abandon',
            'phonetic': '/əˈbændən/',
            'part_of_speech': 'v.',
            'meaning_cn': '放弃；遗弃；抛弃',
            'meaning_en': 'to leave someone or something permanently',
            'example_en': 'He abandoned his car in the snow.',
            'example_cn': '他把车丢弃在雪地里。',
            'past_tense': 'abandoned',
            'past_participle': 'abandoned',
            'synonyms': ['desert', 'forsake', 'leave'],
            'antonyms': ['keep', 'maintain', 'support'],
            'collocations': ['abandon hope', 'abandon ship', 'abandon oneself to']
        },
        'beautiful': {
            'word': 'beautiful',
            'phonetic': '/ˈbjuːtɪfl/',
            'part_of_speech': 'adj.',
            'meaning_cn': '美丽的；漂亮的',
            'meaning_en': 'having beauty; pleasing to the senses',
            'example_en': 'She has a beautiful smile.',
            'example_cn': '她有美丽的笑容。',
            'synonyms': ['pretty', 'gorgeous', 'lovely'],
            'antonyms': ['ugly', 'hideous', 'plain'],
            'collocations': ['beautiful day', 'beautiful view', 'beautiful music']
        },
        'computer': {
            'word': 'computer',
            'phonetic': '/kəmˈpjuːtər/',
            'part_of_speech': 'n.',
            'meaning_cn': '计算机；电脑',
            'meaning_en': 'an electronic machine for processing data',
            'example_en': 'I use a computer for work every day.',
            'example_cn': '我每天用电脑工作。',
            'plural_form': 'computers',
            'synonyms': ['PC', 'laptop', 'workstation'],
            'antonyms': [],
            'collocations': ['personal computer', 'computer program', 'computer science']
        }
    }
    
    if word in mock_data:
        return mock_data[word]
    
    return {
        'word': word,
        'phonetic': f"/{word}/",
        'part_of_speech': 'n./v./adj.',
        'meaning_cn': f'{word}的中文释义（示例数据）',
        'meaning_en': f'The meaning of {word} (sample data)',
        'example_en': f'This is a sentence with {word}.',
        'example_cn': f'这是一个包含{word}的句子。',
        'synonyms': ['synonym1', 'synonym2'],
        'antonyms': ['antonym1'],
        'collocations': [f'{word} phrase', f'{word} usage', f'common {word}']
    }

const API_BASE = window.location.origin;

let currentUser = null;
let token = localStorage.getItem('parent_token');
let selectedStudentId = localStorage.getItem('selected_student_id');

function checkAppUpdate() {
    if (typeof NativeApp !== 'undefined' && NativeApp.checkUpdate) {
        NativeApp.checkUpdate();
    } else {
        showToast('当前为网页版，无需更新');
    }
}

function initAppVersion() {
    if (typeof NativeApp !== 'undefined' && NativeApp.getVersionName) {
        const ver = NativeApp.getVersionName();
        const el = document.getElementById('update-version');
        if (el) el.textContent = 'v' + ver;
    }
}

function api(url, options = {}) {
    const headers = {
        'Content-Type': 'application/json',
        ...options.headers
    };
    if (token) {
        headers['Authorization'] = 'Bearer ' + token;
    }
    return fetch(API_BASE + url, {
        ...options,
        headers
    }).then(r => r.json());
}

function showToast(msg, duration = 2000) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.add('show');
    setTimeout(() => toast.classList.remove('show'), duration);
}

function showLoading(show = true) {
    const loading = document.getElementById('loading');
    if (show) loading.classList.add('active');
    else loading.classList.remove('active');
}

function navigateTo(pageId) {
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.getElementById(pageId).classList.add('active');
    
    document.querySelectorAll('.nav-item').forEach(n => n.classList.remove('active'));
    const navMap = { 
        'home-page': 'nav-home', 
        'records-page': 'nav-records', 
        'analysis-page': 'nav-analysis',
        'profile-page': 'nav-profile' 
    };
    if (navMap[pageId]) {
        document.getElementById(navMap[pageId]).classList.add('active');
    }
}

function checkLogin() {
    if (!token) {
        showLoginPage();
        return false;
    }
    return true;
}

function showLoginPage() {
    document.getElementById('login-page').style.display = 'flex';
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelector('.bottom-nav').style.display = 'none';
}

function hideLoginPage() {
    document.getElementById('login-page').style.display = 'none';
    document.querySelector('.bottom-nav').style.display = 'flex';
}

let isRegisterMode = false;

function toggleLoginMode() {
    isRegisterMode = !isRegisterMode;
    const title = document.getElementById('login-title');
    const btn = document.getElementById('login-btn');
    const toggle = document.getElementById('login-toggle');
    const nicknameGroup = document.getElementById('register-nickname');
    
    if (isRegisterMode) {
        title.textContent = '家长注册';
        btn.textContent = '注册';
        toggle.textContent = '已有账号？去登录';
        nicknameGroup.style.display = 'block';
    } else {
        title.textContent = '家长登录';
        btn.textContent = '登录';
        toggle.textContent = '没有账号？去注册';
        nicknameGroup.style.display = 'none';
    }
}

async function doLogin() {
    const username = document.getElementById('login-username').value.trim();
    const password = document.getElementById('login-password').value;
    
    if (!username || !password) {
        showToast('请输入用户名和密码');
        return;
    }
    
    showLoading(true);
    
    try {
        let result;
        if (isRegisterMode) {
            const nickname = document.getElementById('login-nickname').value.trim();
            result = await api('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ username, password, role: 'parent', nickname })
            });
        } else {
            result = await api('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ username, password, role: 'parent' })
            });
        }
        
        if (result.code === 200 && result.data) {
            token = result.data.token;
            currentUser = result.data.user;
            localStorage.setItem('parent_token', token);
            localStorage.setItem('parent_user', JSON.stringify(currentUser));
            
            showToast(isRegisterMode ? '注册成功' : '登录成功');
            hideLoginPage();
            initApp();
        } else {
            showToast(result.msg || '操作失败');
        }
    } catch (e) {
        showToast('网络错误：' + e.message);
    }
    
    showLoading(false);
}

function logout() {
    if (confirm('确定要退出登录吗？')) {
        token = null;
        currentUser = null;
        localStorage.removeItem('parent_token');
        localStorage.removeItem('parent_user');
        localStorage.removeItem('selected_student_id');
        showLoginPage();
    }
}

function initApp() {
    if (!currentUser) {
        const saved = localStorage.getItem('parent_user');
        if (saved) currentUser = JSON.parse(saved);
    }
    
    initAppVersion();
    
    if (currentUser) {
        document.getElementById('profile-name').textContent = currentUser.nickname || currentUser.username;
        document.getElementById('profile-username').textContent = '账号：' + currentUser.username;
    }
    
    loadStudents();
    loadRecords();
    navigateTo('home-page');
}

async function loadStudents() {
    try {
        const result = await api('/api/parent/students');
        if (result.code === 200 && result.data) {
            const students = result.data;
            const container = document.getElementById('student-list');
            
            if (students.length === 0) {
                container.innerHTML = '<div style="text-align:center;padding:20px;color:#999;">暂无绑定学生</div>';
                return;
            }
            
            container.innerHTML = students.map(s => `
                <div class="student-item ${s.id == selectedStudentId ? 'active' : ''}" onclick="selectStudent(${s.id})">
                    <div class="avatar">${(s.nickname || s.username).charAt(0)}</div>
                    <div class="student-info" style="flex:1;">
                        <div class="name">${s.nickname || s.username}</div>
                        <div class="grade">${s.grade || '未设置年级'}</div>
                    </div>
                    ${s.id == selectedStudentId ? '<div class="bind-btn">已选择</div>' : '<div class="bind-btn" style="color:#999;border-color:#ddd;">选择</div>'}
                </div>
            `).join('');
        }
    } catch (e) {
        console.error('加载学生列表失败', e);
    }
}

async function selectStudent(studentId) {
    selectedStudentId = studentId;
    localStorage.setItem('selected_student_id', studentId);
    loadStudents();
    loadRecords();
    showToast('已选择学生');
}

async function showBindDialog() {
    const studentUsername = prompt('请输入学生的用户名：');
    if (!studentUsername) return;
    
    showLoading(true);
    
    try {
        const result = await api('/api/parent/bind', {
            method: 'POST',
            body: JSON.stringify({ student_username: studentUsername })
        });
        
        if (result.code === 200) {
            showToast('绑定成功！');
            loadStudents();
        } else {
            showToast(result.msg || '绑定失败');
        }
    } catch (e) {
        showToast('网络错误：' + e.message);
    }
    
    showLoading(false);
}

async function loadRecords() {
    if (!selectedStudentId) {
        document.getElementById('records-list').innerHTML = 
            '<div class="empty-state"><div class="icon">👶</div><p>请先选择学生</p></div>';
        return;
    }
    
    try {
        const result = await api(`/api/parent/student/${selectedStudentId}/records?page=1&per_page=20`);
        if (result.code === 200 && result.data) {
            const items = result.data.items;
            const container = document.getElementById('records-list');
            
            if (items.length === 0) {
                container.innerHTML = '<div class="empty-state"><div class="icon">📝</div><p>该学生暂无搜题记录</p></div>';
                return;
            }
            
            container.innerHTML = items.map(r => `
                <div class="history-item" onclick="viewRecord(${r.id})">
                    <div style="display:flex;align-items:center;gap:8px;margin-bottom:8px;">
                        <span class="badge badge-primary">${r.subject || '未知'}</span>
                        <span class="badge ${r.difficulty === '困难' ? 'badge-danger' : r.difficulty === '中等' ? 'badge-warning' : 'badge-success'}">${r.difficulty || '未知'}</span>
                    </div>
                    <div class="title" style="font-size:14px;font-weight:normal;">${r.query_text || '(图片题目)'}</div>
                    <div class="time">${formatTime(r.created_at)}</div>
                </div>
            `).join('');
        }
    } catch (e) {
        console.error('加载记录失败', e);
    }
}

function viewRecord(id) {
    navigateTo('record-detail-page');
    loadRecordDetail(id);
}

async function loadRecordDetail(id) {
    const container = document.getElementById('record-detail-content');
    container.innerHTML = `
        <div style="text-align:center;padding:60px 20px;color:#999;">
            <div class="spinner" style="margin:0 auto 16px;"></div>
            <p>加载中...</p>
        </div>
    `;
    
    try {
        const result = await api(`/api/search/${id}`);
        if (result.code === 200 && result.data) {
            displayRecordDetail(result.data);
        } else {
            container.innerHTML = `
                <div style="text-align:center;padding:60px 20px;color:#999;">
                    <div style="font-size:60px;margin-bottom:16px;">❌</div>
                    <p>${result.msg || '加载失败'}</p>
                </div>
            `;
        }
    } catch (e) {
        container.innerHTML = `
            <div style="text-align:center;padding:60px 20px;color:#999;">
                <div style="font-size:60px;margin-bottom:16px;">❌</div>
                <p>网络错误：${e.message}</p>
            </div>
        `;
    }
}

function displayRecordDetail(record) {
    const container = document.getElementById('record-detail-content');
    
    let knowledgeHtml = '';
    if (record.knowledge_points) {
        try {
            const kps = JSON.parse(record.knowledge_points);
            if (Array.isArray(kps)) {
                knowledgeHtml = kps.map((k, i) => `<div class="point">${i + 1}. ${formatRichText(k)}</div>`).join('');
            }
        } catch (e) {
            knowledgeHtml = '<div class="point">' + formatRichText(record.knowledge_points) + '</div>';
        }
    }
    
    let imageHtml = '';
    if (record.query_image_url) {
        imageHtml = `
            <div class="detail-section">
                <h3>📷 题目图片</h3>
                <img src="${API_BASE}${record.query_image_url}" style="width:100%;border-radius:8px;" alt="题目图片">
            </div>
        `;
    }
    
    let solutionHtml = '';
    if (record.ai_solution) {
        solutionHtml = `
            <div class="detail-section">
                <h3>📝 解题过程</h3>
                <div class="detail-content">${record.ai_solution}</div>
            </div>
        `;
    }
    
    container.innerHTML = `
        <div class="detail-section" style="margin-top:0;">
            <div style="display:flex;align-items:center;gap:8px;margin-bottom:12px;">
                <span class="badge badge-primary">${record.subject || '未知学科'}</span>
                <span class="badge ${record.difficulty === '困难' ? 'badge-danger' : record.difficulty === '中等' ? 'badge-warning' : 'badge-success'}">难度：${record.difficulty || '未知'}</span>
            </div>
            <div style="font-size:12px;color:#999;">${formatTime(record.created_at)}</div>
        </div>
        
        ${imageHtml}
        
        ${record.query_text ? `
        <div class="detail-section">
            <h3>📋 题目描述</h3>
            <div class="detail-content">${record.query_text}</div>
        </div>
        ` : ''}
        
        <div class="detail-section">
            <h3>🤖 AI 解答</h3>
            <div class="detail-content">${record.ai_answer || '暂无解答'}</div>
        </div>
        
        ${solutionHtml}
        
        ${knowledgeHtml ? `
        <div class="detail-section">
            <h3>📚 知识点</h3>
            <div class="knowledge-points">${knowledgeHtml}</div>
        </div>
        ` : ''}
    `;
    renderMathInContainer(container);
}

function generateAnalysis() {
    if (!selectedStudentId) {
        showToast('请先选择学生');
        return;
    }
    
    showLoading(true);
    
    api(`/api/analysis/student/${selectedStudentId}`, {
        method: 'POST'
    }).then(result => {
        showLoading(false);
        if (result.code === 200 && result.data) {
            displayAnalysis(result.data);
        } else {
            showToast(result.msg || '分析失败');
        }
    }).catch(e => {
        showLoading(false);
        showToast('网络错误：' + e.message);
    });
}

function displayAnalysis(analysis) {
    navigateTo('analysis-detail-page');
    
    document.getElementById('analysis-time').textContent = '生成时间：' + formatTime(analysis.created_at || new Date().toISOString());
    
    if (analysis.overall_score) {
        document.getElementById('overall-score').textContent = analysis.overall_score + ' 分';
    }
    
    let weakPoints = '';
    try {
        if (analysis.weak_points) {
            const weak = typeof analysis.weak_points === 'string' ? JSON.parse(analysis.weak_points) : analysis.weak_points;
            if (Array.isArray(weak)) {
                weakPoints = weak.map((w, i) => `<div class="suggestion-item"><span class="num">${i+1}.</span><span>${w}</span></div>`).join('');
            }
        }
    } catch(e) {
        weakPoints = '<div class="suggestion-item"><span>' + analysis.weak_points + '</span></div>';
    }
    document.getElementById('weak-points').innerHTML = weakPoints || '<div style="color:#999;">暂无数据</div>';
    
    let strongPoints = '';
    try {
        if (analysis.strong_points) {
            const strong = typeof analysis.strong_points === 'string' ? JSON.parse(analysis.strong_points) : analysis.strong_points;
            if (Array.isArray(strong)) {
                strongPoints = strong.map((s, i) => `<div class="suggestion-item"><span class="num">${i+1}.</span><span>${s}</span></div>`).join('');
            }
        }
    } catch(e) {
        strongPoints = '<div class="suggestion-item"><span>' + analysis.strong_points + '</span></div>';
    }
    document.getElementById('strong-points').innerHTML = strongPoints || '<div style="color:#999;">暂无数据</div>';
    
    let suggestions = '';
    try {
        if (analysis.suggestions) {
            const sug = typeof analysis.suggestions === 'string' ? JSON.parse(analysis.suggestions) : analysis.suggestions;
            if (Array.isArray(sug)) {
                suggestions = sug.map((s, i) => `<div class="suggestion-item"><span class="num">${i+1}.</span><span>${s}</span></div>`).join('');
            }
        }
    } catch(e) {
        suggestions = '<div class="suggestion-item"><span>' + analysis.suggestions + '</span></div>';
    }
    document.getElementById('suggestions').innerHTML = suggestions || '<div style="color:#999;">暂无数据</div>';
    
    document.getElementById('learning-habits').innerHTML = formatRichText(analysis.learning_habits || '暂无分析');
    document.getElementById('overall-comment').innerHTML = formatRichText(analysis.overall_comment || '暂无分析');
    renderMathInContainer(document.getElementById('learning-habits'));
    renderMathInContainer(document.getElementById('overall-comment'));
    
    renderCharts(analysis);
}

let subjectChart = null;
let difficultyChart = null;
let trendChart = null;

function renderCharts(analysis) {
    if (typeof Chart === 'undefined') {
        setTimeout(() => renderCharts(analysis), 200);
        return;
    }
    
    const subjects = analysis.subject_distribution ? 
        (typeof analysis.subject_distribution === 'string' ? JSON.parse(analysis.subject_distribution) : analysis.subject_distribution) 
        : {};
    
    const subjectLabels = Object.keys(subjects);
    const subjectData = Object.values(subjects);
    
    const subjectCtx = document.getElementById('subject-chart').getContext('2d');
    if (subjectChart) subjectChart.destroy();
    subjectChart = new Chart(subjectCtx, {
        type: 'doughnut',
        data: {
            labels: subjectLabels.length ? subjectLabels : ['暂无数据'],
            datasets: [{
                data: subjectData.length ? subjectData : [1],
                backgroundColor: [
                    '#FF6B6B', '#4A90E2', '#50C878', '#FFA500', '#9B59B6',
                    '#1ABC9C', '#E74C3C', '#3498DB', '#F39C12', '#2ECC71'
                ],
                borderWidth: 0
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: {
                    position: 'right',
                    labels: {
                        font: { size: 12 },
                        boxWidth: 12,
                        padding: 8
                    }
                }
            }
        }
    });
    
    const difficulties = analysis.difficulty_distribution ? 
        (typeof analysis.difficulty_distribution === 'string' ? JSON.parse(analysis.difficulty_distribution) : analysis.difficulty_distribution)
        : {};
    
    const diffLabels = Object.keys(difficulties);
    const diffData = Object.values(difficulties);
    
    const diffCtx = document.getElementById('difficulty-chart').getContext('2d');
    if (difficultyChart) difficultyChart.destroy();
    difficultyChart = new Chart(diffCtx, {
        type: 'bar',
        data: {
            labels: diffLabels.length ? diffLabels : ['暂无数据'],
            datasets: [{
                label: '题目数量',
                data: diffData.length ? diffData : [0],
                backgroundColor: ['#50C878', '#FFA500', '#FF6B6B'],
                borderRadius: 6
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { stepSize: 1 }
                }
            }
        }
    });
    
    const trendCtx = document.getElementById('trend-chart').getContext('2d');
    if (trendChart) trendChart.destroy();
    trendChart = new Chart(trendCtx, {
        type: 'line',
        data: {
            labels: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'],
            datasets: [{
                label: '搜题次数',
                data: [3, 5, 2, 7, 4, 8, 6],
                borderColor: '#FF6B6B',
                backgroundColor: 'rgba(255, 107, 107, 0.1)',
                fill: true,
                tension: 0.4,
                pointBackgroundColor: '#FF6B6B',
                pointRadius: 4
            }]
        },
        options: {
            responsive: true,
            maintainAspectRatio: false,
            plugins: {
                legend: { display: false }
            },
            scales: {
                y: {
                    beginAtZero: true,
                    ticks: { stepSize: 2 }
                }
            }
        }
    });
}

function renderMathInContainer(container) {
    if (!container) return;
    requestAnimationFrame(() => {
        if (window.renderMathInElement) {
            try {
                renderMathInElement(container, {
                    delimiters: [
                        {left: '$$', right: '$$', display: true},
                        {left: '$', right: '$', display: false},
                        {left: '\\(', right: '\\)', display: false},
                        {left: '\\[', right: '\\]', display: true}
                    ],
                    throwOnError: false,
                    trust: true
                });
            } catch (e) {
                console.warn('KaTeX render error:', e);
            }
        }
    });
}

function formatRichText(text) {
    if (!text) return '';
    
    const formulaBlocks = [];
    let fi = 0;
    
    text = text.replace(/\$\$([\s\S]*?)\$\$/g, (m, f) => {
        const p = `__F_${fi}__`;
        formulaBlocks.push(f.trim());
        fi++;
        return p;
    });
    
    text = text.replace(/\$([^$\n]+?)\$/g, (m, f) => {
        const p = `__IF_${fi}__`;
        formulaBlocks.push(f.trim());
        fi++;
        return p;
    });
    
    const div = document.createElement('div');
    div.textContent = text;
    let html = div.innerHTML;
    
    formulaBlocks.forEach((formula, i) => {
        formula = formula.replace(/\^(\d+)/g, '<sup>$1</sup>');
        formula = formula.replace(/\^{([^{}]+)}/g, '<sup>$1</sup>');
        formula = formula.replace(/_(\d+)/g, '<sub>$1</sub>');
        formula = formula.replace(/_{([^{}]+)}/g, '<sub>$1</sub>');
        
        html = html.replace(
            `__F_${i}__`,
            `<div style="background:rgba(52,199,89,0.1);padding:12px;border-radius:8px;margin:8px 0;text-align:center;font-size:15px;line-height:1.8;border-left:4px solid #34c759;">$$${formula}$$</div>`
        );
        html = html.replace(
            `__IF_${i}__`,
            `<span style="background:rgba(52,199,89,0.08);padding:2px 6px;border-radius:4px;">$${formula}$</span>`
        );
    });
    
    html = html.replace(/\n/g, '<br>');
    
    return html;
}

function formatTime(timeStr) {
    if (!timeStr) return '';
    const d = new Date(timeStr);
    const now = new Date();
    const diff = now - d;
    
    if (diff < 60000) return '刚刚';
    if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前';
    if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前';
    if (diff < 604800000) return Math.floor(diff / 86400000) + '天前';
    
    return d.toLocaleDateString();
}

window.onload = function() {
    if (token) {
        hideLoginPage();
        initApp();
    } else {
        showLoginPage();
    }
};

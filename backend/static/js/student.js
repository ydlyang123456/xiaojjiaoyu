const API_BASE = window.location.origin;

let currentUser = null;
let token = localStorage.getItem('student_token');
let selectedSubject = localStorage.getItem('selected_subject') || '数学';
let currentMode = localStorage.getItem('app_mode') || '';

function selectMode(mode) {
    localStorage.setItem('app_mode', mode);
    localStorage.removeItem('student_token');
    localStorage.removeItem('student_user');
    
    if (mode === 'student') {
        window.location.href = '/student';
    } else if (mode === 'parent') {
        window.location.href = '/parent';
    }
}

function switchMode() {
    const currentMode = localStorage.getItem('app_mode') || 'student';
    const newMode = currentMode === 'student' ? 'parent' : 'student';
    
    localStorage.setItem('app_mode', newMode);
    localStorage.removeItem('student_token');
    localStorage.removeItem('student_user');
    
    if (newMode === 'student') {
        window.location.href = '/student';
    } else if (newMode === 'parent') {
        window.location.href = '/parent';
    }
}

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

function updateCurrentModeDisplay() {
    const mode = localStorage.getItem('app_mode') || 'student';
    const display = document.getElementById('current-mode-display');
    if (display) {
        display.textContent = mode === 'student' ? '学生' : '家长';
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
    }).then(r => {
        const contentType = r.headers.get('content-type') || '';
        if (contentType.includes('application/json')) {
            return r.json();
        } else {
            return r.text().then(text => {
                throw new Error('服务器返回了非JSON数据，请检查网络连接');
            });
        }
    }).catch(e => {
        if (e.message && e.message.includes('JSON')) {
            throw new Error('网络错误：数据解析失败');
        }
        throw e;
    });
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
        'knowledge-page': 'nav-knowledge',
        'search-page': 'nav-search', 
        'records-page': 'nav-home',
        'record-detail-page': 'nav-home',
        'profile-page': 'nav-profile',
        'pet-page': 'nav-pet',
        'leaderboard-page': 'nav-profile',
        'reader-page': null
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

function showChoosePage() {
    document.getElementById('choose-page').style.display = 'flex';
    document.getElementById('login-page').style.display = 'none';
    document.querySelectorAll('.page').forEach(p => p.classList.remove('active'));
    document.querySelector('.bottom-nav').style.display = 'none';
}

let isRegisterMode = false;

function toggleLoginMode() {
    isRegisterMode = !isRegisterMode;
    const title = document.getElementById('login-title');
    const btn = document.getElementById('login-btn');
    const toggle = document.getElementById('login-toggle');
    const nicknameGroup = document.getElementById('register-nickname');
    const gradeGroup = document.getElementById('register-grade');
    
    if (isRegisterMode) {
        title.textContent = '学生注册';
        btn.textContent = '注册';
        toggle.textContent = '已有账号？去登录';
        nicknameGroup.style.display = 'block';
        gradeGroup.style.display = 'block';
    } else {
        title.textContent = '学生登录';
        btn.textContent = '登录';
        toggle.textContent = '没有账号？去注册';
        nicknameGroup.style.display = 'none';
        gradeGroup.style.display = 'none';
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
            const grade = document.getElementById('login-grade').value.trim();
            result = await api('/api/auth/register', {
                method: 'POST',
                body: JSON.stringify({ username, password, role: 'student', nickname, grade })
            });
        } else {
            result = await api('/api/auth/login', {
                method: 'POST',
                body: JSON.stringify({ username, password, role: 'student' })
            });
        }
        
        if (result.code === 200 && result.data) {
            token = result.data.token;
            currentUser = result.data.user;
            localStorage.setItem('student_token', token);
            localStorage.setItem('student_user', JSON.stringify(currentUser));
            
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
        localStorage.removeItem('student_token');
        localStorage.removeItem('student_user');
        showLoginPage();
    }
}

function initApp() {
    if (!currentUser) {
        const saved = localStorage.getItem('student_user');
        if (saved) currentUser = JSON.parse(saved);
    }
    
    initAppVersion();
    
    if (currentUser) {
        document.getElementById('welcome-name').textContent = currentUser.nickname || currentUser.username;
        document.getElementById('profile-name').textContent = currentUser.nickname || currentUser.username;
        document.getElementById('profile-username').textContent = '账号：' + currentUser.username;
        document.getElementById('profile-grade').textContent = '年级：' + (currentUser.grade || '未设置');
    }
    
    loadQuestions();
    loadRecords();
    loadStats();
    navigateTo('home-page');
}

async function loadQuestions() {
    try {
        const result = await api('/api/questions/?page=1&per_page=10');
        if (result.code === 200 && result.data) {
            const items = result.data.items;
            const container = document.getElementById('recent-questions');
            document.getElementById('total-questions').textContent = '共 ' + result.data.total + ' 道题目';
            
            if (items.length === 0) {
                container.innerHTML = '<div class="empty-state"><div class="icon">📚</div><p>还没有上传题目</p></div>';
                return;
            }
            
            container.innerHTML = items.slice(0, 5).map(q => `
                <div class="question-item" onclick="viewQuestion(${q.id})">
                    <img src="${API_BASE}${q.image_url}" alt="题目图片" onerror="this.style.background='#f0f0f0'">
                    <div class="question-info">
                        <div class="subject">${q.subject || '未知学科'}</div>
                        <div class="time">${formatTime(q.created_at)}</div>
                        <div><span class="badge badge-primary">${q.status === 'pending' ? '待解答' : '已解答'}</span></div>
                    </div>
                </div>
            `).join('');
        }
    } catch (e) {
        console.error('加载题目失败', e);
    }
}

async function loadRecords() {
    try {
        const result = await api('/api/search/records?page=1&per_page=20');
        if (result.code === 200 && result.data) {
            const items = result.data.items;
            const container = document.getElementById('records-list');
            
            if (items.length === 0) {
                container.innerHTML = '<div class="empty-state"><div class="icon">🔍</div><p>还没有搜题记录</p></div>';
                return;
            }
            
            container.innerHTML = items.map(r => `
                <div class="question-item" onclick="viewRecord(${r.id})">
                    <div class="question-info" style="margin-left:0;flex:1;">
                        <div style="display:flex;align-items:center;gap:8px;margin-bottom:6px;">
                            <span class="badge badge-primary">${r.subject || '未知'}</span>
                            <span class="badge ${r.difficulty === '困难' ? 'badge-danger' : r.difficulty === '中等' ? 'badge-warning' : 'badge-success'}">${r.difficulty || '未知'}</span>
                        </div>
                        <div style="font-size:14px;color:#555;line-height:1.5;display:-webkit-box;-webkit-line-clamp:2;-webkit-box-orient:vertical;overflow:hidden;">
                            ${r.query_text || '(图片题目)'}
                        </div>
                        <div class="time" style="margin-top:6px;">${formatTime(r.created_at)}</div>
                    </div>
                </div>
            `).join('');
        }
    } catch (e) {
        console.error('加载记录失败', e);
    }
}

async function loadStats() {
    try {
        const result = await api('/api/search/records?page=1&per_page=100');
        if (result.code === 200 && result.data) {
            const items = result.data.items;
            const subjects = new Set();
            let difficultCount = 0;
            
            items.forEach(r => {
                if (r.subject) subjects.add(r.subject);
                if (r.difficulty === '困难') difficultCount++;
            });
            
            document.getElementById('stat-searches').textContent = result.data.total;
            document.getElementById('stat-subjects').textContent = subjects.size;
            document.getElementById('stat-difficult').textContent = difficultCount;
        }
    } catch (e) {
        console.error('加载统计失败', e);
    }
}

function openSearch() {
    navigateTo('search-page');
    resetSearchPage();
}

function resetSearchPage() {
    document.getElementById('search-preview').classList.remove('show');
    document.getElementById('search-preview').src = '';
    document.getElementById('search-text').value = '';
    document.getElementById('search-result').style.display = 'none';
    document.getElementById('search-subject').value = '数学';
}

let selectedImageFile = null;

function takePhoto() {
    const input = document.getElementById('camera-input');
    input.accept = 'image/*';
    input.capture = 'environment';
    input.click();
}

function chooseImage() {
    const input = document.getElementById('gallery-input');
    input.accept = 'image/*';
    input.removeAttribute('capture');
    input.click();
}

function handleImageSelect(input) {
    if (input.files && input.files[0]) {
        selectedImageFile = input.files[0];
        const reader = new FileReader();
        reader.onload = function(e) {
            const img = document.getElementById('search-preview');
            img.src = e.target.result;
            img.classList.add('show');
        };
        reader.readAsDataURL(selectedImageFile);
    }
}

async function doSearch() {
    const queryText = document.getElementById('search-text').value.trim();
    const subject = document.getElementById('search-subject').value;
    
    if (!selectedImageFile && !queryText) {
        showToast('请上传图片或输入题目');
        return;
    }
    
    showLoading(true);
    
    try {
        const formData = new FormData();
        if (selectedImageFile) {
            formData.append('image', selectedImageFile);
        }
        if (queryText) {
            formData.append('query_text', queryText);
        }
        formData.append('subject', subject);
        
        const response = await fetch(API_BASE + '/api/search/question', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });
        
        const result = await response.json();
        
        if (result.code === 200 && result.data) {
            displaySearchResult(result.data);
        } else {
            showToast(result.msg || '搜题失败');
        }
    } catch (e) {
        showToast('网络错误：' + e.message);
    }
    
    showLoading(false);
}

function displaySearchResult(record) {
    const resultDiv = document.getElementById('search-result');
    resultDiv.style.display = 'block';
    
    document.getElementById('result-subject').innerHTML = '<span class="badge badge-primary">' + (record.subject || '未知学科') + '</span>';
    document.getElementById('result-difficulty').innerHTML = '<span class="badge ' + (record.difficulty === '困难' ? 'badge-danger' : record.difficulty === '中等' ? 'badge-warning' : 'badge-success') + '">难度：' + (record.difficulty || '未知') + '</span>';
    document.getElementById('result-answer').innerHTML = formatRichText(record.ai_answer || '暂无解答');
    renderMathInContainer(document.getElementById('result-answer'));
    
    if (record.knowledge_points) {
        try {
            const kps = JSON.parse(record.knowledge_points);
            const el = document.getElementById('result-knowledge');
            el.innerHTML = kps.map((k, i) => `
                <div class="point">${i + 1}. ${formatRichText(k)}</div>
            `).join('');
            renderMathInContainer(el);
        } catch (e) {
            const el = document.getElementById('result-knowledge');
            el.innerHTML = '<div class="point">' + formatRichText(record.knowledge_points) + '</div>';
            renderMathInContainer(el);
        }
    }
    
    resultDiv.scrollIntoView({ behavior: 'smooth' });
}

function viewQuestion(id) {
    showToast('题目详情功能开发中');
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
    
    let html = text
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    
    html = html.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>');
    html = html.replace(/\*([^*]+?)\*/g, '<em>$1</em>');
    html = html.replace(/`([^`]+?)`/g, '<code style="background:rgba(74,144,226,0.08);padding:1px 5px;border-radius:3px;font-size:13px;">$1</code>');
    
    html = html.replace(/\n/g, '<br>');
    
    return html;
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
                <div class="detail-content">${formatRichText(record.ai_solution)}</div>
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
            <div class="detail-content">${formatRichText(record.ai_answer || '暂无解答')}</div>
        </div>
        
        ${solutionHtml}
        
        ${knowledgeHtml ? `
        <div class="detail-section">
            <h3>📚 知识点</h3>
            <div class="knowledge-points">${knowledgeHtml}</div>
        </div>
        ` : ''}
        
        <div class="detail-section" style="display:flex;gap:10px;margin-top:8px;">
            <button class="btn btn-primary" style="flex:1;border-radius:14px;font-size:14px;" onclick="startCorrection(${record.id})">✏️ 订正错题</button>
            <button class="btn" style="flex:1;border-radius:14px;font-size:14px;background:#f5f5f7;color:#1d1d1f;" onclick="openReader('${(record.ai_answer || '').replace(/'/g, "\\'")}', '${(record.query_text || 'AI解答').replace(/'/g, "\\'")}')">📖 阅读原文</button>
        </div>
        
        <div id="correction-area-${record.id}" style="display:none;background:white;border-radius:16px;padding:18px;margin-top:12px;box-shadow:var(--shadow-sm);">
            <h3 style="font-size:16px;font-weight:700;margin:0 0 12px 0;">📝 上传订正</h3>
            <div style="margin-bottom:12px;">
                <textarea id="correction-text-${record.id}" rows="3" placeholder="输入你的订正答案..." style="width:100%;padding:12px;border:1px solid #e5e5ea;border-radius:12px;font-size:14px;resize:none;box-sizing:border-box;"></textarea>
            </div>
            <div style="display:flex;gap:8px;margin-bottom:12px;">
                <label style="flex:1;display:flex;align-items:center;justify-content:center;padding:10px;border:2px dashed #e5e5ea;border-radius:12px;cursor:pointer;font-size:13px;color:#8e8e93;" id="correction-upload-label-${record.id}">
                    📷 上传订正图片
                    <input type="file" id="correction-image-${record.id}" accept="image/*" style="display:none;" onchange="handleCorrectionImage(${record.id}, this)">
                </label>
                <img id="correction-preview-${record.id}" style="display:none;width:60px;height:60px;object-fit:cover;border-radius:8px;" alt="预览">
            </div>
            <div style="display:flex;gap:8px;">
                <label style="display:flex;align-items:center;gap:6px;font-size:13px;color:#555;cursor:pointer;">
                    <input type="checkbox" id="correction-correct-${record.id}"> 我做对了
                </label>
            </div>
            <button class="btn btn-primary" style="margin-top:12px;border-radius:12px;font-size:14px;width:100%;" onclick="submitCorrection(${record.id})">提交订正</button>
        </div>
    `;
    renderMathInContainer(container);
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

const knowledgeQuickSuggestions = [
    { text: '勾股定理', icon: '📐' },
    { text: '牛顿第一定律', icon: '⚡' },
    { text: '元素周期表', icon: '🧪' },
    { text: '细胞结构', icon: '🧬' },
    { text: '比喻修辞手法', icon: '📖' },
    { text: '英语八大时态', icon: '🔤' },
    { text: '七大洲四大洋', icon: '🌍' },
    { text: '中国古代朝代顺序', icon: '📜' }
];

function initKnowledgePage() {
    renderQuickSuggestions();
    const input = document.getElementById('knowledge-search-input');
    if (input) {
        input.addEventListener('keypress', function(e) {
            if (e.key === 'Enter') {
                askAIKnowledge();
            }
        });
    }
}

function renderQuickSuggestions() {
    const container = document.getElementById('knowledge-quick-tags');
    if (!container) return;
    
    let html = '';
    for (const item of knowledgeQuickSuggestions) {
        html += `
            <div class="quick-tag" onclick="quickSearchKnowledge('${item.text}')"
                 style="display:flex;align-items:center;gap:6px;padding:10px 16px;background:var(--surface-1);border-radius:20px;font-size:14px;color:var(--text-primary);cursor:pointer;transition:all 0.25s;border:1px solid var(--border-light);">
                <span style="font-size:16px;">${item.icon}</span>
                <span style="font-weight:500;">${item.text}</span>
            </div>
        `;
    }
    
    container.innerHTML = html;
}

function quickSearchKnowledge(text) {
    const input = document.getElementById('knowledge-search-input');
    if (input) {
        input.value = text;
    }
    askAIKnowledge();
}

function searchKnowledge() {
    askAIKnowledge();
}

function formatKnowledgeContent(content) {
    if (!content) return '';
    
    let processedContent = content
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;')
        .replace(/'/g, '&#039;');
    
    processedContent = processedContent.replace(/\*\*([^*]+?)\*\*/g, '<strong>$1</strong>');
    processedContent = processedContent.replace(/\*([^*]+?)\*/g, '<em>$1</em>');
    
    const sections = [
        { title: '📚 概念定义', icon: '📚', keyword: '【📚 概念定义】', highlight: true },
        { title: '📐 核心公式', icon: '📐', keyword: '【📐 核心公式/定理】', highlight: false, special: 'formula' },
        { title: '💡 解题模板', icon: '💡', keyword: '【💡 解题模板/方法步骤】', highlight: false, special: 'steps' },
        { title: '📖 核心要点', icon: '📖', keyword: '【📖 核心要点】', highlight: false },
        { title: '📝 典型例题', icon: '📝', keyword: '【📝 典型例题】', highlight: false, special: 'example' },
        { title: '⚠️ 易错点', icon: '⚠️', keyword: '【⚠️ 易错点/注意事项】', highlight: false, special: 'warning' },
        { title: '🎯 应用场景', icon: '🎯', keyword: '【🎯 应用场景】', highlight: false },
        { title: '🔗 相关知识', icon: '🔗', keyword: '【🔗 相关知识拓展】', highlight: false },
        { title: '🎵 记忆口诀', icon: '🎵', keyword: '【🎵 记忆口诀/技巧】', highlight: false },
        { title: '📚 练习题', icon: '📚', keyword: '【练习题】', highlight: false, special: 'practice' }
    ];
    
    let html = '';
    
    for (const section of sections) {
        const regex = new RegExp(escapeRegExp(section.keyword) + '\\s*\\n?([\\s\\S]*?)(?=\\n【|$)');
        const match = processedContent.match(regex);
        if (match) {
            let text = match[1].trim();
            text = text.replace(/\n/g, '<br>');
            
            let style = 'background:white;border-radius:16px;padding:18px;margin-bottom:12px;box-shadow:var(--shadow-sm);';
            if (section.highlight) {
                style = 'background:linear-gradient(135deg,var(--brand-500),var(--brand-400));border-radius:16px;padding:18px;margin-bottom:12px;color:white;';
            }
            
            let contentStyle = 'font-size:14px;line-height:1.8;';
            if (section.highlight) {
                contentStyle += 'color:rgba(255,255,255,0.95);';
            } else {
                contentStyle += 'color:var(--text-secondary);';
            }
            
            if (section.special === 'formula') {
                contentStyle += '';
            }
            if (section.special === 'example' || section.special === 'practice') {
                contentStyle += 'background:rgba(74,144,226,0.08);padding:12px;border-radius:8px;border-left:4px solid var(--brand-500);';
            }
            if (section.special === 'warning') {
                contentStyle += 'background:rgba(255,152,0,0.1);padding:12px;border-radius:8px;border-left:4px solid #ff9800;';
            }
            
            html += `
                <div class="detail-section card" style="${style}">
                    <h3 style="font-size:16px;font-weight:700;margin:0 0 12px 0;display:flex;align-items:center;gap:8px;">${section.title}</h3>
                    <div class="detail-content" style="${contentStyle}">${text}</div>
                </div>
            `;
        }
    }
    
    if (!html) {
        html = `
            <div class="detail-section card" style="background:white;border-radius:16px;padding:18px;margin-bottom:12px;box-shadow:var(--shadow-sm);">
                <div class="detail-content" style="font-size:14px;color:var(--text-secondary);line-height:1.8;">${processedContent}</div>
            </div>
        `;
    }
    
    return html;
}

// 转义正则特殊字符
function escapeRegExp(string) {
    return string.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

async function askAIKnowledge() {
    const keyword = document.getElementById('knowledge-search-input')?.value?.trim();
    if (!keyword) {
        showToast('请输入查询内容');
        return;
    }
    
    const emptyState = document.getElementById('knowledge-empty-state');
    const resultSection = document.getElementById('knowledge-result-section');
    const container = document.getElementById('knowledge-detail-content');
    
    if (emptyState) emptyState.style.display = 'none';
    if (resultSection) resultSection.style.display = 'block';
    
    if (container) {
        container.innerHTML = `
            <div style="text-align:center;padding:60px 20px;">
                <div class="spinner" style="margin:0 auto 16px;"></div>
                <p style="color:var(--text-secondary);font-size:14px;">AI老师正在思考中...</p>
            </div>
        `;
    }
    
    try {
        const result = await api('/api/knowledge/query', {
            method: 'POST',
            body: JSON.stringify({
                query: keyword
            })
        });
        
        if (result.code === 200 && result.data) {
            const contentHtml = formatKnowledgeContent(result.data.content);
            if (container) {
                container.innerHTML = `
                    <div class="detail-section card" style="background:linear-gradient(135deg,var(--brand-500),var(--brand-400));border-radius:16px;padding:20px;margin-bottom:12px;color:white;">
                        <div style="display:flex;align-items:center;gap:12px;margin-bottom:8px;">
                            <div style="font-size:44px;">🤖</div>
                            <div>
                                <h2 style="margin:0;font-size:19px;font-weight:700;">${result.data.query}</h2>
                                <div style="font-size:12px;opacity:0.85;margin-top:4px;">AI智能解答</div>
                            </div>
                        </div>
                    </div>
                    ${contentHtml}
                    <div style="text-align:center;padding:12px;font-size:12px;color:var(--text-hint);">
                        由小米MiMo大模型生成 · 仅供学习参考
                    </div>
                `;
                renderMathInContainer(container);
            }
        } else {
            if (container) {
                container.innerHTML = `
                    <div style="text-align:center;padding:40px 20px;">
                        <div style="font-size:56px;margin-bottom:16px;">⚠️</div>
                        <p style="color:var(--text-primary);font-weight:500;margin-bottom:8px;">${result.msg || '查询失败'}</p>
                        <p style="color:var(--text-secondary);font-size:13px;">请尝试输入学科相关问题，如：勾股定理、牛顿定律等</p>
                    </div>
                `;
            }
        }
    } catch (e) {
        if (container) {
            container.innerHTML = `
                <div style="text-align:center;padding:40px 20px;">
                    <div style="font-size:56px;margin-bottom:16px;">❌</div>
                    <p style="color:var(--text-primary);font-weight:500;margin-bottom:8px;">网络错误</p>
                    <p style="color:var(--text-secondary);font-size:13px;">${e.message}</p>
                </div>
            `;
        }
    }
}

// ==================== 错题本订正功能 ====================
let correctionImages = {};

function startCorrection(recordId) {
    const area = document.getElementById('correction-area-' + recordId);
    if (area) {
        area.style.display = area.style.display === 'none' ? 'block' : 'none';
        if (area.style.display === 'block') {
            area.scrollIntoView({ behavior: 'smooth' });
        }
    }
}

function handleCorrectionImage(recordId, input) {
    if (input.files && input.files[0]) {
        correctionImages[recordId] = input.files[0];
        const reader = new FileReader();
        reader.onload = function(e) {
            const preview = document.getElementById('correction-preview-' + recordId);
            const label = document.getElementById('correction-upload-label-' + recordId);
            if (preview) {
                preview.src = e.target.result;
                preview.style.display = 'block';
            }
            if (label) label.style.display = 'none';
        };
        reader.readAsDataURL(input.files[0]);
    }
}

async function submitCorrection(recordId) {
    const text = document.getElementById('correction-text-' + recordId)?.value?.trim() || '';
    const isCorrect = document.getElementById('correction-correct-' + recordId)?.checked || false;
    
    if (!text && !correctionImages[recordId]) {
        showToast('请输入订正内容或上传图片');
        return;
    }
    
    showLoading(true);
    
    try {
        const formData = new FormData();
        formData.append('search_record_id', recordId);
        formData.append('corrected_text', text);
        formData.append('is_correct', isCorrect ? 'true' : 'false');
        
        if (correctionImages[recordId]) {
            formData.append('image', correctionImages[recordId]);
        }
        
        const response = await fetch(API_BASE + '/api/correction/upload', {
            method: 'POST',
            headers: {
                'Authorization': 'Bearer ' + token
            },
            body: formData
        });
        
        const result = await response.json();
        
        if (result.code === 200) {
            showToast(result.msg || '订正提交成功');
            const area = document.getElementById('correction-area-' + recordId);
            if (area) area.style.display = 'none';
            delete correctionImages[recordId];
        } else {
            showToast(result.msg || '提交失败');
        }
    } catch (e) {
        showToast('网络错误：' + e.message);
    }
    
    showLoading(false);
}

// ==================== 电子宠物功能 ====================
async function loadPetInfo() {
    try {
        const result = await api('/api/pet/info');
        if (result.code === 200 && result.data) {
            displayPetInfo(result.data);
        }
    } catch (e) {
        console.error('加载宠物信息失败', e);
    }
}

function displayPetInfo(pet) {
    const skinIcons = {
        'default': '🐱', 'dog': '🐶', 'bunny': '🐰', 'fox': '🦊',
        'panda': '🐼', 'unicorn': '🦄', 'dragon': '🐲', 'alien': '👽'
    };
    
    const icon = skinIcons[pet.skin] || '🐱';
    document.getElementById('pet-display').textContent = icon;
    document.getElementById('pet-name').textContent = pet.name;
    document.getElementById('pet-level').textContent = 'Lv.' + pet.level;
    
    const expPercent = pet.max_exp > 0 ? (pet.exp / pet.max_exp * 100) : 0;
    document.getElementById('pet-exp-bar').style.width = expPercent + '%';
    document.getElementById('pet-exp-text').textContent = pet.exp + '/' + pet.max_exp;
    
    document.getElementById('pet-hunger').textContent = '饱腹 ' + pet.hunger + '%';
    document.getElementById('pet-happiness').textContent = '心情 ' + pet.happiness + '%';
    document.getElementById('pet-corrected').textContent = '订正 ' + pet.total_corrected;
}

async function feedPet() {
    try {
        showLoading(true);
        const result = await api('/api/pet/feed', { method: 'POST', body: '{}' });
        if (result.code === 200) {
            showToast(result.msg);
            displayPetInfo(result.data);
        } else {
            showToast(result.msg || '喂食失败');
        }
    } catch (e) {
        showToast('网络错误');
    }
    showLoading(false);
}

async function playWithPet() {
    try {
        showLoading(true);
        const result = await api('/api/pet/play', { method: 'POST', body: '{}' });
        if (result.code === 200) {
            showToast(result.msg);
            displayPetInfo(result.data);
        } else {
            showToast(result.msg || '陪玩失败');
        }
    } catch (e) {
        showToast('网络错误');
    }
    showLoading(false);
}

function showPetSkins() {
    const section = document.getElementById('pet-skins-section');
    if (!section) return;
    
    const isHidden = section.style.display === 'none';
    section.style.display = isHidden ? 'block' : 'none';
    
    if (isHidden) {
        loadPetSkins();
    }
}

async function loadPetSkins() {
    try {
        const result = await api('/api/pet/info');
        if (result.code === 200 && result.data) {
            const pet = result.data;
            const skins = pet.skins || {};
            const grid = document.getElementById('pet-skins-grid');
            
            const skinIcons = {
                'default': '🐱', 'dog': '🐶', 'bunny': '🐰', 'fox': '🦊',
                'panda': '🐼', 'unicorn': '🦄', 'dragon': '🐲', 'alien': '👽'
            };
            
            grid.innerHTML = Object.entries(skins).map(([key, skin]) => {
                const isActive = pet.skin === key;
                const canUse = pet.level * 10 >= skin.cost;
                return `
                    <div onclick="${canUse ? `changePetSkin('${key}')` : ''}" 
                         style="text-align:center;padding:12px 8px;border-radius:12px;cursor:${canUse ? 'pointer' : 'not-allowed'};background:${isActive ? '#e3f2fd' : '#f5f5f7'};border:2px solid ${isActive ? '#007aff' : 'transparent'};opacity:${canUse ? 1 : 0.5};transition:all 0.2s;">
                        <div style="font-size:32px;margin-bottom:4px;">${skin.icon}</div>
                        <div style="font-size:11px;font-weight:600;color:#1d1d1f;">${skin.name}</div>
                        <div style="font-size:10px;color:#8e8e93;">${skin.cost > 0 ? skin.cost + '级' : '免费'}</div>
                        ${isActive ? '<div style="font-size:10px;color:#007aff;font-weight:600;">使用中</div>' : ''}
                    </div>
                `;
            }).join('');
        }
    } catch (e) {
        console.error('加载皮肤失败', e);
    }
}

async function changePetSkin(skinKey) {
    try {
        showLoading(true);
        const result = await api('/api/pet/change-skin', {
            method: 'POST',
            body: JSON.stringify({ skin: skinKey })
        });
        if (result.code === 200) {
            showToast(result.msg);
            loadPetInfo();
            loadPetSkins();
        } else {
            showToast(result.msg || '换装失败');
        }
    } catch (e) {
        showToast('网络错误');
    }
    showLoading(false);
}

function renamePet() {
    const newName = prompt('给宠物起个新名字：');
    if (newName && newName.trim()) {
        doRenamePet(newName.trim());
    }
}

async function doRenamePet(name) {
    try {
        showLoading(true);
        const result = await api('/api/pet/rename', {
            method: 'POST',
            body: JSON.stringify({ name })
        });
        if (result.code === 200) {
            showToast(result.msg);
            loadPetInfo();
        } else {
            showToast(result.msg || '改名失败');
        }
    } catch (e) {
        showToast('网络错误');
    }
    showLoading(false);
}

// ==================== 排行榜功能 ====================
async function loadLeaderboard() {
    try {
        const result = await api('/api/pet/leaderboard');
        if (result.code === 200 && result.data) {
            displayLeaderboard(result.data);
        }
    } catch (e) {
        console.error('加载排行榜失败', e);
    }
}

function displayLeaderboard(list) {
    const container = document.getElementById('leaderboard-list');
    if (!container) return;
    
    const skinIcons = {
        'default': '🐱', 'dog': '🐶', 'bunny': '🐰', 'fox': '🦊',
        'panda': '🐼', 'unicorn': '🦄', 'dragon': '🐲', 'alien': '👽'
    };
    
    const rankIcons = ['🥇', '🥈', '🥉'];
    
    if (list.length === 0) {
        container.innerHTML = '<div class="empty-state"><div class="icon">🏆</div><p>暂无排行数据</p></div>';
        return;
    }
    
    container.innerHTML = list.map((item, i) => {
        const rankIcon = i < 3 ? rankIcons[i] : (i + 1);
        const icon = skinIcons[item.pet_skin] || '🐱';
        return `
            <div style="display:flex;align-items:center;gap:12px;padding:14px 16px;background:white;border-radius:14px;margin-bottom:8px;box-shadow:0 2px 8px rgba(0,0,0,0.05);${i < 3 ? 'border:1px solid rgba(255,149,0,0.2);' : ''}">
                <div style="width:36px;text-align:center;font-size:${i < 3 ? '24px' : '16px'};font-weight:700;color:${i < 3 ? '#ff9500' : '#8e8e93'};">${rankIcon}</div>
                <div style="font-size:32px;">${icon}</div>
                <div style="flex:1;">
                    <div style="font-size:15px;font-weight:600;color:#1d1d1f;">${item.pet_name}</div>
                    <div style="font-size:12px;color:#8e8e93;">${item.student_name} · Lv.${item.level}</div>
                </div>
                <div style="text-align:right;">
                    <div style="font-size:13px;font-weight:600;color:#ff6b9d;">${item.total_corrected}题</div>
                    <div style="font-size:11px;color:#8e8e93;">${item.exp}exp</div>
                </div>
            </div>
        `;
    }).join('');
}

// ==================== 阅读原文全屏功能 ====================
function openReader(content, title) {
    navigateTo('reader-page');
    
    document.getElementById('reader-title').textContent = title || '阅读原文';
    
    const readerContent = document.getElementById('reader-content');
    let html = formatRichText(content || '暂无内容');
    html = html.replace(/<br>/g, '</p><p>');
    html = '<p>' + html + '</p>';
    readerContent.innerHTML = html;
    
    renderMathInContainer(readerContent);
    
    try {
        if (document.documentElement.requestFullscreen) {
            document.documentElement.requestFullscreen();
        }
    } catch (e) {}
}

function closeReader() {
    try {
        if (document.fullscreenElement) {
            document.exitFullscreen();
        }
    } catch (e) {}
    navigateTo('records-page');
}

function toggleFullscreen() {
    try {
        if (document.fullscreenElement) {
            document.exitFullscreen();
        } else {
            document.documentElement.requestFullscreen();
        }
    } catch (e) {
        showToast('全屏功能不可用');
    }
}

function loadBattlePage() {
    navigateTo('battle-page');
    document.getElementById('battle-start').style.display = 'block';
    document.getElementById('battle-area').style.display = 'none';
}

// ==================== 记忆卡片 ====================
async function loadFlashcards() {
    try {
        const result = await api('/api/flashcard/list');
        if (result.code === 200 && result.data) {
            renderFlashcards(result.data);
        }
        // 检查今日复习
        const todayResult = await api('/api/flashcard/today');
        if (todayResult.code === 200 && todayResult.data && todayResult.data.length > 0) {
            const reminder = document.getElementById('flashcard-reminder');
            reminder.textContent = `📅 今天有 ${todayResult.data.length} 张卡片需要复习`;
            reminder.style.display = 'block';
        }
    } catch (e) {
        showToast('加载失败');
    }
}

function renderFlashcards(cards) {
    const list = document.getElementById('flashcard-list');
    const empty = document.getElementById('flashcard-empty');
    const stageNames = ['新学', '1天后', '2天后', '4天后', '7天后', '15天后', '30天后', '已掌握'];
    
    if (!cards || cards.length === 0) {
        list.innerHTML = '';
        empty.style.display = 'block';
        return;
    }
    empty.style.display = 'none';
    
    list.innerHTML = cards.map(c => `
        <div id="card-${c.id}" style="background:white;border-radius:16px;padding:18px;margin-bottom:12px;box-shadow:0 2px 12px rgba(0,0,0,0.08);">
            <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:8px;">
                <div style="font-weight:700;font-size:16px;color:#1d1d1f;">${c.question}</div>
                <div onclick="deleteFlashcard(${c.id})" style="color:#ff3b30;cursor:pointer;font-size:18px;">✕</div>
            </div>
            <div style="background:#f7f7fa;border-radius:10px;padding:12px;font-size:14px;color:#3c3c43;margin-bottom:8px;border-left:3px solid #5856d6;">${c.answer}</div>
            <div style="height:4px;background:#e5e5ea;border-radius:2px;margin-bottom:8px;overflow:hidden;">
                <div style="height:100%;background:#34c759;border-radius:2px;width:${c.mastery}%;transition:width 0.3s;"></div>
            </div>
            <div style="display:flex;justify-content:space-between;font-size:11px;color:#8e8e93;margin-bottom:8px;">
                <span>${stageNames[c.review_stage] || '未知'}</span>
                <span>熟练度 ${c.mastery}% · 复习${c.review_count}次</span>
            </div>
            <div style="display:flex;gap:8px;">
                <button onclick="reviewFlashcard(${c.id},true)" style="flex:1;padding:8px;border:none;border-radius:10px;background:#34c759;color:white;font-size:13px;font-weight:600;cursor:pointer;">记住了</button>
                <button onclick="reviewFlashcard(${c.id},false)" style="flex:1;padding:8px;border:none;border-radius:10px;background:#fff;color:#ff3b30;font-size:13px;font-weight:600;cursor:pointer;border:1px solid #ff3b30;">忘记了</button>
            </div>
        </div>
    `).join('');
}

function showCreateFlashcard() {
    const q = prompt('输入知识点（如：勾股定理）：');
    if (!q) return;
    const a = prompt('输入答案/解释：');
    if (!a) return;
    createFlashcard(q, a);
}

async function createFlashcard(question, answer) {
    try {
        const result = await api('/api/flashcard/create', {
            method: 'POST',
            body: JSON.stringify({ question, answer })
        });
        if (result.code === 200) {
            showToast('创建成功');
            loadFlashcards();
        } else {
            showToast(result.msg);
        }
    } catch (e) {
        showToast('创建失败');
    }
}

async function reviewFlashcard(id, remembered) {
    try {
        const result = await api(`/api/flashcard/${id}/review`, {
            method: 'POST',
            body: JSON.stringify({ remembered })
        });
        if (result.code === 200) {
            showToast(remembered ? '已掌握 +1' : '继续加油');
            loadFlashcards();
        }
    } catch (e) {
        showToast('操作失败');
    }
}

async function deleteFlashcard(id) {
    if (!confirm('确定删除这张卡片吗？')) return;
    try {
        await api(`/api/flashcard/${id}`, { method: 'DELETE' });
        showToast('删除成功');
        loadFlashcards();
    } catch (e) {
        showToast('删除失败');
    }
}

async function exportFlashcards() {
    try {
        const result = await api('/api/flashcard/export');
        if (result.code === 400) {
            showToast(result.msg);
            return;
        }
        const win = window.open('', '_blank');
        win.document.write(result);
        win.document.close();
    } catch (e) {
        showToast('导出失败');
    }
}

// ==================== 宠物对战 ====================
let battleState = null;
let battleFormulas = [];
let battleOver = false;
let isPlayerTurn = true;

async function startBattleWeb() {
    try {
        const result = await api('/api/battle/start', { method: 'POST' });
        if (result.code === 200 && result.data) {
            battleState = result.data;
            battleOver = false;
            isPlayerTurn = true;
            document.getElementById('battle-start').style.display = 'none';
            document.getElementById('battle-area').style.display = 'block';
            document.getElementById('battle-log').innerHTML = '';
            updateBattleUI();
            addBattleLog('⚔️ 对战开始！');
            addBattleLog(`${battleState.player.pet_name} VS ${battleState.opponent.name}`);
            await loadBattleFormulas();
        }
    } catch (e) {
        showToast('开始失败');
    }
}

async function loadBattleFormulas() {
    try {
        const result = await api('/api/battle/formulas');
        if (result.code === 200 && result.data) {
            battleFormulas = result.data.sort((a, b) => b.mastery - a.mastery).slice(0, 4);
            renderBattleFormulas();
        }
    } catch (e) {}
}

function renderBattleFormulas() {
    const container = document.getElementById('battle-formulas');
    const colors = ['#4a90e2', '#50c878', '#ffa500', '#9b59b6'];
    container.innerHTML = battleFormulas.map((f, i) => `
        <button onclick="battleAttack('${f.id}')" 
            style="padding:10px;border:none;border-radius:12px;background:${colors[i]};color:white;font-size:12px;font-weight:600;cursor:pointer;text-align:center;">
            ${f.name}<br>
            <span style="font-size:10px;opacity:0.9;">${f.mastery >= 100 ? '⚡自动' : '📝解题'} · ${f.damage}伤</span>
        </button>
    `).join('');
}

function updateBattleUI() {
    if (!battleState) return;
    const skinEmojis = { default: '🐱', dog: '🐶', bunny: '🐰', fox: '🦊', panda: '🐼', unicorn: '🦄', dragon: '🐲', alien: '👽' };
    
    document.getElementById('player-emoji').textContent = skinEmojis[battleState.player.pet_skin] || '🐱';
    document.getElementById('player-name').textContent = battleState.player.pet_name;
    document.getElementById('player-level').textContent = 'Lv.' + battleState.player.level;
    document.getElementById('player-hp-bar').style.width = (battleState.player.hp / battleState.player.max_hp * 100) + '%';
    document.getElementById('player-hp-text').textContent = battleState.player.hp + '/' + battleState.player.max_hp;
    
    document.getElementById('opp-emoji').textContent = battleState.opponent.emoji;
    document.getElementById('opp-name').textContent = battleState.opponent.name;
    document.getElementById('opp-level').textContent = 'Lv.' + battleState.opponent.level;
    document.getElementById('opp-hp-bar').style.width = (battleState.opponent.hp / battleState.opponent.max_hp * 100) + '%';
    document.getElementById('opp-hp-text').textContent = battleState.opponent.hp + '/' + battleState.opponent.max_hp;
}

function addBattleLog(msg) {
    const log = document.getElementById('battle-log');
    log.innerHTML += msg + '<br>';
    log.scrollTop = log.scrollHeight;
}

async function battleAttack(formulaId) {
    if (!isPlayerTurn || battleOver) return;
    isPlayerTurn = false;
    
    try {
        const result = await api('/api/battle/attack', {
            method: 'POST',
            body: JSON.stringify({ formula_id: formulaId, skip_solve: true })
        });
        if (result.code === 200 && result.data) {
            if (result.data.auto) {
                applyBattleDamage(result.data.damage, false);
                addBattleLog(result.data.msg);
                setTimeout(opponentTurnWeb, 1200);
            } else {
                const p = result.data.problem;
                const answer = prompt(p.question + '\n\n提示：' + p.hint + '\n公式：' + p.formula);
                await submitBattleSolve(formulaId, answer || '', p);
            }
        }
    } catch (e) {
        isPlayerTurn = true;
    }
}

async function submitBattleSolve(formulaId, answer, problem) {
    try {
        const result = await api('/api/battle/solve', {
            method: 'POST',
            body: JSON.stringify({ formula_id: formulaId, answer, problem })
        });
        if (result.code === 200 && result.data) {
            addBattleLog(result.data.msg);
            if (result.data.correct) {
                applyBattleDamage(result.data.damage, false);
                if (result.data.pet_exp > 0) {
                    addBattleLog('🐾 宠物获得 ' + result.data.pet_exp + ' 经验');
                }
            }
            setTimeout(opponentTurnWeb, 1200);
        }
    } catch (e) {
        isPlayerTurn = true;
    }
}

function applyBattleDamage(damage, toPlayer) {
    if (toPlayer) {
        battleState.player.hp = Math.max(0, battleState.player.hp - damage);
        if (battleState.player.hp <= 0) endBattleWeb(false);
    } else {
        battleState.opponent.hp = Math.max(0, battleState.opponent.hp - damage);
        if (battleState.opponent.hp <= 0) endBattleWeb(true);
    }
    updateBattleUI();
}

async function opponentTurnWeb() {
    if (battleOver) return;
    try {
        const result = await api('/api/battle/opponent-attack', {
            method: 'POST',
            body: JSON.stringify({ opponent_level: battleState.opponent.level, opponent_name: battleState.opponent.name })
        });
        if (result.code === 200 && result.data) {
            addBattleLog(result.data.msg);
            applyBattleDamage(result.data.damage, true);
            if (!battleOver) {
                isPlayerTurn = true;
                addBattleLog('轮到你了，选择一个公式！');
            }
        }
    } catch (e) {
        isPlayerTurn = true;
    }
}

function endBattleWeb(playerWon) {
    battleOver = true;
    addBattleLog(playerWon ? '🎉 恭喜获胜！' : '💔 战斗失败，继续加油！');
    setTimeout(() => {
        if (confirm((playerWon ? '胜利！' : '失败！') + '\n' + (playerWon ? '你的宠物在战斗中获胜！' : '别灰心，多练练公式再来挑战！') + '\n\n点击确定再来一局')) {
            startBattleWeb();
        } else {
            loadBattlePage();
        }
    }, 1000);
}

// ==================== CSS动画 ====================
const petStyle = document.createElement('style');
petStyle.textContent = `
    @keyframes petBounce {
        0%, 100% { transform: translateY(0); }
        50% { transform: translateY(-8px); }
    }
    @keyframes floatApple {
        0%, 100% { transform: translateY(0); }
        50% { transform: translateY(-6px); }
    }
`;
document.head.appendChild(petStyle);

window.onload = function() {
    const hasSelectedMode = localStorage.getItem('app_mode');
    
    if (!hasSelectedMode) {
        showChoosePage();
        return;
    }
    
    if (token) {
        hideLoginPage();
        updateCurrentModeDisplay();
        initApp();
    } else {
        showLoginPage();
    }
};

const APP_CALLBACK = "orangecloud://oauth/callback";
const CALLBACK_PATH = "/oauth/callback";
const KV_PREFIX = "device:";
const COOKIE_NAME = "oc_admin";

// 管理面板页面（与预览版同一文件，构建时内嵌）
const ADMIN_HTML = `<!DOCTYPE html>
<html lang="zh-CN">
<head>
<meta charset="UTF-8">
<meta name="viewport" content="width=device-width, initial-scale=1.0">
<title>Orange Cloud · 设备管理控制台</title>
<link rel="preconnect" href="https://fonts.googleapis.com">
<link href="https://fonts.googleapis.com/css2?family=JetBrains+Mono:wght@300;400;500;700&family=Inter:wght@400;500;600;800&display=swap" rel="stylesheet">
<style>
:root{
  --accent:#22d3ee;
  --ok:#34d399;
  --warn:#fbbf24;
  --bad:#f87171;
  --bg:#050810;
  --panel:12,18,32;          /* rgb 三元组，配合 --fg-opacity 使用 */
  --fg-opacity:.72;
  --border:#1a2540;
  --text:#e5edf7;
  --text-dim:#7c8aa5;
  --mono:'JetBrains Mono','PingFang SC','Microsoft YaHei',monospace;
  --sans:'Inter','PingFang SC','Microsoft YaHei',sans-serif;
}
html[data-theme=light]{
  --bg:#eef1f6;
  --panel:255,255,255;
  --border:#d4dbe8;
  --text:#131a28;
  --text-dim:#5b6a85;
}
*{margin:0;padding:0;box-sizing:border-box}
::selection{background:var(--accent);color:#050810}
::-webkit-scrollbar{width:6px;height:6px}
::-webkit-scrollbar-thumb{background:var(--border);border-radius:3px}
::-webkit-scrollbar-thumb:hover{background:var(--accent)}
body{
  font-family:var(--sans);
  background:var(--bg);
  color:var(--text);
  min-height:100vh;
  overflow-x:hidden;
}
/* 自定义背景图层 */
#bgLayer{
  position:fixed;inset:0;z-index:-2;
  background-size:cover;background-position:center;
  transition:opacity .4s ease;
}
/* 扫描线质感 */
body::after{
  content:'';position:fixed;inset:0;z-index:60;pointer-events:none;
  background:repeating-linear-gradient(0deg,transparent 0 2px,rgba(255,255,255,.012) 2px 3px);
}
/* ============ 登录门 ============ */
#gate{
  position:fixed;inset:0;z-index:50;display:flex;align-items:center;justify-content:center;
  background:var(--bg);
}
#gate .card{
  width:min(400px,90vw);padding:40px 36px;
  background:rgba(var(--panel),var(--fg-opacity));
  backdrop-filter:blur(10px);-webkit-backdrop-filter:blur(10px);
  border:1px solid var(--border);border-radius:14px;
}
#gate h1{font-family:var(--mono);font-size:20px;font-weight:700;letter-spacing:.08em}
#gate .sub{font-size:12px;color:var(--text-dim);margin:8px 0 24px;letter-spacing:.04em}
#gate .radar{width:44px;height:44px;margin-bottom:20px}
#gateErr{color:var(--bad);font-size:12px;margin-top:12px;min-height:16px;font-family:var(--mono)}
/* ============ 主框架 ============ */
#app{display:none;min-height:100vh}
aside{
  position:fixed;left:0;top:0;bottom:0;width:248px;z-index:20;
  background:rgba(var(--panel),calc(var(--fg-opacity) * .9));
  backdrop-filter:blur(12px);-webkit-backdrop-filter:blur(12px);
  border-right:1px solid var(--border);
  display:flex;flex-direction:column;padding:24px 0;
}
.brand{display:flex;align-items:center;gap:12px;padding:0 24px 24px;border-bottom:1px solid var(--border)}
.brand .name{font-family:var(--mono);font-weight:700;font-size:14px;letter-spacing:.1em}
.brand .ver{font-size:10px;color:var(--text-dim);letter-spacing:.16em;margin-top:2px}
nav{padding:20px 12px;flex:1}
.nav-item{
  display:flex;align-items:center;gap:12px;padding:11px 14px;margin-bottom:4px;
  border-radius:9px;cursor:pointer;font-size:13px;font-weight:500;
  color:var(--text-dim);transition:all .15s ease;border:1px solid transparent;
}
.nav-item svg{width:16px;height:16px;stroke:currentColor;fill:none;stroke-width:1.8;stroke-linecap:round;stroke-linejoin:round}
.nav-item:hover{color:var(--text);background:rgba(var(--panel),.5)}
.nav-item.active{
  color:var(--accent);border-color:var(--border);
  background:rgba(34,211,238,.07);
}
.side-foot{padding:16px 24px 0;border-top:1px solid var(--border);font-family:var(--mono);font-size:10px;color:var(--text-dim);letter-spacing:.1em}
main{margin-left:248px;padding:32px 36px;max-width:1200px}
header.topbar{display:flex;align-items:baseline;justify-content:space-between;margin-bottom:28px}
h2.page-title{font-family:var(--mono);font-size:22px;font-weight:700;letter-spacing:.06em}
.crumb{font-family:var(--mono);font-size:11px;color:var(--text-dim);letter-spacing:.14em;text-transform:uppercase}
/* 统计行 */
.stats{display:grid;grid-template-columns:repeat(3,1fr);gap:16px;margin-bottom:24px}
.stat{
  padding:20px 22px;border-radius:14px;border:1px solid var(--border);
  background:rgba(var(--panel),var(--fg-opacity));
  backdrop-filter:blur(10px);-webkit-backdrop-filter:blur(10px);
  position:relative;overflow:hidden;
}
.stat .num{font-family:var(--mono);font-size:30px;font-weight:700;line-height:1.1}
.stat .lbl{font-family:var(--mono);font-size:10px;letter-spacing:.18em;text-transform:uppercase;color:var(--text-dim);margin-top:6px}
/* 玻璃卡片 */
.card{
  background:rgba(var(--panel),var(--fg-opacity));
  backdrop-filter:blur(10px);-webkit-backdrop-filter:blur(10px);
  border:1px solid var(--border);border-radius:14px;padding:24px;
}
.card-head{display:flex;align-items:center;justify-content:space-between;margin-bottom:18px;flex-wrap:wrap;gap:12px}
.card-title{font-family:var(--mono);font-size:13px;font-weight:700;letter-spacing:.14em;text-transform:uppercase;display:flex;align-items:center;gap:10px}
.dot{width:7px;height:7px;border-radius:50%;background:var(--ok);animation:pulse 1.6s ease-in-out infinite}
@keyframes pulse{0%,100%{box-shadow:0 0 0 0 rgba(52,211,153,.5)}50%{box-shadow:0 0 0 6px rgba(52,211,153,0)}}
.dot.off{background:var(--text-dim);animation:none}
/* 按钮：统一主色 */
.btn{
  font-family:var(--mono);font-size:12px;font-weight:600;letter-spacing:.06em;
  padding:8px 16px;border-radius:8px;cursor:pointer;
  border:1px solid var(--accent);background:var(--accent);color:#050810;
  transition:all .15s ease;
}
.btn:hover{filter:brightness(1.12);transform:translateY(-1px)}
.btn:active{transform:translateY(0)}
.btn.ghost{background:transparent;color:var(--accent)}
.btn.ghost:hover{background:rgba(34,211,238,.1)}
.btn.sm{padding:5px 12px;font-size:11px}
.btn:disabled{opacity:.4;cursor:not-allowed;transform:none}
/* 表格 */
table{width:100%;border-collapse:collapse;font-size:13px}
th{
  font-family:var(--mono);font-size:10px;letter-spacing:.16em;text-transform:uppercase;
  color:var(--text-dim);text-align:left;padding:10px 12px;border-bottom:1px solid var(--border);
}
td{padding:13px 12px;border-bottom:1px solid var(--border);vertical-align:middle}
tr:last-child td{border-bottom:none}
tbody tr{transition:background .12s}
tbody tr:hover{background:rgba(34,211,238,.04)}
.mono{font-family:var(--mono);font-size:12px}
.dim{color:var(--text-dim)}
.badge{
  display:inline-flex;align-items:center;gap:7px;
  font-family:var(--mono);font-size:11px;letter-spacing:.06em;
  padding:4px 11px;border-radius:100px;border:1px solid;
}
.badge .b-dot{width:6px;height:6px;border-radius:50%;background:currentColor}
.badge.ok{color:var(--ok);border-color:rgba(52,211,153,.4);background:rgba(52,211,153,.08)}
.badge.ok .b-dot{animation:pulse 1.6s ease-in-out infinite}
.badge.ban{color:var(--bad);border-color:rgba(248,113,113,.4);background:rgba(248,113,113,.08)}
.empty{
  border:1px dashed var(--border);border-radius:12px;padding:48px;
  text-align:center;color:var(--text-dim);font-family:var(--mono);font-size:12px;letter-spacing:.08em;
}
/* 设置区 */
.field{margin-bottom:22px}
.field label{
  display:block;font-family:var(--mono);font-size:10px;letter-spacing:.18em;
  text-transform:uppercase;color:var(--text-dim);margin-bottom:9px;
}
input[type=text],input[type=password],input[type=url]{
  width:100%;padding:10px 14px;border-radius:9px;
  border:1px solid var(--border);background:rgba(var(--panel),.5);
  color:var(--text);font-family:var(--mono);font-size:13px;outline:none;
  transition:border-color .15s;
}
input:focus{border-color:var(--accent);box-shadow:0 0 0 3px rgba(34,211,238,.12)}
input[type=range]{width:100%;accent-color:var(--accent);cursor:pointer}
.row-flex{display:flex;gap:12px;align-items:center}
.seg{display:flex;border:1px solid var(--border);border-radius:9px;overflow:hidden}
.seg button{
  flex:1;padding:9px 0;background:transparent;border:none;cursor:pointer;
  font-family:var(--mono);font-size:12px;color:var(--text-dim);transition:all .15s;
}
.seg button.on{background:var(--accent);color:#050810;font-weight:600}
.hint{font-size:11px;color:var(--text-dim);margin-top:8px;line-height:1.6}
/* 视图切换 */
.view{display:none}
.view.on{display:block;animation:fadeUp .3s ease}
@keyframes fadeUp{from{opacity:0;transform:translateY(8px)}to{opacity:1;transform:none}}
/* toast */
#toast{
  position:fixed;bottom:24px;right:24px;z-index:70;
  padding:12px 20px;border-radius:10px;font-family:var(--mono);font-size:12px;
  background:rgba(var(--panel),.92);border:1px solid var(--accent);color:var(--text);
  backdrop-filter:blur(8px);opacity:0;pointer-events:none;transition:opacity .25s,transform .25s;
  transform:translateY(8px);
}
#toast.show{opacity:1;transform:none}
@media(max-width:860px){
  aside{width:64px;padding:16px 0}
  .brand .name,.brand .ver,.nav-item span,.side-foot{display:none}
  .nav-item{justify-content:center;padding:12px}
  main{margin-left:64px;padding:20px 16px}
  .stats{grid-template-columns:1fr}
  table .hide-m{display:none}
}
</style>
</head>
<body>
<div id="bgLayer"></div>

<!-- 登录门 -->
<div id="gate">
  <div class="card">
    <svg class="radar" viewBox="0 0 44 44" fill="none">
      <circle cx="22" cy="22" r="19" stroke="#22d3ee" stroke-opacity=".3"/>
      <circle cx="22" cy="22" r="12" stroke="#22d3ee" stroke-opacity=".5"/>
      <circle cx="22" cy="22" r="5" fill="#22d3ee"/>
      <line x1="22" y1="22" x2="40" y2="10" stroke="#22d3ee" stroke-width="1.5">
        <animateTransform attributeName="transform" type="rotate" from="0 22 22" to="360 22 22" dur="4.2s" repeatCount="indefinite"/>
      </line>
    </svg>
    <h1>DEVICE CONSOLE</h1>
    <p class="sub">Orange Cloud 设备管理 · 仅限管理员</p>
    <div class="field">
      <label>管理密码</label>
      <div style="position:relative">
        <input type="password" id="pwd" placeholder="••••••••" autocomplete="current-password" style="padding-right:64px">
        <button type="button" id="pwdToggle" onclick="togglePwd()"
          style="position:absolute;right:6px;top:50%;transform:translateY(-50%);
          background:transparent;border:1px solid var(--border);border-radius:6px;
          color:var(--text-dim);font-family:var(--mono);font-size:10px;letter-spacing:.08em;
          padding:4px 9px;cursor:pointer">显示</button>
      </div>
    </div>
    <button class="btn" style="width:100%;padding:11px" onclick="login()">进入控制台</button>
    <div id="gateErr"></div>
  </div>
</div>

<!-- 主界面 -->
<div id="app">
  <aside>
    <div class="brand">
      <svg width="26" height="26" viewBox="0 0 44 44" fill="none">
        <circle cx="22" cy="22" r="19" stroke="#22d3ee" stroke-opacity=".4"/>
        <circle cx="22" cy="22" r="5" fill="#22d3ee"/>
      </svg>
      <div>
        <div class="name">OC CONSOLE</div>
        <div class="ver">DEVICE OPS v1.0</div>
      </div>
    </div>
    <nav>
      <div class="nav-item active" data-view="devices" onclick="switchView('devices')">
        <svg viewBox="0 0 24 24"><rect x="5" y="2" width="14" height="20" rx="2.5"/><line x1="10" y1="18.5" x2="14" y2="18.5"/></svg>
        <span>设备管理</span>
      </div>
      <div class="nav-item" data-view="settings" onclick="switchView('settings')">
        <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3.2"/><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 1 1-2.83 2.83l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 1 1-4 0v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 1 1-2.83-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 1 1 0-4h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 1 1 2.83-2.83l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 1 1 4 0v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 1 1 2.83 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 1 1 0 4h-.09a1.65 1.65 0 0 0-1.51 1z"/></svg>
        <span>面板设置</span>
      </div>
    </nav>
    <div class="side-foot">OSS.OMAIL.US.KG<br>KV · EDGE</div>
  </aside>

  <main>
    <!-- 设备管理视图 -->
    <section class="view on" id="view-devices">
      <header class="topbar">
        <h2 class="page-title">设备管理</h2>
        <span class="crumb">console / devices</span>
      </header>
      <div class="stats">
        <div class="stat"><div class="num" id="stTotal">0</div><div class="lbl">登记设备</div></div>
        <div class="stat"><div class="num" id="stActive" style="color:var(--ok)">0</div><div class="lbl">正常</div></div>
        <div class="stat warn"><div class="num" id="stBanned" style="color:var(--bad)">0</div><div class="lbl">已封禁</div></div>
      </div>
      <div class="card">
        <div class="card-head">
          <div class="card-title"><span class="dot" id="liveDot"></span>在线名单 · LIVE ROSTER</div>
          <div class="row-flex">
            <input type="text" id="search" placeholder="筛选机器码…" style="width:200px;padding:8px 12px" oninput="renderTable()">
            <button class="btn ghost sm" onclick="loadDevices()">刷新</button>
          </div>
        </div>
        <div id="tableWrap"></div>
      </div>
    </section>

    <!-- 设置视图 -->
    <section class="view" id="view-settings">
      <header class="topbar">
        <h2 class="page-title">面板设置</h2>
        <span class="crumb">console / settings</span>
      </header>
      <div class="card" style="max-width:560px">
        <div class="field">
          <label>主题 / THEME</label>
          <div class="seg">
            <button id="thDark" onclick="setTheme('dark')">深色</button>
            <button id="thLight" onclick="setTheme('light')">浅色</button>
          </div>
        </div>
        <div class="field">
          <label>背景图片 URL / BACKGROUND</label>
          <input type="url" id="bgUrl" placeholder="https://…（留空使用纯色）" onchange="setBg(this.value)">
          <p class="hint">支持任意可访问的图片地址；设置后自动覆盖面板底层。</p>
        </div>
        <div class="field">
          <label>前景不透明度 / PANEL OPACITY · <span id="opVal" class="mono"></span></label>
          <input type="range" id="opRange" min="20" max="100" oninput="setOpacity(this.value)">
          <p class="hint">控制磨砂玻璃卡片的不透明度，数值越低背景越通透。</p>
        </div>
        <div class="field">
          <button class="btn ghost" onclick="resetPrefs()">恢复默认外观</button>
        </div>
      </div>
    </section>
  </main>
</div>

<div id="toast"></div>

<script>
/* ================================================================
 * Orange Cloud 设备管理控制台
 * 优先对接 Worker 管理 API（/admin/api/*）；接口不可用时（预览模式）
 * 自动降级为内置模拟数据，便于直接预览界面。
 * ================================================================ */
const API = {
  async login(pwd){
    try{
      const r = await fetch('/admin/api/login',{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({password:pwd})});
      if(r.ok) return 'ok';
      if(r.status === 401) return 'denied';
      if(r.status === 503) return 'not_configured';
      return 'denied';
    }catch(e){ return previewLogin(pwd) ? 'ok' : 'denied'; }
  },
  async session(){
    try{
      const r = await fetch('/admin/api/devices');
      return r.ok;
    }catch(e){ return false; }
  },
  async list(){
    try{
      const r = await fetch('/admin/api/devices');
      if(!r.ok) throw 0;
      return await r.json();
    }catch(e){ return mockDevices(); }
  },
  async act(action,id){
    try{
      const r = await fetch('/admin/api/'+action,{method:'POST',headers:{'content-type':'application/json'},body:JSON.stringify({id})});
      if(!r.ok) throw 0;
      return true;
    }catch(e){ return mockAct(action,id); }
  }
};

/* ---------- 预览模式模拟数据 ---------- */
let _mock = null;
function previewLogin(pwd){ localStorage.setItem('oc_preview_gate','1'); return pwd.length >= 1; }
function mockDevices(){
  if(_mock) return _mock;
  const ids = ['a3f8c2e19b04d7a6','7e1b4d92c8f3a015','c9d6e2f8471b03aa','f2a7b1c9e4d60853','91d3e6c2a7f40b18'];
  const now = Date.now();
  _mock = { devices: ids.map((id,i)=>({
    id,
    firstSeen: now - (i+3)*86400000*4,
    lastSeen: now - i*3600000*7,
    app: i%2 ? 'oss' : 'play',
    version: '2.1.0',
    banned: i===3
  }))};
  return _mock;
}
function mockAct(action,id){
  const d = _mock && _mock.devices.find(x=>x.id===id);
  if(!d) return true;
  if(action==='ban') d.banned = true;
  if(action==='unban') d.banned = false;
  if(action==='delete') _mock.devices = _mock.devices.filter(x=>x.id!==id);
  return true;
}

/* ---------- 登录 ---------- */
let authed = false;
function enterApp(){
  authed = true;
  document.getElementById('gate').style.display = 'none';
  document.getElementById('app').style.display = 'block';
  loadDevices();
}
async function login(){
  const pwd = document.getElementById('pwd').value;
  const err = document.getElementById('gateErr');
  if(!pwd){ err.textContent = 'PASSWORD REQUIRED'; return; }
  const result = await API.login(pwd);
  if(result === 'ok'){
    enterApp();
  }else if(result === 'not_configured'){
    err.textContent = 'ADMIN NOT CONFIGURED · 未设置 ADMIN_PASSWORD';
  }else{
    err.textContent = 'ACCESS DENIED · 密码错误（注意全角/半角与输入法）';
  }
}
/* 已登录（Cookie 未过期）则跳过密码门，刷新不掉线 */
(async function restoreSession(){
  if(await API.session()) enterApp();
})();
document.getElementById('pwd').addEventListener('keydown', e=>{ if(e.key==='Enter') login(); });
/* 密码可见性切换 */
function togglePwd(){
  const i = document.getElementById('pwd');
  const show = i.type === 'password';
  i.type = show ? 'text' : 'password';
  document.getElementById('pwdToggle').textContent = show ? '隐藏' : '显示';
}

/* ---------- 视图切换 ---------- */
function switchView(v){
  document.querySelectorAll('.nav-item').forEach(n=>n.classList.toggle('active', n.dataset.view===v));
  document.querySelectorAll('.view').forEach(s=>s.classList.toggle('on', s.id==='view-'+v));
}

/* ---------- 设备名单 ---------- */
let devices = [];
async function loadDevices(){
  const data = await API.list();
  devices = data.devices || [];
  renderTable();
}
function fmt(t){
  const d = new Date(t);
  return d.getFullYear()+'-'+String(d.getMonth()+1).padStart(2,'0')+'-'+String(d.getDate()).padStart(2,'0')
    +' '+String(d.getHours()).padStart(2,'0')+':'+String(d.getMinutes()).padStart(2,'0');
}
function ago(t){
  const s = Math.max(1, Math.floor((Date.now()-t)/1000));
  if(s < 3600) return Math.floor(s/60)+' 分钟前';
  if(s < 86400) return Math.floor(s/3600)+' 小时前';
  return Math.floor(s/86400)+' 天前';
}
function renderTable(){
  const q = (document.getElementById('search').value||'').toLowerCase();
  const list = devices.filter(d=>d.id.toLowerCase().includes(q));
  document.getElementById('stTotal').textContent = devices.length;
  document.getElementById('stActive').textContent = devices.filter(d=>!d.banned).length;
  document.getElementById('stBanned').textContent = devices.filter(d=>d.banned).length;
  document.getElementById('liveDot').className = devices.length ? 'dot' : 'dot off';
  const wrap = document.getElementById('tableWrap');
  if(!list.length){
    wrap.innerHTML = '<div class="empty">NO DEVICES · 暂无登记设备<br><br>设备在用户完成登录后自动出现在这里</div>';
    return;
  }
  wrap.innerHTML = '<table><thead><tr>'+
    '<th>机器码 / DEVICE ID</th><th class="hide-m">客户端</th><th class="hide-m">首次登记</th><th>最近活跃</th><th>状态</th><th style="text-align:right">操作</th>'+
    '</tr></thead><tbody>'+
    list.map(d=>'<tr>'+
      '<td class="mono">'+d.id.slice(0,8)+'…'+d.id.slice(-4)+'</td>'+
      '<td class="hide-m"><span class="mono dim">'+(d.app||'-')+' '+(d.version||'')+'</span></td>'+
      '<td class="hide-m"><span class="mono dim">'+fmt(d.firstSeen)+'</span></td>'+
      '<td><span class="mono">'+ago(d.lastSeen)+'</span></td>'+
      '<td>'+(d.banned
        ? '<span class="badge ban"><span class="b-dot"></span>已封禁</span>'
        : '<span class="badge ok"><span class="b-dot"></span>正常</span>')+'</td>'+
      '<td style="text-align:right"><div class="row-flex" style="justify-content:flex-end">'+
        (d.banned
          ? '<button class="btn sm" data-act="unban" data-id="'+d.id+'">解禁</button>'
          : '<button class="btn ghost sm" data-act="ban" data-id="'+d.id+'">封禁</button>')+
        '<button class="btn ghost sm" data-act="delete" data-id="'+d.id+'">移除</button>'+
      '</div></td>'+
    '</tr>').join('')+
    '</tbody></table>';
}
/* 事件委托：表格按钮用 data 属性，避免内联 onclick 的引号转义
   （本文件会被 Worker 模板字符串内嵌，反斜杠转义会被提前吃掉） */
document.getElementById('tableWrap').addEventListener('click', function(e){
  const btn = e.target.closest('button[data-act]');
  if(btn) doAct(btn.dataset.act, btn.dataset.id);
});
async function doAct(action,id){
  await API.act(action,id);
  toast({ban:'已封禁该设备',unban:'已解除封禁',delete:'已移除登记'}[action]||'完成');
  await loadDevices();
}

/* ---------- 外观设置（localStorage 持久化） ---------- */
function setTheme(t){
  document.documentElement.dataset.theme = t;
  localStorage.setItem('oc_theme',t);
  document.getElementById('thDark').classList.toggle('on',t==='dark');
  document.getElementById('thLight').classList.toggle('on',t==='light');
}
function setBg(url){
  document.getElementById('bgLayer').style.backgroundImage = url ? 'url("'+url+'")' : 'none';
  localStorage.setItem('oc_bg',url);
}
function setOpacity(v){
  document.documentElement.style.setProperty('--fg-opacity', v/100);
  document.getElementById('opVal').textContent = v+'%';
  document.getElementById('opRange').value = v;
  localStorage.setItem('oc_opacity',v);
}
function resetPrefs(){
  localStorage.removeItem('oc_theme');localStorage.removeItem('oc_bg');localStorage.removeItem('oc_opacity');
  setTheme('dark'); setBg(''); setOpacity(72);
  document.getElementById('bgUrl').value = '';
  toast('已恢复默认外观');
}
(function initPrefs(){
  setTheme(localStorage.getItem('oc_theme')||'dark');
  const bg = localStorage.getItem('oc_bg')||'';
  if(bg){ setBg(bg); document.getElementById('bgUrl').value = bg; }
  setOpacity(localStorage.getItem('oc_opacity')||72);
})();

/* ---------- toast ---------- */
let toastTimer;
function toast(msg){
  const t = document.getElementById('toast');
  t.textContent = msg; t.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(()=>t.classList.remove('show'), 2200);
}
</script>
</body>
</html>
`;

/* ---------- 管理 Cookie（HMAC 签名，密钥即 ADMIN_PASSWORD 本身） ---------- */
async function signToken(secret) {
  const key = await crypto.subtle.importKey(
    "raw", new TextEncoder().encode(secret),
    { name: "HMAC", hash: "SHA-256" }, false, ["sign"],
  );
  const sig = await crypto.subtle.sign("HMAC", key, new TextEncoder().encode("oc-admin-v1"));
  return Array.from(new Uint8Array(sig)).map(b => b.toString(16).padStart(2, "0")).join("");
}

async function isAuthed(request, env) {
  if (!env.ADMIN_PASSWORD) return false;
  const cookie = request.headers.get("cookie") || "";
  const m = cookie.match(new RegExp(COOKIE_NAME + "=([0-9a-f]{64})"));
  if (!m) return false;
  return m[1] === await signToken(env.ADMIN_PASSWORD);
}

function json(data, status = 200, extraHeaders = {}) {
  return new Response(JSON.stringify(data), {
    status,
    headers: { "content-type": "application/json; charset=utf-8", ...extraHeaders },
  });
}

/* ---------- 设备 API（App 端调用，匿名） ---------- */
async function handleCheckin(request, env) {
  let body;
  try { body = await request.json(); } catch { return json({ ok: false }, 400); }
  const id = String(body.id || "").trim();
  if (!/^[0-9a-fA-F]{8,64}$/.test(id)) return json({ ok: false }, 400);

  const key = KV_PREFIX + id.toLowerCase();
  const now = Date.now();
  const existing = await env.DEVICES.get(key, "json");
  const record = {
    id: id.toLowerCase(),
    firstSeen: existing?.firstSeen ?? now,
    lastSeen: now,
    app: String(body.app || existing?.app || "unknown"),
    version: String(body.version || existing?.version || ""),
    banned: existing?.banned ?? false,
  };
  await env.DEVICES.put(key, JSON.stringify(record));
  return json({ ok: true });
}

async function handleStatus(url, env) {
  const id = String(url.searchParams.get("id") || "").toLowerCase();
  if (!id) return json({ banned: false });
  const record = await env.DEVICES.get(KV_PREFIX + id, "json");
  return json({ banned: record?.banned === true });
}

/* ---------- 管理 API（需 Cookie） ---------- */
async function handleAdminLogin(request, env) {
  if (!env.ADMIN_PASSWORD) return json({ ok: false, error: "admin_not_configured" }, 503);
  let body;
  try { body = await request.json(); } catch { return json({ ok: false }, 400); }
  if (String(body.password || "") !== env.ADMIN_PASSWORD) {
    return json({ ok: false }, 401);
  }
  const token = await signToken(env.ADMIN_PASSWORD);
  return json({ ok: true }, 200, {
    "set-cookie": `${COOKIE_NAME}=${token}; HttpOnly; Secure; SameSite=Strict; Path=/; Max-Age=604800`,
  });
}

async function handleAdminDevices(env) {
  const out = [];
  let cursor;
  do {
    const page = await env.DEVICES.list({ prefix: KV_PREFIX, cursor });
    for (const k of page.keys) {
      const v = await env.DEVICES.get(k.name, "json");
      if (v) out.push(v);
    }
    cursor = page.list_complete ? undefined : page.cursor;
  } while (cursor);
  out.sort((a, b) => b.lastSeen - a.lastSeen);
  return json({ devices: out });
}

async function handleAdminAction(action, request, env) {
  let body;
  try { body = await request.json(); } catch { return json({ ok: false }, 400); }
  const id = String(body.id || "").toLowerCase();
  const key = KV_PREFIX + id;
  const record = await env.DEVICES.get(key, "json");
  if (!record) return json({ ok: false, error: "not_found" }, 404);

  if (action === "delete") {
    await env.DEVICES.delete(key);
  } else {
    record.banned = action === "ban";
    await env.DEVICES.put(key, JSON.stringify(record));
  }
  return json({ ok: true });
}

/* ---------- 路由 ---------- */
export default {
  async fetch(request, env) {
    const url = new URL(request.url);
    const path = url.pathname;
    const method = request.method;

    // OAuth 回调中转（原有功能）
    if (path === CALLBACK_PATH) {
      const hasCode = url.searchParams.has("code");
      const hasState = url.searchParams.has("state");
      const hasError = url.searchParams.has("error");
      if (!hasError && (!hasCode || !hasState)) {
        return new Response("Missing OAuth callback parameters", {
          status: 400, headers: { "content-type": "text/plain; charset=utf-8" },
        });
      }
      const appCallback = new URL(APP_CALLBACK);
      url.searchParams.forEach((value, key) => appCallback.searchParams.set(key, value));
      return Response.redirect(appCallback.toString(), 302);
    }

    // 设备 API
    if (path === "/api/device/checkin" && method === "POST") return handleCheckin(request, env);
    if (path === "/api/device/status" && method === "GET") return handleStatus(url, env);

    // 管理面板页面
    if (path === "/admin" || path === "/admin/") {
      return new Response(ADMIN_HTML, {
        headers: { "content-type": "text/html; charset=utf-8" },
      });
    }

    // 管理 API
    if (path === "/admin/api/login" && method === "POST") return handleAdminLogin(request, env);
    if (path.startsWith("/admin/api/")) {
      if (!(await isAuthed(request, env))) return json({ ok: false, error: "unauthorized" }, 401);
      if (path === "/admin/api/devices" && method === "GET") return handleAdminDevices(env);
      for (const action of ["ban", "unban", "delete"]) {
        if (path === "/admin/api/" + action && method === "POST") {
          return handleAdminAction(action, request, env);
        }
      }
      return json({ ok: false, error: "not_found" }, 404);
    }

    return new Response("Orange Cloud OAuth callback worker", {
      status: path === "/" ? 200 : 404,
      headers: { "content-type": "text/plain; charset=utf-8" },
    });
  },
};

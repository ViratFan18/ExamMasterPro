const state = {
  token: localStorage.getItem("token"),
  role: localStorage.getItem("role"),
  username: localStorage.getItem("username"),
  page: "dashboard",
  users: [],
  workspaceUserId: "",
  userSearch: "",
  complaintSearch: "",
  selectedComplaintCollege: ""
};

function debugLog(message, detail) {
  console.log(`[examaster] ${message}`, detail ?? "");
}

function debugWarn(message, detail) {
  console.warn(`[examaster] ${message}`, detail ?? "");
}

const userNav = ["dashboard","buildings","halls","students","import export","invigilators","exams","allocation","analytics","hall visualizer","building visualizer","complaints","audit"];
const adminNav = ["dashboard","user management","user data management","user credentials","go to user account","complaints","audit","storage analytics"];
const navMeta = {
  dashboard: {icon: 'layout-dashboard', tooltip: 'Overview of your application.'},
  buildings: {icon: 'building-2', tooltip: 'Manage buildings and capacities.'},
  halls: {icon: 'clipboard-list', tooltip: 'Manage examination halls.'},
  students: {icon: 'user-check', tooltip: 'Manage student records.'},
  'import export': {icon: 'upload-cloud', tooltip: 'Manage Excel data imports and exports.'},
  invigilators: {icon: 'user', tooltip: 'Manage faculty.'},
  exams: {icon: 'book-open', tooltip: 'Manage exam sessions.'},
  allocation: {icon: 'layout-grid', tooltip: 'Automatically generate seating.'},
  analytics: {icon: 'bar-chart-3', tooltip: 'View reports and statistics.'},
  'hall visualizer': {icon: 'grid', tooltip: 'Interactive seating preview.'},
  'building visualizer': {icon: 'map-pin', tooltip: 'Interactive building layout.'},
  complaints: {icon: 'message-circle', tooltip: 'Track complaints.'},
  audit: {icon: 'clock-3', tooltip: 'View system logs.'},
  'user management': {icon: 'users', tooltip: 'Manage user accounts.'},
  'user data management': {icon: 'database', tooltip: 'Clear user data and history.'},
  'user credentials': {icon: 'key', tooltip: 'Manage saved credentials.'},
  'go to user account': {icon: 'log-in', tooltip: 'Open a selected user workspace.'},
  'storage analytics': {icon: 'server', tooltip: 'View storage summary for all users.'}
};

  document.querySelectorAll("[data-auth]").forEach(btn => btn.addEventListener("click", () => {
  document.querySelectorAll("[data-auth]").forEach(b => b.classList.remove("active"));
  btn.classList.add("active");
  const tab = btn.dataset.auth;
  document.getElementById("loginForm").classList.toggle("d-none", tab !== "login");
  document.getElementById("registerForm").classList.toggle("d-none", tab !== "register");
}));

document.getElementById("loginForm").addEventListener("submit", e => submitAuth(e, "/api/auth/login"));
document.getElementById("registerForm").addEventListener("submit", e => submitAuth(e, "/api/auth/register"));
document.getElementById("logoutBtn").addEventListener("click", () => {
  const savedCredentials = localStorage.getItem("userCredentials");
  const savedVisits = localStorage.getItem("complaintVisits");
  const savedAdmin = localStorage.getItem("adminSession");
  localStorage.clear();
  if (savedCredentials) localStorage.setItem("userCredentials", savedCredentials);
  if (savedVisits) localStorage.setItem("complaintVisits", savedVisits);
  if (savedAdmin) localStorage.setItem("adminSession", savedAdmin);
  state.token = null;
  start();
});
document.getElementById("workspaceSelect").addEventListener("change", e => {
  state.workspaceUserId = e.target.value;
  render();
});

async function submitAuth(event, url) {
  event.preventDefault();
  const data = Object.fromEntries(new FormData(event.target).entries());
  debugLog("submitAuth start", {url, data});
  const res = await api(url, {method:"POST", body: JSON.stringify(data)}, false);
  debugLog("submitAuth response", {url, response: res});
  if (!res.success) {
    debugWarn("submitAuth failed", {url, response: res, payload: data});
    return;
  }
  if (res.success) {
    localStorage.setItem("token", res.data.token);
    localStorage.setItem("role", res.data.role);
    localStorage.setItem("username", res.data.username);

    // If a normal user logs in directly, clear any previous admin impersonation state.
    if (res.data.role !== "ROLE_SUPER_ADMIN") {
      localStorage.removeItem("adminSessionActive");
      if (!localStorage.getItem("adminSessionActive")) {
        localStorage.removeItem("adminSession");
      }
    }

      state.token = res.data.token;
    state.role = res.data.role;
    state.username = res.data.username;
    state.page = "dashboard";
    if (url.includes("/login")) {
      storeCredential({username: data.username, password: data.password, collegeName: data.collegeName || ""});
    }
    if (url.includes("/register")) {
      storeCredential({username: data.username, password: data.password, collegeName: data.collegeName});
    }
    toast(res.message);
    await start();
  }
}

// Student portal moved into Import & Export center for per-user workspace lookups.

async function api(path, options = {}, auth = true) {
  const headers = {"Content-Type":"application/json", ...(options.headers || {})};
  if (auth && state.token) headers.Authorization = `Bearer ${state.token}`;
  const {silent, bodyIsForm, ...fetchOptions} = options;
  if (bodyIsForm) delete headers["Content-Type"];
  debugLog("API request", {path, options: fetchOptions, auth, headers});
  try {
    const response = await fetch(path, {...fetchOptions, headers});
    const type = response.headers.get("content-type") || "";
    debugLog("API response", {path, status: response.status, type});
    if (type.includes("text/csv")) return response.text();
    const body = await response.json().catch(() => ({success:false, message:"Unable to read server response."}));
    if (!body.success && !silent) {
      debugWarn("API failure", {path, status: response.status, body});
      toast(body.message || "Action could not be completed.");
    }
    return body;
  } catch (error) {
    debugWarn("API network error", {path, error});
    return {success:false, message:"Unable to reach the server."};
  }
}

function toast(message) {
  debugLog("toast", message);
  const el = document.getElementById("toast");
  el.querySelector(".toast-body").textContent = message;
  bootstrap.Toast.getOrCreateInstance(el).show();
}

async function start() {
  debugLog("start", {token: !!state.token, role: state.role, username: state.username, page: state.page});
  document.getElementById("authView").classList.toggle("d-none", !!state.token);
  document.getElementById("appView").classList.toggle("d-none", !state.token);
  if (!state.token) {
    debugLog("start: no token found, showing auth view");
    return;
  }
  // Initialize Bootstrap tooltips for any elements with data-bs-toggle="tooltip"
  try {
    const tooltipTriggerList = [].slice.call(document.querySelectorAll('[data-bs-toggle="tooltip"]'));
    tooltipTriggerList.map(function (el) { return new bootstrap.Tooltip(el); });
  } catch (e) {
    // ignore if bootstrap not available
  }
  syncAdminReturnButton();
  buildNav();
  if (window.lucide) lucide.replace({width: 18, height: 18, strokeWidth: 1.7});
  if (state.role === "ROLE_SUPER_ADMIN") await loadUsers();
  try {
    await render();
  } catch (ex) {
    debugWarn("Render failed", {error: ex, state});
    content(`<div class="panel"><div class="panel-title"><h3>Unable to load the page</h3></div><p class="muted">There was a problem rendering your workspace. Please refresh the browser or try again.</p></div>`);
    toast("Unable to load workspace. Please refresh and try again.");
  }
}

function syncAdminReturnButton() {
  const actions = document.querySelector(".top-actions");
  let btn = document.getElementById("backToAdminBtn");
  const adminSession = localStorage.getItem("adminSession");
  const activeImpersonation = localStorage.getItem("adminSessionActive");
  if (!adminSession || !activeImpersonation || state.role === "ROLE_SUPER_ADMIN") {
    if (btn) btn.remove();
    return;
  }
  if (!btn) {
    btn = document.createElement("button");
    btn.id = "backToAdminBtn";
    btn.className = "btn btn-primary btn-sm";
    btn.type = "button";
    btn.textContent = "Back To Admin";
    actions.prepend(btn);
  }
  btn.onclick = () => restoreAdminSession();
}

function restoreAdminSession() {
  const adminSession = JSON.parse(localStorage.getItem("adminSession") || "null");
  if (!adminSession) return;
  localStorage.setItem("token", adminSession.token);
  localStorage.setItem("role", adminSession.role);
  localStorage.setItem("username", adminSession.username);
  localStorage.removeItem("adminSession");
  localStorage.removeItem("adminSessionActive");
  state.token = adminSession.token;
  state.role = adminSession.role;
  state.username = adminSession.username;
  state.workspaceUserId = "";
  state.page = "dashboard";
  toast("Returned to admin workspace.");
  start();
}

function buildNav() {
  const nav = document.getElementById("nav");
  const items = state.role === "ROLE_SUPER_ADMIN" ? adminNav : userNav;
  nav.innerHTML = items.map(item => {
    const meta = navMeta[item] || {};
    return `<button class="nav-button ${state.page===item?'active':''}" data-page="${item}" title="${escapeAttr(meta.tooltip || title(item))}">
      <span class="nav-icon" data-lucide="${meta.icon || 'circle'}"></span>
      <span>${title(item)}</span>
    </button>`;
  }).join("");
  if (window.lucide) lucide.replace({width: 18, height: 18, strokeWidth: 1.7});
  nav.querySelectorAll("button").forEach(btn => btn.addEventListener("click", () => {
    state.page = btn.dataset.page;
    buildNav();
    render();
  }));
}

async function loadUsers() {
  const res = await api("/api/admin/users");
  debugLog("loadUsers response", res);
  state.users = res.success ? res.data : [];
  const select = document.getElementById("workspaceSelect");
  select.innerHTML = `<option value="">Admin Workspace</option>` + state.users.filter(u => u.role === "ROLE_USER").map(u => `<option value="${u.id}">${u.collegeName}</option>`).join("");
  select.classList.remove("d-none");
}

function qs() {
  return state.workspaceUserId ? `?workspaceUserId=${state.workspaceUserId}` : "";
}

function apiUrl(path) {
  const query = qs();
  if (!query) return path;
  return path.includes("?") ? `${path}&${query.slice(1)}` : `${path}${query}`;
}

async function render() {
  document.getElementById("pageTitle").textContent = title(state.page);
  const selected = state.users.find(u => String(u.id) === String(state.workspaceUserId));
  document.getElementById("workspaceBanner").textContent = selected ? `Viewing User Workspace: ${selected.collegeName}` : `${state.username || ""} Workspace`;
  if (state.page === "dashboard") return dashboard();
  if (state.page === "buildings") return buildings();
  if (state.page === "halls") return halls();
  if (state.page === "students") return students();
  if (state.page === "import export") return importExport();
  if (state.page === "invigilators") return invigilators();
  if (state.page === "exams") return exams();
  if (state.page === "allocation") return allocation();
  if (state.page === "analytics") return analytics();
  if (state.page === "hall visualizer") return hallVisualizer();
  if (state.page === "building visualizer") return buildingVisualizer();
  if (state.page === "complaints") return complaints();
  if (state.page === "audit") return audit();
  if (state.page === "user management") return userManagement();
  if (state.page === "user data management") return userDataManagement();
  if (state.page === "user credentials") return userCredentials();
  if (state.page === "go to user account") return goToUser();
  if (state.page === "storage analytics") return storage();
}

async function dashboard() {
  if (state.role === "ROLE_SUPER_ADMIN" && !state.workspaceUserId) return adminDashboard();
  const res = await api(`/api/dashboard${qs()}`);
  if (!res.success || !res.data) {
    const errorMessage = res.message || "Unable to load dashboard.";
    content(`<div class="panel"><div class="panel-title"><h3>Dashboard unavailable</h3></div><p class="muted">${escapeAttr(errorMessage)}</p></div>`);
    return;
  }
  const d = res.data;
  const steps = Array.isArray(d.setupSteps) ? d.setupSteps : [];
  const completed = steps.filter(s => s.completed).length;
  const stepsHtml = steps.map(s => `
    <div class="setup-step neu-surface ${s.completed ? "done" : "pending"}">
      <div class="setup-step-head">
        <span class="step-num ${s.completed ? "done" : ""}">${s.completed ? "✓" : s.step}</span>
        <div class="flex-grow-1">
          <strong>Step ${s.step} — ${s.title}</strong>
          <span class="status-pill ${s.completed ? "ok" : "warn"} ms-2">${s.completed ? "Done" : "Pending"}</span>
          <p class="muted mb-0 mt-1">${s.description}</p>
          ${s.count > 0 ? `<small class="text-muted-custom">${s.count} record(s) added</small>` : ""}
        </div>
        <button class="btn btn-sm ${s.completed ? "btn-outline-light neu-btn-flat" : "btn-primary neu-btn"}" data-goto-step="${s.page}">${s.completed ? "Review" : "Start"}</button>
      </div>
    </div>`).join("");
  content(`<div class="panel glass-panel hero-panel">
    <div>
      <p class="eyebrow mb-1">Setup Progress</p>
      <h3>${completed} of 5 steps complete</h3>
      <p class="muted mb-0">Follow each step in order. When all 5 are done, go to Allocation to generate seats.</p>
    </div>
    <div class="readiness-ring">${Math.round((completed / 5) * 100)}%</div>
  </div>
  <div class="panel glass-panel">
    <div class="panel-title"><h3>5-Step Setup Guide</h3><span class="badge badge-soft">${completed}/5 complete</span></div>
    <div class="setup-steps-list">${stepsHtml}</div>
    ${completed === 5 ? `<div class="mt-3"><button class="btn btn-primary neu-btn" data-goto-step="allocation">Go to Seat Allocation →</button></div>` : ""}
  </div>
  <div class="metric-grid compact-metrics">
    ${metric("Buildings", d.buildings)}${metric("Halls", d.halls)}${metric("Students", d.students)}
    ${metric("Invigilators", d.invigilators)}${metric("Exams", d.exams)}${metric("Allocations", d.allocations)}
  </div>`);
}

async function adminDashboard() {
  const [storageRes, complaints] = await Promise.all([api("/api/admin/storage"), loadAdminComplaints()]);
  const colleges = storageRes.success ? storageRes.data : [];
  const totals = colleges.reduce((sum, college) => ({
    buildings: sum.buildings + college.buildings,
    halls: sum.halls + college.halls,
    students: sum.students + college.students,
    invigilators: sum.invigilators + college.invigilators,
    exams: sum.exams + college.exams,
    allocations: sum.allocations + college.allocations
  }), {buildings:0,halls:0,students:0,invigilators:0,exams:0,allocations:0});
  const visited = complaints.filter(c => isComplaintVisited(c.id)).length;
  const open = complaints.filter(c => c.status !== "RESOLVED" && c.status !== "CLOSED").length;
  const resolved = complaints.filter(c => c.status === "RESOLVED" || c.status === "CLOSED").length;
  content(`<div class="panel hero-panel dashboard-hero">
    <div>
      <p class="eyebrow mb-1">Superadmin Command Center</p>
      <h3>${colleges.length} colleges monitored</h3>
      <p class="muted mb-0">Track storage, complaints, visits, and account health from one clean overview.</p>
    </div>
    <div class="hero-score">${open}<span>Active Complaints</span></div>
  </div>
  <div class="metric-grid admin-metrics">
    ${metric("Buildings", totals.buildings)}${metric("Halls", totals.halls)}${metric("Students", totals.students)}
    ${metric("Complaints", complaints.length)}${metric("Visited", visited)}${metric("Not Visited", Math.max(0, complaints.length - visited))}
  </div>
  <div class="panel">
    <div class="panel-title"><h3>Performance Snapshot</h3><span class="badge badge-soft">${resolved} resolved</span></div>
    <div class="metric-grid compact-metrics">${metric("Invigilators", totals.invigilators)}${metric("Exams", totals.exams)}${metric("Allocations", totals.allocations)}</div>
  </div>
  ${collegeSummaryCards(colleges, false)}`);
}

async function buildings() {
  const res = await api(`/api/buildings${qs()}`);
  content(`${dataHint("buildings", 1)}
  ${form("buildingForm", [["buildingName","Building Name"],["maxHallCount","Maximum Halls","number"]], "Add Building")}
  <div class="panel glass-panel">${table(["Name","Hall Limit","Current Halls","Available Slots","Occupancy",""], res.data, b => [b.buildingName,b.maxHallCount,b.currentHalls,b.availableHallSlots,`${b.occupancyPercent}%`, delBtn("building", b.id)])}</div>`);
  bindForm("buildingForm", `/api/buildings${qs()}`);
  // client-side hints: building name length
  (function(){
    const bName = document.querySelector('#buildingForm [name="buildingName"]');
    if (bName) {
      bName.setAttribute('minlength','2');
      bName.setAttribute('maxlength','100');
      bName.setAttribute('title','Building name must be 2-100 characters');
    }
    const maxHall = document.querySelector('#buildingForm [name="maxHallCount"]');
    if (maxHall) {
      maxHall.setAttribute('min','1');
      maxHall.setAttribute('title','Maximum halls must be a positive number (max 100)');
      maxHall.setAttribute('max','100');
    }
  })();
  applyFriendlyValidation('#buildingForm');
  bindDelete("building", "/api/buildings");
}

async function halls() {
  const [b, h, dash] = await Promise.all([api(`/api/buildings${qs()}`), api(`/api/halls${qs()}`), api(`/api/dashboard${qs()}`)]);
  const buildingOptions = b.data.map(x => `<option value="${x.id}">${x.buildingName}</option>`).join("");
  const hasAllocations = dash.data?.allocations > 0;
  const deleteAllBtn = h.data.length > 0 ? `<button class="btn btn-sm btn-outline-danger" id="deleteAllHalls" style="margin-top: 12px;" ${hasAllocations ? 'disabled title="Delete allocations first"' : ''}>${hasAllocations ? '❌ Delete allocations first' : 'Delete All Halls'}</button>` : "";
  content(`${dataHint("halls", 2)}
  <form id="hallForm" class="panel glass-panel inline-form hall-form">
    <select class="form-select neu-input" name="buildingId" required>${buildingOptions}</select>
    <input class="form-control neu-input" name="hallName" placeholder="Hall Name" required>
    <input class="form-control neu-input" name="benchCount" type="number" min="1" placeholder="Number of Benches" required>
    <select class="form-select neu-input" name="studentsPerBench" id="studentsPerBench" required>
      <option value="1">1 Student / Bench</option>
      <option value="2">2 Students / Bench</option>
    </select>
    <div class="capacity-preview neu-inset" id="capacityPreview">Capacity: —</div>
    <button class="btn btn-primary neu-btn">Add Hall</button>
  </form>
  ${deleteAllBtn}
  <div class="panel glass-panel">${table(["Hall","Building","Benches","Per Bench","Capacity","Allocated","Occupancy",""], h.data, x => [x.hallName,x.buildingName,x.benchCount,x.studentsPerBench,x.capacity,x.allocatedStudents,`${x.occupancyPercent}%`,delBtn("hall", x.id)])}</div>`);
  const updateCapacity = () => {
    const benches = Number(document.querySelector('[name="benchCount"]')?.value || 0);
    const perBench = Number(document.querySelector('[name="studentsPerBench"]')?.value || 1);
    const el = document.getElementById("capacityPreview");
    if (el) el.textContent = benches > 0 ? `Capacity: ${benches * perBench} seats` : "Capacity: —";
  };
  document.querySelector('[name="benchCount"]')?.addEventListener("input", updateCapacity);
  document.getElementById("studentsPerBench")?.addEventListener("change", updateCapacity);
  // client-side hints for hall form
  (function(){
    const hallNameEl = document.querySelector('#hallForm [name="hallName"]');
    if (hallNameEl) {
      hallNameEl.setAttribute('pattern','^[A-Za-z0-9._-]{1,50}$');
      hallNameEl.setAttribute('maxlength','50');
      hallNameEl.setAttribute('title','Hall name must be 1-50 characters using letters, numbers, dot, underscore, or hyphen; spaces are not allowed');
    }
    const benchEl = document.querySelector('#hallForm [name="benchCount"]');
    if (benchEl) {
      benchEl.setAttribute('min','1');
      benchEl.setAttribute('max','500');
      benchEl.setAttribute('title','Bench count must be a positive whole number (max 500)');
    }
    const perBenchEl = document.querySelector('#hallForm [name="studentsPerBench"]');
    if (perBenchEl) {
      perBenchEl.setAttribute('title','Students per bench: choose 1 or 2');
    }
  })();
  applyFriendlyValidation('#hallForm');

  bindForm("hallForm", `/api/halls${qs()}`, null, payload => ({
    buildingId: Number(payload.buildingId),
    hallName: payload.hallName,
    benchCount: Number(payload.benchCount),
    studentsPerBench: Number(payload.studentsPerBench)
  }));
  bindDelete("hall", "/api/halls");
  document.getElementById("deleteAllHalls")?.addEventListener("click", async () => {
    if (hasAllocations) { toast("Delete allocations first before deleting halls."); return; }
    if (!confirm("Delete ALL halls? This action cannot be undone.")) return;
    const res = await api(`/api/halls/delete-all${qs()}`, {method:"POST"});
    if (res.success) { toast(res.message); render(); }
  });
}

async function students() {
  const res = await api(`/api/students${qs()}`);
  const deleteAllBtn = res.data.length > 0 ? `<button class="btn btn-sm btn-outline-danger" id="deleteAllStudents" style="margin-top: 12px;">Delete All Students</button>` : "";
  content(`${dataHint("students", 3)}
  ${form("studentForm", [["hallTicketNumber","Hall Ticket Number"],["studentName","Student Name"],["branch","Branch"],["year","Year"],["semester","Semester"],["section","Section"]], "Add Student", "wide-form")}
  ${deleteAllBtn}
  <div class="panel glass-panel">${table(["Ticket","Name","Branch","Year","Semester","Section",""], res.data, s => [s.hallTicketNumber,s.studentName,s.branch,s.year,s.semester,s.section,delBtn("student", s.id)])}</div>`);
  // client-side hints for student form
  (function(){
    const ticket = document.querySelector('#studentForm [name="hallTicketNumber"]');
    if (ticket) {
      ticket.setAttribute('pattern','^[A-Z0-9]{3,20}$');
      ticket.setAttribute('maxlength','20');
      ticket.setAttribute('title','Hall ticket must be 3-20 uppercase letters or digits');
    }
    const sname = document.querySelector('#studentForm [name="studentName"]');
    if (sname) {
      sname.setAttribute('minlength','2');
      sname.setAttribute('maxlength','100');
      sname.setAttribute('title', "Student name must be 2-100 characters and can contain letters, spaces, . \"'\" -");
    }
    const branch = document.querySelector('#studentForm [name="branch"]');
    if (branch) {
      branch.setAttribute('pattern','^[A-Z0-9]{2,10}$');
      branch.setAttribute('maxlength','10');
      branch.setAttribute('title','Branch must be 2-10 uppercase letters or digits');
    }
    const year = document.querySelector('#studentForm [name="year"]');
    if (year) {
      year.setAttribute('pattern','^[1-4]$');
      year.setAttribute('title','Year must be 1, 2, 3, or 4');
    }
    const sem = document.querySelector('#studentForm [name="semester"]');
    if (sem) {
      sem.setAttribute('pattern','^[1-8]$');
      sem.setAttribute('title','Semester must be 1-8');
    }
    const section = document.querySelector('#studentForm [name="section"]');
    if (section) {
      section.setAttribute('pattern','^[A-Z]$');
      section.setAttribute('maxlength','1');
      section.setAttribute('title','Section must be a single uppercase letter (A-Z)');
    }
  })();
  applyFriendlyValidation('#studentForm');

  bindForm("studentForm", `/api/students${qs()}`);
  bindDelete("student", "/api/students");
  document.getElementById("deleteAllStudents")?.addEventListener("click", async () => {
    if (!confirm("Delete ALL students? This action cannot be undone. First delete allocations.")) return;
    const res = await api(`/api/students/delete-all${qs()}`, {method:"POST"});
    if (res.success) { toast(res.message); render(); }
  });
}

async function importExport() {
  const summary = await api(`/api/export/summary${qs()}`);
  const s = summary.success ? summary.data : {buildings:0,halls:0,students:0,invigilators:0,exams:0,whatsappSummary:""};
  content(`
  <div class="panel glass-panel hero-panel import-hero">
    <div>
      <p class="eyebrow mb-1">Data Management</p>
      <h3>Import & Export Center</h3>
      <p class="muted mb-0">Upload CSV files in order. We validate every row before saving — wrong files are rejected with clear fixes.</p>
    </div>
    <div class="import-stats">${s.buildings}<span>Buildings</span></div>
  </div>

  <div class="panel glass-panel">
    <div class="panel-title"><h3>Import Order</h3><span class="badge badge-soft">Follow steps 1 → 4</span></div>
    <div class="import-steps">
      <div class="import-step neu-surface"><span class="step-num">1</span><div><strong>Buildings</strong><p class="muted mb-0">buildingName, maxHallCount</p></div></div>
      <div class="import-step neu-surface"><span class="step-num">2</span><div><strong>Halls</strong><p class="muted mb-0">buildingName, hallName, benchCount, studentsPerBench (1 or 2)</p></div></div>
      <div class="import-step neu-surface"><span class="step-num">3</span><div><strong>Students</strong><p class="muted mb-0">hallTicketNumber, studentName, branch, year, semester, section</p></div></div>
      <div class="import-step neu-surface"><span class="step-num">4</span><div><strong>Invigilators</strong><p class="muted mb-0">invigilatorId, invigilatorName</p></div></div>
    </div>
  </div>

  <div class="import-export-grid">
    ${importCard("buildings", "Buildings", "Step 1 — Import buildings first", "buildings", "/api/import/buildings")}
    ${importCard("halls", "Halls", "Step 2 — Requires buildings in workspace", "halls", "/api/import/halls")}
    ${importCard("students", "Students", "Step 3 — Unique hall tickets only", "students", "/api/import/students")}
    ${importCard("invigilators", "Invigilators", "Step 4 — Unique invigilator IDs only", "invigilators", "/api/import/invigilators")}
  </div>

  <div class="panel glass-panel">
    <div class="panel-title"><h3>Student Lookup (Workspace)</h3><span class="badge badge-soft">Per-user lookup</span>
      <button type="button" class="btn btn-sm btn-outline-light neu-btn-flat ms-2" data-bs-toggle="tooltip" title="Uses the selected workspace (top-right) and chosen exam to find the student's allocated seat. Enter hall ticket and click Find Seat.">?</button>
    </div>
    <p class="muted">Quickly find a student's allocated seat by typing their hall ticket. This uses the selected workspace (upper-right).</p>
    <div class="inline-form">
      <select id="lookupExam" class="form-select neu-input" aria-label="Select Exam"><option value="">Loading exams…</option></select>
      <input id="lookupTicket" class="form-control neu-input" placeholder="Hall Ticket Number">
      <button id="lookupBtn" class="btn btn-primary neu-btn">Find Seat</button>
    </div>
    <div id="lookupResult" class="mt-3 d-none"></div>
  </div>

  <div class="panel glass-panel">
    <div class="panel-title"><h3>Overall Export</h3><span class="badge badge-soft">One report file</span></div>
    <p class="muted">Download a single CSV of all imported data and share it by WhatsApp from supported devices.</p>
    <div class="export-actions">
      <button class="btn btn-primary neu-btn" data-export="/api/export/overall.csv" data-filename="exammaster-overall-export.csv">Download Overall CSV</button>
      <button class="btn btn-outline-light neu-btn-flat" data-whatsapp="overall">Share Overall CSV on WhatsApp</button>
    </div>
    <div class="metric-grid compact-metrics mt-3">
      ${metric("Buildings", s.buildings)}${metric("Halls", s.halls)}${metric("Students", s.students)}${metric("Exams", s.exams)}
    </div>
  </div>

  <div id="importResultPanel"></div>`);

  document.querySelectorAll("[data-template]").forEach(btn => btn.onclick = () => showTemplatePreview(btn.dataset.template));
  document.querySelectorAll("[data-export]").forEach(btn => btn.onclick = () => downloadCsv(btn.dataset.export, btn.dataset.filename));
  document.querySelectorAll("[data-import]").forEach(btn => btn.onclick = () => document.getElementById(btn.dataset.import).click());
  document.querySelectorAll("[data-import-input]").forEach(input => input.onchange = e => handleImport(e.target));
  document.querySelectorAll("[data-whatsapp]").forEach(btn => btn.onclick = () => shareWhatsApp(btn.dataset.whatsapp, s));

  const loadBtn = document.getElementById("loadAllocExport");
  if (loadBtn) {
    loadBtn.onclick = () => loadAllocationExport(document.getElementById("allocExportExam").value);
    loadAllocationExport(document.getElementById("allocExportExam").value);
    document.getElementById("allocExportExam").onchange = () => loadAllocationExport(document.getElementById("allocExportExam").value);
  }

  // Populate per-user exam select for lookup
  const lookupExam = document.getElementById("lookupExam");
  const lookupBtn = document.getElementById("lookupBtn");
  const lookupTicket = document.getElementById("lookupTicket");
  const lookupResult = document.getElementById("lookupResult");
  if (lookupExam) {
    const exRes = await api(`/api/exams${qs()}`);
    lookupExam.innerHTML = exRes.success && exRes.data.length ? `<option value="">Select Exam</option>` + exRes.data.map(e => `<option value="${e.id}">${escapeAttr(e.examName)} — ${escapeAttr(e.semester)}</option>`).join("") : `<option value="">No exams available</option>`;
    lookupBtn.onclick = async () => {
      const examId = lookupExam.value;
      const ticket = lookupTicket.value.trim();
      lookupResult.classList.add("d-none");
      if (!examId || !ticket) { toast("Select exam and enter hall ticket."); return; }
      const res = await api(apiUrl(`/api/seat?examId=${examId}&hallTicket=${encodeURIComponent(ticket)}`), {}, false);
      const panel = lookupResult;
      panel.classList.remove("d-none");
      if (!res.success) {
        panel.innerHTML = `<p class="text-warning-custom mb-0">${res.message || "Seat not found in this workspace."}</p>`;
        return;
      }
      const s = res.data;
      panel.innerHTML = `
        <div class="portal-seat-card">
          <p class="eyebrow mb-1">Allocated Seat</p>
          <h3 class="mb-2">${s.seatNumber}</h3>
          <p class="mb-1"><strong>${s.studentName}</strong> · ${s.hallTicketNumber}</p>
          <p class="mb-1 muted">${s.branch} · Section ${s.section}</p>
          <p class="mb-1">🏢 ${s.buildingName} → 🚪 ${s.hallName}</p>
          <p class="mb-0 muted">Exam: ${s.examName}</p>
        </div>`;
    };
  }
}

function dataHint(section, step) {
  return `<div class="panel glass-panel data-hint neu-inset">
    <span class="step-num">${step}</span>
    <div><strong>Step ${step} — ${title(section)}</strong>
    <p class="muted mb-0">Bulk upload? Go to <button type="button" class="link-btn" data-goto-import>Import & Export</button> for CSV templates, validation, and WhatsApp share.</p></div>
  </div>`;
}

function navigateToPage(page) {
  state.page = page;
  buildNav();
  render();
}

document.addEventListener("click", e => {
  const gotoStep = e.target.closest("[data-goto-step]");
  if (gotoStep) {
    e.preventDefault();
    navigateToPage(gotoStep.dataset.gotoStep);
    return;
  }
  if (e.target.matches("[data-goto-import]")) {
    e.preventDefault();
    navigateToPage("import export");
  }
});

function importCard(id, title, hint, templateId, importPath) {
  return `<article class="data-card import-card neu-surface">
    <div class="card-row"><h3>${title}</h3><span class="status-pill ok">CSV</span></div>
    <p class="muted">${hint}</p>
    <div class="import-card-actions">
      ${templateId ? `<button class="btn btn-sm btn-outline-light neu-btn-flat" data-template="${templateId}">Template</button>` : ""}
      <button class="btn btn-sm btn-primary neu-btn" data-import="${id}Import">Import</button>
    </div>
    <input type="file" id="${id}Import" class="d-none" accept=".csv" data-import-input data-path="${importPath}" data-label="${title}">
  </article>`;
}

const templateSchemas = {
  buildings: {
    title: "Buildings Template",
    headers: ["buildingName", "maxHallCount"],
    example: ["Science Block", "4"]
  },
  halls: {
    title: "Halls Template",
    headers: ["buildingName", "hallName", "benchCount", "studentsPerBench"],
    example: ["Science Block", "Hall A", "10", "2"]
  },
  students: {
    title: "Students Template",
    headers: ["hallTicketNumber", "studentName", "branch", "year", "semester", "section"],
    example: ["HT1234", "John Doe", "CSE", "3", "5", "A"]
  },
  invigilators: {
    title: "Invigilators Template",
    headers: ["invigilatorId", "invigilatorName"],
    example: ["INV001", "John Doe"]
  }
};

// Validation hints for each template field to help users prepare CSVs
templateSchemas.buildings.hints = [
  "buildingName: 2-100 characters",
  "maxHallCount: positive integer (e.g., 5)"
];
templateSchemas.halls.hints = [
  "buildingName: must match a Building already imported (case-insensitive)",
  "hallName: 1-50 characters using letters, numbers, dot, underscore, or hyphen; spaces are not allowed",
  "benchCount: positive whole number (max 500)",
  "studentsPerBench: 1 or 2"
];
templateSchemas.students.hints = [
  "hallTicketNumber: 3-20 characters, uppercase letters or digits",
  "studentName: 2-100 characters (letters, spaces, . ' -)",
  "branch: 2-10 uppercase letters or digits",
  "year: 1-4",
  "semester: 1-8",
  "section: single uppercase letter (A-Z)"
];
templateSchemas.invigilators.hints = [
  "invigilatorId: 2-20 characters, uppercase letters or digits",
  "invigilatorName: 2-100 characters"
];

function showTemplatePreview(templateId) {
  const schema = templateSchemas[templateId];
  if (!schema) return;
  const existing = document.getElementById("templatePreviewModal");
  if (existing) existing.remove();
  const modal = document.createElement("div");
  modal.id = "templatePreviewModal";
  modal.className = "modal-overlay";
  modal.innerHTML = `<div class="modal-panel">
    <div class="modal-header">
      <div>
        <h3>${schema.title}</h3>
        <p class="muted mb-0">Copy these headers into your CSV before importing.</p>
      </div>
      <button type="button" class="modal-close" aria-label="Close">×</button>
    </div>
    <div class="table-responsive modal-table">
      <table class="table align-middle mb-0">
        <thead><tr>${schema.headers.map(h => `<th>${escapeAttr(h)}</th>`).join("")}</tr></thead>
        <tbody><tr>${schema.example.map(v => `<td>${escapeAttr(v)}</td>`).join("")}</tr></tbody>
      </table>
    </div>
    ${schema.hints && schema.hints.length ? `<div class="modal-hints mt-3"><strong>Validation hints:</strong><ul class="muted mt-1">${schema.hints.map(h => `<li>${escapeAttr(h)}</li>`).join("")}</ul></div>` : ''}
    <div class="modal-actions">
      <button class="btn btn-primary neu-btn" type="button">Got it</button>
    </div>
  </div>`;
  document.body.appendChild(modal);
  modal.querySelector(".modal-close").onclick = () => modal.remove();
  modal.querySelector(".modal-actions button").onclick = () => modal.remove();
  modal.addEventListener("click", event => {
    if (event.target === modal) modal.remove();
  });
}

async function handleImport(input) {
  const file = input.files[0];
  if (!file) return;
  const panel = document.getElementById("importResultPanel");
  panel.innerHTML = `<div class="panel glass-panel"><p class="muted mb-0">Validating <strong>${escapeAttr(file.name)}</strong>…</p></div>`;

  const form = new FormData();
  form.append("file", file);
  const res = await api(`${input.dataset.path}${qs()}`, {method:"POST", body: form, bodyIsForm: true, silent: true});

  if (res.success && res.data?.accepted) {
    toast(res.message);
    panel.innerHTML = `<div class="panel glass-panel success-panel">
      <div class="panel-title"><h3>✓ ${input.dataset.label} Import Successful</h3><span class="status-pill ok">Accepted</span></div>
      <p class="mb-0">${res.data.importedCount} record(s) imported from ${res.data.totalRows} row(s).</p>
    </div>`;
    input.value = "";
    return;
  }

  const errors = res.data?.errors || [];
  const maxDisplay = 500;
  const displayErrors = errors.slice(0, maxDisplay);
  const headerErrors = displayErrors.filter(e => e.field === 'header');
  panel.innerHTML = `<div class="panel glass-panel error-panel">
    <div class="panel-title"><h3>Import Rejected</h3><span class="status-pill danger">Fix & Re-upload</span></div>
    <p class="text-warning-custom">${res.message || "Your file has validation errors. Nothing was saved."}</p>
    <p class="muted">Correct the issues below, then upload the same file again.</p>
    ${headerErrors.length ? `<div class="mt-2"><strong>Header issues:</strong><ul class="muted">${headerErrors.map(h => `<li>${escapeAttr(h.message)}</li>`).join("")}</ul></div>` : ''}
    ${displayErrors.length ? `<div class="table-responsive mt-3"><table class="table align-middle mb-0">
      <thead><tr><th>Row</th><th>Field</th><th>Value</th><th>Issue</th></tr></thead>
      <tbody>${displayErrors.map(e => `<tr><td>${e.row}</td><td>${e.field}</td><td>${escapeAttr(e.value)}</td><td>${e.message}</td></tr>`).join("")}</tbody>
    </table></div>` : `<p class="muted mb-0">No detailed row errors returned.</p>`}
    ${errors.length > maxDisplay ? `<p class="muted mt-2">Showing first ${maxDisplay} of ${errors.length} issues. Fix these first, then re-upload.</p>` : ''}
  </div>`;
  toast(res.message || "Import rejected. Please fix errors and re-upload.");
  input.value = "";
}

async function downloadCsv(path, filename) {
  try {
    const headers = {};
    if (state.token) headers.Authorization = `Bearer ${state.token}`;
    const response = await fetch(apiUrl(path), {headers});
    if (!response.ok) { toast("Download failed. Please log in again."); return; }
    const text = await response.text();
    const blob = new Blob([text], {type: "text/csv;charset=utf-8"});
    const url = URL.createObjectURL(blob);
    const a = document.createElement("a");
    a.href = url;
    a.download = filename || "export.csv";
    a.click();
    URL.revokeObjectURL(url);
    toast("Download started.");
  } catch {
    toast("Could not download file.");
  }
}

async function shareWhatsApp(kind, summary) {
  const s = summary || {};
  const text = `📄 *ExamMaster Pro Report*\nBuildings: ${s.buildings || 0}\nHalls: ${s.halls || 0}\nStudents: ${s.students || 0}\nExams: ${s.exams || 0}\n\nPlease find the attached report file.`;
  const filename = "exammaster-overall-export.csv";
  const path = "/api/export/overall.csv";
  await shareCsvOnWhatsApp(path, filename, text);
}

function allocationExportPath(examId, buildingId, hallId) {
  const params = [];
  if (buildingId) params.push(`buildingId=${buildingId}`);
  if (hallId) params.push(`hallId=${hallId}`);
  const base = `/api/allocation/${examId}/students.csv`;
  const scoped = params.length ? `${base}?${params.join("&")}` : base;
  return apiUrl(scoped);
}

function allocationExportFilename(examName, buildingName, hallName) {
  const slug = value => String(value || "export").trim().replace(/[^a-zA-Z0-9._-]+/g, "-").replace(/-+/g, "-");
  let name = slug(examName) + "-allocation";
  if (buildingName) name += "-" + slug(buildingName);
  if (hallName) name += "-" + slug(hallName);
  return name + ".csv";
}

function groupAllocationsByBuilding(rows) {
  const buildings = new Map();
  for (const row of rows) {
    if (!buildings.has(row.buildingId)) {
      buildings.set(row.buildingId, {id: row.buildingId, name: row.buildingName, halls: new Map(), rows: []});
    }
    const building = buildings.get(row.buildingId);
    building.rows.push(row);
    if (!building.halls.has(row.hallId)) {
      building.halls.set(row.hallId, {id: row.hallId, name: row.hallName, rows: []});
    }
    building.halls.get(row.hallId).rows.push(row);
  }
  return [...buildings.values()].sort((a, b) => a.name.localeCompare(b.name));
}

function allocationShareText(examName, buildingName, hallName, count) {
  return `📋 *${examName} — Allocation*\n`
    + (buildingName ? `🏢 Building: ${buildingName}\n` : "")
    + (hallName ? `🚪 Hall: ${hallName}\n` : "")
    + `👨‍🎓 Students: ${count}\n— ExamMaster Pro`;
}

function allocationExportActions(examId, examName, buildingId, buildingName, hallId, hallName, count) {
  const path = allocationExportPath(examId, buildingId, hallId);
  const filename = allocationExportFilename(examName, buildingName, hallName);
  return `<div class="export-actions">
    <button class="btn btn-sm btn-outline-light neu-btn-flat" data-alloc-export="${escapeAttr(path)}" data-alloc-filename="${escapeAttr(filename)}">Download CSV</button>
    <button class="btn btn-sm btn-outline-light neu-btn-flat" data-alloc-share="${escapeAttr(path)}" data-alloc-filename="${escapeAttr(filename)}" data-exam-name="${escapeAttr(examName)}" data-building-name="${escapeAttr(buildingName || "")}" data-hall-name="${escapeAttr(hallName || "")}" data-student-count="${count}">WhatsApp</button>
  </div>`;
}

function renderAllocationExportBody(examId, rows) {
  if (!rows.length) {
    return `<p class="muted mb-0">No allocation found for this exam. Generate allocation first.</p>`;
  }
  const examName = rows[0].examName;
  const buildings = groupAllocationsByBuilding(rows);
  const hallTable = rowsList => table(["Student","Ticket","Branch","Section","Seat"], rowsList, a => [a.studentName, a.hallTicketNumber, a.branch, a.section, a.seatNumber]);
  const buildingTable = rowsList => table(["Student","Ticket","Branch","Section","Hall","Seat"], rowsList, a => [a.studentName, a.hallTicketNumber, a.branch, a.section, a.hallName, a.seatNumber]);

  return `<div class="alloc-export-exam">
    <h3 class="alloc-exam-heading">${escapeAttr(examName)}</h3>
    <p class="muted mb-0">${rows.length} students allocated across ${buildings.length} building(s)</p>
  </div>
  ${buildings.map(building => `
    <section class="alloc-export-group neu-surface">
      <div class="alloc-export-head">
        <div>
          <p class="eyebrow mb-1">Building</p>
          <h4 class="mb-0">${escapeAttr(building.name)}</h4>
        </div>
        ${allocationExportActions(examId, examName, building.id, building.name, "", "", building.rows.length)}
      </div>
      ${buildingTable(building.rows)}
      <div class="alloc-export-halls">
        ${[...building.halls.values()].sort((a, b) => a.name.localeCompare(b.name)).map(hall => `
          <div class="alloc-export-subgroup">
            <div class="alloc-export-head">
              <div>
                <p class="eyebrow mb-1">Hall</p>
                <h5 class="mb-0">${escapeAttr(hall.name)}</h5>
              </div>
              ${allocationExportActions(examId, examName, building.id, building.name, hall.id, hall.name, hall.rows.length)}
            </div>
            ${hallTable(hall.rows)}
          </div>`).join("")}
      </div>
    </section>`).join("")}`;
}

async function loadAllocationExport(examId) {
  const body = document.getElementById("allocExportBody");
  if (!body) return;
  body.innerHTML = `<p class="muted mb-0">Loading allocation…</p>`;
  const [status, report] = await Promise.all([
    api(`/api/allocation/${examId}/status${qs()}`, {silent: true}),
    api(`/api/allocation/${examId}/students${qs()}`, {silent: true})
  ]);
  if (!status.success || !status.data?.exists) {
    body.innerHTML = `<p class="muted mb-0">No allocation found for this exam. Generate allocation first.</p>`;
    return;
  }
  const rows = report.success ? report.data || [] : [];
  body.innerHTML = renderAllocationExportBody(examId, rows);
  body.querySelectorAll("[data-alloc-export]").forEach(btn => btn.onclick = () => downloadCsv(btn.dataset.allocExport, btn.dataset.allocFilename));
  body.querySelectorAll("[data-alloc-share]").forEach(btn => btn.onclick = () => shareCsvOnWhatsApp(
    btn.dataset.allocShare,
    btn.dataset.allocFilename,
    allocationShareText(btn.dataset.examName, btn.dataset.buildingName, btn.dataset.hallName, btn.dataset.studentCount)
  ));
}

async function shareCsvOnWhatsApp(path, filename, text) {
  try {
    const headers = {};
    if (state.token) headers.Authorization = `Bearer ${state.token}`;
    const response = await fetch(apiUrl(path), {headers});
    if (!response.ok) { toast("Could not prepare file for sharing."); return; }
    const csvText = await response.text();
    const file = new File([csvText], filename, {type: "text/csv"});

    if (navigator.canShare && navigator.canShare({files: [file]})) {
      await navigator.share({title: "ExamMaster Pro Export", text, files: [file]});
      toast("Shared successfully using native share.");
      return;
    }
  } catch (err) {
    if (err?.name === "AbortError") return;
  }

  const whatsappUrl = `whatsapp://send?text=${encodeURIComponent(text)}`;
  window.location.href = whatsappUrl;
  toast("Opening WhatsApp. If the app does not open, please install WhatsApp or use a device with it installed.");
  downloadCsv(path, filename);
}

async function invigilators() {
  const res = await api(`/api/invigilators${qs()}`);
  const deleteAllBtn = res.data.length > 0 ? `<button class="btn btn-sm btn-outline-danger" id="deleteAllInvigilators" style="margin-top: 12px;">Delete All Invigilators</button>` : "";
  content(`${form("invigilatorForm", [["invigilatorId","Invigilator ID"],["invigilatorName","Invigilator Name"]], "Add Invigilator")}
  ${deleteAllBtn}
  <div class="panel glass-panel">${table(["ID","Name",""], res.data, i => [i.invigilatorId,i.invigilatorName,delBtn("invigilator", i.id)])}</div>`);
  // client-side hints for invigilator form
  (function(){
    const idEl = document.querySelector('#invigilatorForm [name="invigilatorId"]');
    if (idEl) {
      idEl.setAttribute('pattern','^[A-Z0-9]{2,20}$');
      idEl.setAttribute('title','Invigilator ID must be 2-20 uppercase letters or digits');
      idEl.setAttribute('maxlength','20');
    }
    const nameEl = document.querySelector('#invigilatorForm [name="invigilatorName"]');
    if (nameEl) {
      nameEl.setAttribute('minlength','2');
      nameEl.setAttribute('maxlength','100');
      nameEl.setAttribute('title','Invigilator name must be 2-100 characters');
    }
  })();

  applyFriendlyValidation('#invigilatorForm');

  bindForm("invigilatorForm", `/api/invigilators${qs()}`);
  bindDelete("invigilator", "/api/invigilators");
  document.getElementById("deleteAllInvigilators")?.addEventListener("click", async () => {
    if (!confirm("Delete ALL invigilators? This action cannot be undone. First delete allocations.")) return;
    const res = await api(`/api/invigilators/delete-all${qs()}`, {method:"POST"});
    if (res.success) { toast(res.message); render(); }
  });
}

async function exams() {
  const res = await api(`/api/exams${qs()}`);
  const deleteAllBtn = res.data.length > 0 ? `<button class="btn btn-sm btn-outline-danger" id="deleteAllExams" style="margin-top: 12px;">Delete All Exams</button>` : "";
  content(`<form id="examForm" class="panel inline-form"><input class="form-control" name="examName" placeholder="Exam Name" required><input class="form-control" name="academicYear" placeholder="Academic Year" required><input class="form-control" name="semester" placeholder="Semester" required><select class="form-select" name="examType"><option>REGULAR</option><option>SUPPLY</option></select><button class="btn btn-primary">Add Exam</button></form>
  ${deleteAllBtn}
  <div class="panel glass-panel">${table(["Exam","Academic Year","Semester","Type",""], res.data, e => [e.examName,e.academicYear,e.semester,e.examType,delBtn("exam", e.id)])}</div>`);
  bindForm("examForm", `/api/exams${qs()}`);
  bindDelete("exam", "/api/exams");
  document.getElementById("deleteAllExams")?.addEventListener("click", async () => {
    if (!confirm("Delete ALL exams? This action cannot be undone. First delete allocations.")) return;
    const res = await api(`/api/exams/delete-all${qs()}`, {method:"POST"});
    if (res.success) { toast(res.message); render(); }
  });
}

async function allocation() {
  const ex = await api(`/api/exams${qs()}`);
  if (!ex.data?.length) {
    return content(`<div class="panel glass-panel"><p class="muted mb-0">Create an exam first (Step 5), then return here to allocate seats.</p>
      <button class="btn btn-primary neu-btn mt-3" data-goto-step="exams">Go to Exams</button></div>`);
  }
  const examId = ex.data[0].id;
  const status = await api(`/api/allocation/${examId}/status${qs()}`, {silent: true});
  content(`<div class="panel glass-panel allocation-panel">
    <div class="panel-title"><h3>Seat Allocation</h3><span class="badge badge-soft">Flexible bench engine with analytics</span></div>
    <div id="allocationWarning"></div>
    <div class="inline-form allocation-controls">
      <select id="examSelect" class="form-select neu-input">${ex.data.map(e => `<option value="${e.id}">${e.examName} - ${e.semester}</option>`)}</select>
      <select id="allocationMode" class="form-select neu-input">
        <option value="STRICT">Strict — branch-mixed benches</option>
        <option value="FREE">Free — 1 student per bench</option>
        <option value="FLEXIBLE">Flexible — smart auto-balancing</option>
      </select>
      <button id="dryBtn" class="btn btn-outline-light neu-btn-flat">Dry Run</button>
      <button id="deleteAllocBtn" class="btn btn-outline-danger neu-btn-flat d-none">Delete Allocation</button>
      <button id="genBtn" class="btn btn-primary neu-btn">Generate Final</button>
    </div>
    <p class="muted mt-2 mb-0"><small>⚠️ Delete any existing allocation before generating a new one. Export a backup from Import & Export first.</small></p>
    <div id="allocationResult" class="mt-3"></div>
  </div>`);
  const refreshStatus = async () => {
    const id = document.getElementById("examSelect").value;
    const st = await api(`/api/allocation/${id}/status${qs()}`, {silent: true});
    const warn = document.getElementById("allocationWarning");
    const delBtn = document.getElementById("deleteAllocBtn");
    const genBtn = document.getElementById("genBtn");
    if (st.success && st.data?.exists) {
      warn.innerHTML = `<div class="panel glass-panel warning-panel mb-3">
        <strong>Existing allocation found</strong> — ${st.data.allocatedCount} seats assigned for <em>${st.data.examName}</em>.
        Delete it before generating a new allocation.
      </div>`;
      delBtn.classList.remove("d-none");
      genBtn.disabled = true;
      genBtn.title = "Delete existing allocation first";
    } else {
      warn.innerHTML = "";
      delBtn.classList.add("d-none");
      genBtn.disabled = false;
      genBtn.title = "";
    }
  };

  document.getElementById("examSelect").onchange = refreshStatus;
  await refreshStatus();

  const modeParam = () => `${qs() ? "&" : "?"}mode=${document.getElementById("allocationMode").value}`;

  document.getElementById("dryBtn").onclick = async () => {
    const id = document.getElementById("examSelect").value;
    const res = await api(`/api/allocation/${id}/dry-run${qs()}${modeParam()}`);
    const modeLabel = res.data.mode === "FREE" ? "Free (1/bench)" 
                    : res.data.mode === "STRICT" ? "Strict (branch mix)"
                    : "Flexible (auto-balance)";
    
    // Branch Analytics
    const branchAnalytics = res.data.branchAnalytics && res.data.branchAnalytics.length > 0 
      ? `<div class="panel glass-panel mt-3">
          <h4 class="mb-3">📊 Branch Distribution Analytics</h4>
          <div class="table-responsive">
            <table class="table table-sm table-dark">
              <thead><tr><th>Branch</th><th>Students</th><th>%</th><th>Status</th><th>Recommendation</th></tr></thead>
              <tbody>${res.data.branchAnalytics.map(b => `<tr>
                <td><strong>${b.branch}</strong></td>
                <td>${b.studentCount}</td>
                <td>${b.percentage.toFixed(1)}%</td>
                <td>${b.status}</td>
                <td><small>${b.recommendation}</small></td>
              </tr>`).join("")}</tbody>
            </table>
          </div>
        </div>` : "";
    
    // Section Distribution
    const sectionDist = res.data.sectionDistribution && res.data.sectionDistribution.length > 0
      ? `<div class="panel glass-panel mt-3">
          <h4 class="mb-3">📋 Section Distribution</h4>
          <div class="table-responsive">
            <table class="table table-sm table-dark">
              <thead><tr><th>Section</th><th>Students</th><th>Branches</th><th>Strategy</th></tr></thead>
              <tbody>${res.data.sectionDistribution.map(s => `<tr>
                <td><strong>${s.section}</strong></td>
                <td>${s.studentCount}</td>
                <td><small>${s.branches}</small></td>
                <td><small>${s.distributionStrategy}</small></td>
              </tr>`).join("")}</tbody>
            </table>
          </div>
        </div>` : "";
    
    document.getElementById("allocationResult").innerHTML = `
      <div class="metric-grid">${metric("Students",res.data.students)}${metric("Capacity",res.data.totalCapacity)}${metric("Remaining",res.data.remainingCapacity)}${metric("Mode",modeLabel)}${metric("Ready",res.data.ready ? "✓ Yes" : "✗ No")}</div>
      <div class="panel glass-panel neu-inset mt-3">${res.data.messages.map(m => `<p class="mb-1">${m}</p>`).join("")}</div>
      ${branchAnalytics}
      ${sectionDist}`;
  };

  document.getElementById("deleteAllocBtn").onclick = async () => {
    const id = document.getElementById("examSelect").value;
    if (!confirm("Delete all seat assignments for this exam? Export CSV from Import & Export first if you need a backup.")) return;
    const res = await api(`/api/allocation/${id}${qs()}`, {method:"DELETE"});
    if (res.success) { toast(res.message); document.getElementById("allocationResult").innerHTML = ""; await refreshStatus(); }
  };

  document.getElementById("genBtn").onclick = async () => {
    const id = document.getElementById("examSelect").value;
    const res = await api(`/api/allocation/${id}/generate${qs()}${qs() ? "&" : "?"}replaceExisting=false${modeParam().replace("?", "&")}`, {method:"POST"});
    if (res.success) {
      const warnings = res.data.warnings?.length
        ? `<div class="panel glass-panel warning-panel mt-3"><h4 class="mb-2">Warnings</h4>${res.data.warnings.map(w => `<p class="mb-1 text-warning-custom">${w}</p>`).join("")}</div>` : "";
      const unplaced = res.data.unplacedStudents > 0 ? `<p class="text-warning-custom mt-2">${res.data.unplacedStudents} students remain unplaced.</p>` : "";
      document.getElementById("allocationResult").innerHTML = warnings + unplaced + table(["Student","Ticket","Building","Hall","Seat"], res.data.allocations, a => [a.studentName,a.hallTicketNumber,a.buildingName,a.hallName,a.seatNumber]);
      toast("Allocation generated successfully.");
      await refreshStatus();
    }
  };
}

async function analytics() {
  const [b, h] = await Promise.all([api(`/api/analytics/buildings${qs()}`), api(`/api/halls${qs()}`)]);
  content(`<div class="panel glass-panel"><div class="panel-title"><h3>Building Occupancy</h3></div>${table(["Building","Hall Limit","Current Halls","Occupancy"], b.data, x => [x.buildingName,x.hallLimit,x.currentHalls,bar(x.occupancyPercent)])}</div>
  <div class="panel glass-panel"><div class="panel-title"><h3>Hall Occupancy</h3></div>${table(["Hall","Benches","Per Bench","Capacity","Allocated","Occupancy"], h.data, x => [x.hallName,x.benchCount,x.studentsPerBench,x.capacity,x.allocatedStudents,bar(x.occupancyPercent)])}</div>`);
}

async function hallVisualizer(hallId) {
  const hallsRes = await api(`/api/halls${qs()}`);
  const id = hallId || hallsRes.data[0]?.id;
  if (!id) return content(`<div class="panel glass-panel">Create a hall to open the visualizer.</div>`);
  const benches = await api(`/api/visualizer/halls/${id}${qs()}`);
  const benchGrid = benches.data.map(bench => `
    <div class="bench-card neu-surface">
      <div class="bench-label">${bench.benchLabel}</div>
      <div class="bench-seats">${bench.seats.map(s => `
        <div class="seat ${s.allocated?'full':'empty'} ${s.seatInBench===2?'seat-secondary':''}" title="${s.allocated ? `${s.studentName} | ${s.hallTicketNumber} | ${s.branch} | ${s.section}` : 'Empty seat'}">
          ${s.allocated?'✓':'×'}<small>${s.seatNumber}</small>
        </div>`).join("")}</div>
    </div>`).join("");
  content(`<div class="panel glass-panel">
    <select id="hallPick" class="form-select neu-input mb-3">${hallsRes.data.map(h => `<option value="${h.id}" ${String(h.id)===String(id)?"selected":""}>${h.hallName} - ${h.buildingName} (${h.benchCount} benches)</option>`)}</select>
    <div class="bench-grid">${benchGrid}</div>
  </div>`);
  document.getElementById("hallPick").onchange = e => hallVisualizer(e.target.value);
}

async function buildingVisualizer() {
  const h = await api(`/api/halls${qs()}`);
  if (!h.data || h.data.length === 0) return content(`<div class="panel glass-panel">Add building to use this visualizer.</div>`);
  content(`<div class="hall-cards">${h.data.map(x => `<div class="data-card" data-hall="${x.id}"><h3>${x.hallName}</h3><p class="muted">${x.buildingName}</p><p>${x.allocatedStudents}/${x.capacity} allocated</p>${bar(x.occupancyPercent)}</div>`).join("")}</div>`);
  document.querySelectorAll("[data-hall]").forEach(card => card.onclick = () => { state.page = "hall visualizer"; buildNav(); hallVisualizer(card.dataset.hall); });
}

async function complaints() {
  if (state.role === "ROLE_SUPER_ADMIN") return adminComplaints();
  const res = await api(`/api/complaints${qs()}`);
  content(`${form("complaintForm", [["title","Title"],["category","Category"],["email","Email","email"],["description","Description"]], "Create Complaint", "wide-form")}
  <div class="panel">${table(["Title","Category","Email","Status","Action"], res.data, c => [c.title,c.category,c.email,c.status, (c.status === "RESOLVED" || c.status === "CLOSED") ? delBtn("complaint", c.id) : ""])}</div>`);
  bindForm("complaintForm", `/api/complaints${qs()}`);
  bindDelete("complaint", "/api/complaints");
}

async function adminComplaints() {
  const complaints = await loadAdminComplaints();
  const filtered = filterBy(complaints, state.complaintSearch, c => `${c.collegeName} ${c.title} ${c.category} ${c.email} ${c.status}`);
  const grouped = complaintCollegeCards(filtered);
  const selected = filtered.filter(c => String(c.userId) === String(state.selectedComplaintCollege));
  content(`<div class="panel">
    <div class="panel-title"><h3>Complaint Colleges</h3><span class="badge badge-soft">${grouped.length} colleges</span></div>
    ${searchBox("complaintSearch", "Search college, complaint, email, or status", state.complaintSearch)}
    <div class="college-grid mt-3">${grouped.length ? grouped.map(complaintCollegeCard).join("") : `<p class="muted mb-0">No complaints found.</p>`}</div>
  </div>
  ${state.selectedComplaintCollege ? `<div class="panel">
    <div class="panel-title"><h3>${selected[0]?.collegeName || "Complaint"} Details</h3><span class="badge badge-soft">${selected.length} complaints</span></div>
    <div class="complaint-detail-list">${selected.map(complaintDetailCard).join("") || `<p class="muted mb-0">No complaints found.</p>`}</div>
  </div>` : ""}`);
  bindSearch("complaintSearch", value => { state.complaintSearch = value; adminComplaints(); });
  document.querySelectorAll("[data-complaint-college]").forEach(card => card.onclick = () => {
    state.selectedComplaintCollege = card.dataset.complaintCollege;
    filtered.filter(c => String(c.userId) === String(state.selectedComplaintCollege)).forEach(c => markComplaintVisited(c.id));
    adminComplaints();
  });
  document.querySelectorAll("[data-resolve]").forEach(btn => btn.onclick = async () => {
    const id = btn.dataset.resolve;
    markComplaintVisited(id);
    const res = await api(`/api/admin/complaints/${id}`, {method:"PATCH", body: JSON.stringify({status: "RESOLVED"})});
    if (res.success) toast("Complaint marked as resolved.");
    await adminComplaints();
  });
  document.querySelectorAll("[data-visit-complaint]").forEach(card => card.onclick = event => {
    if (event.target.closest("button,select,a")) return;
    markComplaintVisited(card.dataset.visitComplaint);
    card.classList.add("visited");
    card.querySelector("[data-visit-label]").textContent = "Visited";
  });
  document.querySelectorAll("[data-status]").forEach(select => select.onchange = async event => {
    const id = event.target.dataset.status;
    markComplaintVisited(id);
    await api(`/api/admin/complaints/${id}`, {method:"PATCH", body: JSON.stringify({status: event.target.value})});
    await adminComplaints();
  });
}

async function loadAdminComplaints() {
  const direct = await api("/api/admin/complaints", {silent: true});
  const directRows = direct.success ? direct.data : [];
  if (directRows.length) return directRows;
  if (!state.users.length) await loadUsers();
  const userRows = state.users.filter(u => u.role === "ROLE_USER");
  const responses = await Promise.all(userRows.map(async user => {
    const res = await api(`/api/complaints?workspaceUserId=${user.id}`);
    return res.success ? res.data.map(c => ({
      ...c,
      userId: user.id,
      collegeName: user.collegeName
    })) : [];
  }));
  return responses.flat();
}

async function audit() {
  const res = await api(`/api/audit${qs()}`);
  content(`<div class="panel">${table(["Action","Module","Description","Performed By","Time"], res.data, a => [a.action,a.module,a.description,a.performedBy,new Date(a.performedAt).toLocaleString()])}</div>`);
}

async function userManagement() {
  await loadUsers();
  content(`${form("userForm", [["collegeName","College Name"],["username","Username"],["email","Email","email"],["password","Password","password"]], "Create User", "wide-form")}
  <div class="panel">
    <div class="panel-title"><h3>User Management</h3><span class="badge badge-soft">Enable, disable, or delete</span></div>
    ${searchBox("userSearch", "Search college, username, or email", state.userSearch)}
    <div class="college-grid mt-3">${collegeUserCards(state.users).join("") || `<p class="muted mb-0">No users found.</p>`}</div>
  </div>`);
  bindForm("userForm", "/api/admin/users", async (payload, created) => {
    storeCredential({...payload, id: created?.id});
    await loadUsers();
    await userManagement();
  });
  bindSearch("userSearch", value => { state.userSearch = value; userManagement(); });
  document.querySelectorAll("[data-toggle]").forEach(btn => btn.onclick = async () => {
    await api(`/api/admin/users/${btn.dataset.toggle}/${btn.dataset.enabled === "true" ? "disable" : "enable"}`, {method:"POST"});
    await userManagement();
  });
  document.querySelectorAll("[data-delete-user]").forEach(btn => btn.onclick = async () => {
    if (!confirm("Delete this user account completely from the database? This cannot be undone.")) return;
    const res = await api(`/api/admin/users/${btn.dataset.deleteUser}`, {method:"DELETE"});
    if (res.success) {
      removeCredential(btn.dataset.deleteUser);
      toast("User account deleted completely.");
      await loadUsers();
      await userManagement();
    }
  });
}

async function userDataManagement() {
  await loadUsers();
  const storageRes = await api("/api/admin/storage");
  const storage = storageRes.success ? storageRes.data : [];
  content(`<div class="panel">
    <div class="panel-title"><h3>User Data Management</h3><span class="badge badge-soft">${state.users.filter(u => u.role === "ROLE_USER").length} users</span></div>
    ${searchBox("userSearch", "Search college, username, or email", state.userSearch)}
    <div class="college-grid mt-3">${state.users.filter(u => u.role === "ROLE_USER").map(u => userDataCard(u, storage.find(row => String(row.userId) === String(u.id)))).join("") || `<p class="muted mb-0">No users found.</p>`}</div>
  </div>`);
  bindSearch("userSearch", value => { state.userSearch = value; userDataManagement(); });
  document.querySelectorAll("[data-clear]").forEach(btn => btn.onclick = async () => {
    const userId = btn.dataset.user;
    const type = btn.dataset.clear;
    const user = state.users.find(u => String(u.id) === String(userId));
    if (!user) return;
    if (!confirm(`Clear ${type} for ${user.collegeName}? This will affect their account data.`)) return;
    const res = await api(`/api/admin/users/${userId}/clear/${type}`, {method: "POST"});
    if (res.success) {
      toast(`${title(type)} cleared for ${user.collegeName}.`);
      await userDataManagement();
    }
  });
}

function userDataCard(u, stats) {
  const buildings = stats?.buildings || 0;
  const halls = stats?.halls || 0;
  const students = stats?.students || 0;
  const invigilators = stats?.invigilators || 0;
  const exams = stats?.exams || 0;
  const allocations = stats?.allocations || 0;
  return `<article class="data-card college-card management-card">
    <div class="card-row"><h3>${u.collegeName}</h3><span class="status-pill ${u.enabled ? "ok" : "danger"}">${u.enabled ? "Enabled" : "Disabled"}</span></div>
    <p class="muted">${u.username} · ${u.email}</p>
    <div class="mini-metrics">
      <span><strong>${buildings}</strong> Buildings</span>
      <span><strong>${halls}</strong> Halls</span>
      <span><strong>${students}</strong> Students</span>
      <span><strong>${invigilators}</strong> Invigilators</span>
      <span><strong>${exams}</strong> Exams</span>
      <span><strong>${allocations}</strong> Allocations</span>
    </div>
    <div class="card-actions">
      <button class="btn btn-sm btn-outline-danger" data-user="${u.id}" data-clear="allocations">Clear allocations</button>
      <button class="btn btn-sm btn-outline-danger" data-user="${u.id}" data-clear="exams">Clear exams</button>
      <button class="btn btn-sm btn-outline-danger" data-user="${u.id}" data-clear="buildings">Clear buildings</button>
      <button class="btn btn-sm btn-outline-danger" data-user="${u.id}" data-clear="halls">Clear halls</button>
      <button class="btn btn-sm btn-outline-danger" data-user="${u.id}" data-clear="students">Clear students</button>
      <button class="btn btn-sm btn-outline-danger" data-user="${u.id}" data-clear="invigilators">Clear invigilators</button>
    </div>
  </article>`;
}

async function goToUser() {
  await loadUsers();
  const users = filterBy(state.users.filter(u => u.role === "ROLE_USER"), state.userSearch, u => `${u.collegeName} ${u.username} ${u.email}`);
  content(`<div class="panel">
    <div class="panel-title"><h3>Open User Account</h3><span class="badge badge-soft">${users.length} colleges</span></div>
    ${searchBox("userSearch", "Search college name fast", state.userSearch)}
    <div class="college-grid mt-3">${users.map(signInCollegeCard).join("") || `<p class="muted mb-0">No college accounts found.</p>`}</div>
  </div>
  <div id="userSignInPanel"></div>`);
  bindSearch("userSearch", value => { state.userSearch = value; goToUser(); });
  document.querySelectorAll("[data-signin-card]").forEach(card => card.onclick = () => renderUserSignIn(card.dataset.signinCard));
}

async function userCredentials() {
  await loadUsers();
  const users = filterBy(state.users.filter(u => u.role === "ROLE_USER"), state.userSearch, u => `${u.collegeName} ${u.username} ${u.email}`);
  content(`<div class="panel glass-panel">
    <div class="panel-title"><h3>User Credentials</h3><span class="badge badge-soft">${users.length} users</span></div>
    <p class="muted">Passwords are stored per user on this browser when you log in, reset, or save manually. Each college account is kept separate.</p>
    ${searchBox("userSearch", "Search college or username", state.userSearch)}
    <div class="credentials-grid mt-3">${users.map(credentialCard).join("") || `<p class="muted mb-0">No credentials found.</p>`}</div>
  </div>`);
  bindSearch("userSearch", value => { state.userSearch = value; userCredentials(); });
  document.querySelectorAll("[data-reset-password]").forEach(form => form.onsubmit = async e => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target).entries());
    const res = await api(`/api/admin/users/${e.target.dataset.resetPassword}/reset-password`, {method:"POST", body: JSON.stringify({password: data.password})});
    if (res.success) {
      storeCredential({...data, id: e.target.dataset.resetPassword});
      toast("Password reset and saved for this user.");
      await userCredentials();
    }
  });
  document.querySelectorAll("[data-save-credential]").forEach(form => form.onsubmit = async e => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target).entries());
    storeCredential({...data, id: e.target.dataset.saveCredential});
    toast(`Password saved for ${data.username}.`);
    await userCredentials();
  });
}

async function storage() {
  const res = await api("/api/admin/storage");
  const rows = res.success ? res.data : [];
  content(collegeSummaryCards(rows, false));
}

function metric(label, value) { return `<div class="metric"><strong>${value}</strong><span>${label}</span></div>`; }
function content(html) { document.getElementById("content").innerHTML = html; }
function title(text) { return text.replace(/\b\w/g, c => c.toUpperCase()); }
function searchBox(id, placeholder, value = "") {
  return `<input id="${id}" class="form-control search-input" placeholder="${placeholder}" value="${escapeAttr(value)}">`;
}
function bindSearch(id, onSearch) {
  const input = document.getElementById(id);
  if (!input) return;
  input.addEventListener("input", e => onSearch(e.target.value));
  input.focus();
  input.setSelectionRange(input.value.length, input.value.length);
}
function filterBy(rows, value, text) {
  const term = (value || "").trim().toLowerCase();
  if (!term) return rows;
  return rows.filter(row => text(row).toLowerCase().includes(term));
}
function collegeUserCards(users) {
  return filterBy(users.filter(u => u.role === "ROLE_USER"), state.userSearch, u => `${u.collegeName} ${u.username} ${u.email}`)
    .map(u => `<article class="data-card college-card management-card">
      <div class="card-row"><h3>${u.collegeName}</h3><span class="status-pill ${u.enabled ? "ok" : "danger"}">${u.enabled ? "Enabled" : "Disabled"}</span></div>
      <p class="muted">${u.username}</p>
      <p>${u.email}</p>
      <div class="card-actions">
        <button class="btn btn-sm btn-outline-light" data-toggle="${u.id}" data-enabled="${u.enabled}">${u.enabled ? "Disable" : "Enable"}</button>
        <button class="btn btn-sm btn-outline-danger" data-delete-user="${u.id}">Delete</button>
      </div>
    </article>`);
}
function signInCollegeCard(u) {
  const saved = findCredential(u);
  return `<article class="data-card college-card signin-card" data-signin-card="${u.id}">
    <div class="card-row"><h3>${u.collegeName}</h3><span class="status-pill ${u.enabled ? "ok" : "danger"}">${u.enabled ? "Active" : "Disabled"}</span></div>
    <p class="muted mb-1">${u.username}</p>
    <p class="mb-0">${saved?.password ? "Saved password ready" : "Enter password to continue"}</p>
  </article>`;
}
function renderUserSignIn(userId) {
  const user = state.users.find(u => String(u.id) === String(userId));
  const saved = user ? findCredential(user) : null;
  const panel = document.getElementById("userSignInPanel");
  if (!user || !panel) return;
  panel.innerHTML = `<div class="panel signin-panel">
    <div class="panel-title"><h3>Sign In To ${user.collegeName}</h3><span class="badge badge-soft">${user.username}</span></div>
    <form id="adminUserLoginForm" class="wide-form">
      <input class="form-control" name="username" value="${escapeAttr(user.username)}" required>
      <input class="form-control" name="password" type="password" placeholder="User Password" value="${escapeAttr(saved?.password || "")}" required>
      <button class="btn btn-primary">Login As User</button>
    </form>
  </div>`;
  document.getElementById("adminUserLoginForm").onsubmit = async e => {
    e.preventDefault();
    const data = Object.fromEntries(new FormData(e.target).entries());
    const res = await api("/api/auth/login", {method:"POST", body: JSON.stringify(data)}, false);
    if (!res.success) return;
    localStorage.setItem("adminSession", JSON.stringify({
      token: state.token,
      role: state.role,
      username: state.username
    }));
    localStorage.setItem("adminSessionActive", "true");
    localStorage.setItem("token", res.data.token);
    localStorage.setItem("role", res.data.role);
    localStorage.setItem("username", res.data.username);
    storeCredential({...data, id: user.id, collegeName: user.collegeName});
    state.token = res.data.token;
    state.role = res.data.role;
    state.username = res.data.username;
    state.workspaceUserId = "";
    state.page = "dashboard";
    toast(`Logged in to ${user.collegeName}.`);
    start();
  };
}
function credentialCard(u) {
  const saved = findCredential(u);
  const updated = saved?.updatedAt ? `<small class="muted">Saved ${new Date(saved.updatedAt).toLocaleString()}</small>` : "";
  return `<article class="data-card credential-card neu-surface">
    <div class="card-row"><h3>${u.collegeName}</h3><span class="status-pill ${saved?.password ? "ok" : "warn"}">${saved?.password ? "Saved" : "Not saved"}</span></div>
    <p><strong>Username</strong><br>${u.username}</p>
    <p><strong>Stored Password</strong><br><code class="cred-password">${saved?.password ? escapeAttr(saved.password) : "—"}</code></p>
    ${updated}
    <form class="credential-save" data-save-credential="${u.id}">
      <input name="username" value="${escapeAttr(u.username)}" hidden>
      <input name="collegeName" value="${escapeAttr(u.collegeName)}" hidden>
      <input class="form-control form-control-sm neu-input" name="password" type="text" placeholder="Type password to store" value="${escapeAttr(saved?.password || "")}" required minlength="6">
      <button class="btn btn-sm btn-outline-light neu-btn-flat">Save Password</button>
    </form>
    <form class="credential-reset" data-reset-password="${u.id}">
      <input name="username" value="${escapeAttr(u.username)}" hidden>
      <input name="collegeName" value="${escapeAttr(u.collegeName)}" hidden>
      <input class="form-control form-control-sm neu-input" name="password" type="text" placeholder="New password (reset on server)" required minlength="6">
      <button class="btn btn-sm btn-primary neu-btn">Reset & Save</button>
    </form>
  </article>`;
}
function collegeWorkspaceCards(rows) {
  return filterBy(rows, state.userSearch, row => row.collegeName)
    .map(row => `<article class="data-card college-card" data-college="${row.userId}">
      <div class="card-row"><h3>${row.collegeName}</h3><span class="status-pill">Open</span></div>
      <div class="mini-metrics">
        <span><strong>${row.buildings}</strong> Buildings</span>
        <span><strong>${row.halls}</strong> Halls</span>
        <span><strong>${row.students}</strong> Students</span>
      </div>
      <p class="muted mb-0">${row.invigilators} invigilators, ${row.exams} exams, ${row.allocations} allocations</p>
    </article>`);
}
function collegeSummaryCards(rows, clickable = true) {
  return `<div class="panel">
    <div class="panel-title"><h3>College Storage Summary</h3><span class="badge badge-soft">${rows.length} colleges</span></div>
    <div class="college-grid">${rows.length ? rows.map(row => `<article class="data-card college-card ${clickable ? "" : "static-card"}" ${clickable ? `data-college="${row.userId}"` : ""}>
      <h3>${row.collegeName}</h3>
      <div class="mini-metrics">
        <span><strong>${row.buildings}</strong> Buildings</span>
        <span><strong>${row.halls}</strong> Halls</span>
        <span><strong>${row.students}</strong> Students</span>
      </div>
      <p class="muted mb-0">${row.invigilators} invigilators, ${row.exams} exams, ${row.allocations} allocations</p>
    </article>`).join("") : `<p class="muted mb-0">No college storage found.</p>`}</div>
  </div>`;
}
function bindCollegeCards() {
  document.querySelectorAll("[data-college], [data-open]").forEach(el => el.onclick = event => {
    if (event.target.closest("[data-toggle]")) return;
    const id = el.dataset.college || el.dataset.open;
    state.workspaceUserId = id;
    const select = document.getElementById("workspaceSelect");
    if (select) select.value = id;
    state.page = "dashboard";
    buildNav();
    render();
  });
}
function complaintCard(c) {
  const visited = isComplaintVisited(c.id);
  return `<article class="data-card complaint-card ${visited ? "visited" : ""}" data-complaint="${c.id}">
    <div class="card-row"><h3>${c.collegeName}</h3><span class="status-pill ${visited ? "ok" : "warn"}" data-visit-label>${visited ? "Visited" : "Not visited"}</span></div>
    <p class="complaint-title">${c.title}</p>
    <p class="muted">${c.category} | ${new Date(c.createdAt).toLocaleString()}</p>
    <p>${c.description}</p>
    <div class="complaint-meta">
      <a href="mailto:${escapeAttr(c.email)}">${c.email}</a>
      <select class="form-select form-select-sm" data-status="${c.id}">
        ${["OPEN","IN_PROGRESS","RESOLVED","CLOSED"].map(status => `<option ${c.status === status ? "selected" : ""}>${status}</option>`).join("")}
      </select>
    </div>
  </article>`;
}
function complaintCollegeCards(complaints) {
  const grouped = new Map();
  complaints.forEach(c => {
    if (!grouped.has(c.userId)) grouped.set(c.userId, {userId: c.userId, collegeName: c.collegeName, complaints: []});
    grouped.get(c.userId).complaints.push(c);
  });
  return [...grouped.values()];
}
function complaintCollegeCard(group) {
  const total = group.complaints.length;
  const visited = group.complaints.every(c => isComplaintVisited(c.id));
  const active = group.complaints.filter(c => c.status !== "RESOLVED" && c.status !== "CLOSED").length;
  const selected = String(state.selectedComplaintCollege) === String(group.userId);
  return `<article class="data-card college-card complaint-college-card ${visited ? "visited" : ""} ${selected ? "selected" : ""}" data-complaint-college="${group.userId}">
    <div class="card-row"><h3>${group.collegeName}</h3><span class="status-pill ${visited ? "ok" : "warn"}">${visited ? "Visited" : "Not visited"}</span></div>
    <div class="mini-metrics">
      <span><strong>${total}</strong> Complaints</span>
      <span><strong>${active}</strong> Active</span>
      <span><strong>${total - active}</strong> Solved</span>
    </div>
    <p class="muted mb-0">Click to open complaint details.</p>
  </article>`;
}
function complaintDetailCard(c) {
  const visited = isComplaintVisited(c.id);
  return `<article class="data-card complaint-card ${visited ? "visited" : ""}" data-visit-complaint="${c.id}">
    <div class="card-row"><h3>${c.title}</h3><span class="status-pill ${visited ? "ok" : "warn"}" data-visit-label>${visited ? "Visited" : "Not visited"}</span></div>
    <p class="muted">${c.category} | ${new Date(c.createdAt).toLocaleString()}</p>
    <p>${c.description}</p>
    <div class="complaint-meta">
      <a href="mailto:${escapeAttr(c.email)}">${c.email}</a>
      <select class="form-select form-select-sm" data-status="${c.id}">
        ${["OPEN","IN_PROGRESS","RESOLVED","CLOSED"].map(status => `<option ${c.status === status ? "selected" : ""}>${status}</option>`).join("")}
      </select>
      <button class="btn btn-sm btn-primary" data-resolve="${c.id}">Resolve</button>
    </div>
  </article>`;
}
function complaintVisits() {
  return JSON.parse(localStorage.getItem("complaintVisits") || "{}");
}
function isComplaintVisited(id) {
  return !!complaintVisits()[id];
}
function markComplaintVisited(id) {
  const visits = complaintVisits();
  visits[id] = true;
  localStorage.setItem("complaintVisits", JSON.stringify(visits));
}
function credentials() {
  return JSON.parse(localStorage.getItem("userCredentials") || "{}");
}
function findCredential(user) {
  const all = credentials();
  const byId = all[String(user.id)];
  if (byId) return byId;
  const byUser = all[`user:${(user.username || "").toLowerCase()}`];
  if (byUser) return byUser;
  return Object.values(all).find(item => item && item.username === user.username) || null;
}
function storeCredential(data) {
  if (!data.username || !data.password) return;
  const all = credentials();
  const id = String(data.id || data.username);
  const entry = {
    id,
    collegeName: data.collegeName || all[id]?.collegeName || "",
    username: data.username,
    password: data.password,
    updatedAt: new Date().toISOString()
  };
  all[id] = entry;
  all[`user:${data.username.toLowerCase()}`] = entry;
  localStorage.setItem("userCredentials", JSON.stringify(all));
}
function removeCredential(id) {
  const all = credentials();
  const entry = all[String(id)];
  if (entry?.username) delete all[`user:${entry.username.toLowerCase()}`];
  delete all[String(id)];
  localStorage.setItem("userCredentials", JSON.stringify(all));
}
function escapeAttr(value = "") {
  return String(value).replaceAll("&", "&amp;").replaceAll('"', "&quot;").replaceAll("<", "&lt;").replaceAll(">", "&gt;");
}
// Attach friendly custom validity using `title` as the message for inputs with patterns or titles
function applyFriendlyValidation(formSelector) {
  try {
    const els = document.querySelectorAll(`${formSelector} input[pattern], ${formSelector} input[title], ${formSelector} select[title]`);
    els.forEach(el => {
      el.addEventListener('invalid', e => {
        if (!e.target.validity.valid) e.target.setCustomValidity(e.target.title || 'Invalid value');
      });
      el.addEventListener('input', e => { if (e.target && e.target.setCustomValidity) e.target.setCustomValidity(''); });
    });
  } catch (e) {
    // ignore in older browsers
  }
}
function table(headers, rows = [], map) {
  if (!rows.length) return `<p class="muted mb-0">No records found.</p>`;
  return `<div class="table-responsive"><table class="table align-middle mb-0"><thead><tr>${headers.map(h => `<th>${h}</th>`).join("")}</tr></thead><tbody>${rows.map(row => `<tr>${map(row).map(v => `<td>${v ?? ""}</td>`).join("")}</tr>`).join("")}</tbody></table></div>`;
}
function form(id, fields, label, cls = "inline-form") {
  return `<form id="${id}" class="panel ${cls}">${fields.map(([name, placeholder, type = "text"]) => `<input class="form-control" name="${name}" type="${type}" placeholder="${placeholder}" required>`).join("")}<button class="btn btn-primary">${label}</button></form>`;
}
function bindForm(id, path, after, transform) {
  document.getElementById(id).addEventListener("submit", async e => {
    e.preventDefault();
    const payload = Object.fromEntries(new FormData(e.target).entries());
    const body = transform ? transform(payload) : payload;
    const res = await api(path, {method:"POST", body: JSON.stringify(body)});
    if (res.success) { toast(res.message); after ? await after(payload, res.data) : await render(); e.target.reset(); }
  });
}
function delBtn(kind, id) { return `<button class="btn btn-sm btn-outline-danger" data-del="${kind}:${id}">Delete</button>`; }
function bindDelete(kind, path) {
  document.querySelectorAll(`[data-del^="${kind}:"]`).forEach(btn => btn.onclick = async () => {
    if (!confirm("You are about to delete this item. Related records may also be affected.")) return;
    const id = btn.dataset.del.split(":")[1];
    const res = await api(`${path}/${id}${qs()}`, {method:"DELETE"});
    if (res.success) { toast(res.message); render(); }
  });
}
function bar(value) { return `<div><div class="progress"><div class="progress-bar" style="width:${Math.min(100, value)}%"></div></div><small>${value}%</small></div>`; }

start();

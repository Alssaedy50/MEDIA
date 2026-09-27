const queryEl = document.getElementById("query");
const modeEl = document.getElementById("mode");
const topKEl = document.getElementById("top-k");
const askEl = document.getElementById("ask");
const resultEl = document.getElementById("result");
const statusEl = document.getElementById("status");
const scopeEl = document.getElementById("scope");
const installEl = document.getElementById("install");

let deferredInstallPrompt = null;

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#039;");
}

function renderSources(sources) {
  if (!sources?.length) return "";
  return `
    <div class="section-title">Registered sources</div>
    <div>
      ${sources.map(source => `
        <div class="source">
          <strong>${escapeHtml(source.title || "Untitled source")}</strong>
          ${source.year ? ` <span class="badge">${escapeHtml(source.year)}</span>` : ""}
          ${source.url ? `<br><a href="${escapeHtml(source.url)}" target="_blank" rel="noopener noreferrer">${escapeHtml(source.url)}</a>` : ""}
        </div>
      `).join("")}
    </div>`;
}

function renderResult(data) {
  const meta = `
    <div class="meta">
      <span class="badge">${escapeHtml(data.evidence_state)}</span>
      <span class="badge">Mode: ${escapeHtml(data.mode)}</span>
      <span class="badge">Evidence: ${data.retrieved_ids?.length ?? 0}</span>
      <span class="badge">${escapeHtml(data.knowledge_scope)}</span>
    </div>`;

  if (data.abstain) {
    resultEl.innerHTML = `
      <div class="answer-label">Safe abstention</div>
      <div class="abstain">${escapeHtml(data.answer)}</div>
      ${meta}
    `;
    return;
  }

  let body = `
    <div class="answer-label">MEDIA answer</div>
    <div class="answer">${escapeHtml(data.answer)}</div>
    ${meta}`;

  if (data.comparison?.length) {
    body += `
      <div class="section-title">Comparison</div>
      <div class="comparison">
        ${data.comparison.map(item => `
          <article class="card">
            <h3>${escapeHtml(item.concept || item.id)}</h3>
            ${item.definition ? `<p><strong>Definition:</strong> ${escapeHtml(item.definition)}</p>` : ""}
            ${item.structure ? `<p><strong>Structure:</strong> ${escapeHtml(item.structure)}</p>` : ""}
            ${item.function ? `<p><strong>Function:</strong> ${escapeHtml(item.function)}</p>` : ""}
            ${item.clinical_relevance ? `<p><strong>Clinical relevance:</strong> ${escapeHtml(item.clinical_relevance)}</p>` : ""}
          </article>`).join("")}
      </div>`;
  }

  if (data.key_points?.length) {
    body += `
      <div class="section-title">Key points</div>
      <ul>${data.key_points.map(point => `<li>${escapeHtml(point)}</li>`).join("")}</ul>`;
  }

  if (data.terms?.length) {
    body += `
      <div class="section-title">Medical terms</div>
      <div class="meta">${data.terms.map(term => `<span class="badge">${escapeHtml(term)}</span>`).join("")}</div>`;
  }

  body += renderSources(data.sources);
  resultEl.innerHTML = body;
}

async function checkHealth() {
  try {
    const response = await fetch("/health");
    const data = await response.json();
    statusEl.textContent = data.status === "ok" ? "API online" : "API issue";
  } catch {
    statusEl.textContent = "API offline";
  }

  try {
    const response = await fetch("/api/scope");
    const data = await response.json();
    scopeEl.textContent = data.knowledge_scope;
  } catch {
    scopeEl.textContent = "";
  }

  try {
    const response = await fetch("/api/runtime");
    const data = await response.json();
    const suffix = data.weights_available ? " · model ready" : " · retrieval mode";
    statusEl.textContent = `${statusEl.textContent}${suffix}`;
  } catch {
    // Health status remains authoritative for the shell.
  }
}

async function ask() {
  const query = queryEl.value.trim();
  if (!query) {
    queryEl.focus();
    return;
  }

  askEl.disabled = true;
  askEl.textContent = "Retrieving…";
  resultEl.innerHTML = `
    <div class="empty-state">
      <span class="pulse"></span>
      <h2>Retrieving evidence</h2>
      <p>MEDIA is searching the registered knowledge base.</p>
    </div>`;

  try {
    const response = await fetch("/api/answer", {
      method: "POST",
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify({
        query,
        mode: modeEl.value,
        top_k: Number(topKEl.value)
      })
    });
    const data = await response.json();
    if (!response.ok) throw new Error(data.detail || "Request failed.");
    renderResult(data);
  } catch (error) {
    resultEl.innerHTML = `
      <div class="answer-label">Request error</div>
      <div class="abstain">${escapeHtml(error.message)}</div>`;
  } finally {
    askEl.disabled = false;
    askEl.textContent = "Ask MEDIA";
  }
}

window.addEventListener("beforeinstallprompt", event => {
  event.preventDefault();
  deferredInstallPrompt = event;
  installEl.classList.remove("hidden");
});

installEl.addEventListener("click", async () => {
  if (!deferredInstallPrompt) return;
  deferredInstallPrompt.prompt();
  await deferredInstallPrompt.userChoice;
  deferredInstallPrompt = null;
  installEl.classList.add("hidden");
});

window.addEventListener("appinstalled", () => {
  installEl.classList.add("hidden");
});

if ("serviceWorker" in navigator) {
  window.addEventListener("load", () => {
    navigator.serviceWorker.register("/static/service-worker.js").catch(() => {});
  });
}

askEl.addEventListener("click", ask);
queryEl.addEventListener("keydown", event => {
  if ((event.ctrlKey || event.metaKey) && event.key === "Enter") ask();
});

document.querySelectorAll("[data-query]").forEach(button => {
  button.addEventListener("click", () => {
    queryEl.value = button.dataset.query;
    queryEl.focus();
  });
});

checkHealth();

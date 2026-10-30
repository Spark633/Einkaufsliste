'use strict';

/* ------------------------------------------------------------------
 * Datenhaltung
 * state = {
 *   lists:   [{ id, name, emoji, items: [{ id, name, qty, done }] }],
 *   history: { "milch": { name, count, last, lists: { <listId>: count } } },
 *   seenHint: bool
 * }
 * ------------------------------------------------------------------ */
const STORAGE_KEY = 'einkaufsliste.v1';
const SHARE_PREFIX = 'EKL1:';
const EMOJIS = ['🛒', '🥦', '🥖', '🍎', '🧀', '🥩', '🍷', '🧴', '💊', '🐾', '🏠', '🔧', '🎒', '🧳', '🏕️', '🎁', '📝', '⭐'];

const $ = (sel) => document.querySelector(sel);
const bridge = window.AndroidBridge || null;

let state = load();
let currentListId = null;

function uid() {
  return Date.now().toString(36) + Math.random().toString(36).slice(2, 8);
}

function load() {
  try {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (raw) {
      const s = JSON.parse(raw);
      s.lists = s.lists || [];
      s.history = s.history || {};
      return s;
    }
  } catch (e) { /* kaputte Daten -> neu anfangen */ }
  return {
    lists: [{ id: uid(), name: 'Einkauf', emoji: '🛒', items: [] }],
    history: {},
    seenHint: false,
  };
}

function save() {
  localStorage.setItem(STORAGE_KEY, JSON.stringify(state));
}

const getList = (id) => state.lists.find((l) => l.id === id);
const currentList = () => getList(currentListId);
const keyOf = (name) => name.trim().toLocaleLowerCase('de');

function remember(name, listId) {
  const key = keyOf(name);
  const h = state.history[key] || { name: name.trim(), count: 0, last: 0, lists: {} };
  h.name = name.trim();
  h.count += 1;
  h.last = Date.now();
  if (listId) h.lists[listId] = (h.lists[listId] || 0) + 1;
  state.history[key] = h;
}

/* ------------------------------------------------------------------
 * Hilfsfunktionen
 * ------------------------------------------------------------------ */
function esc(s) {
  return String(s).replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}

const ICON = {
  check: '<svg viewBox="0 0 24 24"><path d="M9 16.2 4.8 12l-1.4 1.4L9 19 21 7l-1.4-1.4z"/></svg>',
  undo: '<svg viewBox="0 0 24 24"><path d="M12.5 8c-2.65 0-5.05 1-6.9 2.6L2 7v9h9l-3.62-3.62A8 8 0 0 1 20.1 16l2.37-.78A10.5 10.5 0 0 0 12.5 8"/></svg>',
  trash: '<svg viewBox="0 0 24 24"><path d="M6 19a2 2 0 0 0 2 2h8a2 2 0 0 0 2-2V7H6zM19 4h-3.5l-1-1h-5l-1 1H5v2h14z"/></svg>',
  more: '<svg viewBox="0 0 24 24"><path d="M12 8a2 2 0 1 0 0-4 2 2 0 0 0 0 4m0 2a2 2 0 1 0 0 4 2 2 0 0 0 0-4m0 6a2 2 0 1 0 0 4 2 2 0 0 0 0-4"/></svg>',
  close: '<svg viewBox="0 0 24 24"><path d="M19 6.4 17.6 5 12 10.6 6.4 5 5 6.4 10.6 12 5 17.6 6.4 19 12 13.4 17.6 19 19 17.6 13.4 12z"/></svg>',
};

/* ------------------------------------------------------------------
 * Navigation
 * ------------------------------------------------------------------ */
function showLists() {
  currentListId = null;
  $('#view-list').hidden = true;
  $('#view-lists').hidden = false;
  $('#btn-back').hidden = true;
  $('#title').textContent = 'Meine Listen';
  renderLists();
}

function openList(id) {
  currentListId = id;
  $('#view-lists').hidden = true;
  $('#view-list').hidden = false;
  $('#btn-back').hidden = false;
  $('#add-input').value = '';
  hideSuggestions();
  renderList();
  window.scrollTo(0, 0);
}

/* Wird von der Android-Zurück-Taste aufgerufen. true = selbst behandelt. */
window.onAndroidBack = function () {
  if (!$('#overlay').hidden) { closeSheet(); return true; }
  if (!$('#suggestions').hidden) { hideSuggestions(); $('#add-input').blur(); return true; }
  if (currentListId) { showLists(); return true; }
  return false;
};

/* ------------------------------------------------------------------
 * Listen-Übersicht
 * ------------------------------------------------------------------ */
function renderLists() {
  const ul = $('#lists');
  ul.innerHTML = '';
  $('#lists-empty').hidden = state.lists.length > 0;
  for (const list of state.lists) {
    const open = list.items.filter((i) => !i.done).length;
    const done = list.items.length - open;
    const li = document.createElement('li');
    li.className = 'card';
    li.innerHTML = `
      <span class="card-emoji">${esc(list.emoji || '📝')}</span>
      <button class="card-main">
        <div class="card-name">${esc(list.name)}</div>
        <div class="card-meta">${open === 0 && done === 0 ? 'leer' : `${open} offen` + (done ? ` · ${done} erledigt` : '')}</div>
      </button>
      <button class="icon-btn" aria-label="Optionen">${ICON.more}</button>`;
    li.querySelector('.card-main').onclick = () => openList(list.id);
    li.querySelector('.icon-btn').onclick = () => listMenu(list.id);
    ul.appendChild(li);
  }
}

function listMenu(id) {
  const list = getList(id);
  openSheet(`
    <h2>${esc(list.emoji)} ${esc(list.name)}</h2>
    <div class="menu-list">
      <button data-a="edit">✏️ Umbenennen / Symbol ändern</button>
      <button data-a="share">📤 Liste teilen</button>
      <button data-a="copy">📄 Liste duplizieren</button>
      <button data-a="delete" class="danger">🗑️ Liste löschen</button>
    </div>`, {
    edit: () => editListDialog(id),
    share: () => shareList(id),
    copy: () => {
      const copy = { id: uid(), name: list.name + ' (Kopie)', emoji: list.emoji, items: list.items.map((i) => ({ ...i, id: uid() })) };
      state.lists.splice(state.lists.indexOf(list) + 1, 0, copy);
      save(); closeSheet(); renderLists();
    },
    delete: () => deleteListDialog(id),
  });
}

function deleteListDialog(id) {
  const list = getList(id);
  confirmDialog(`„${list.name}“ wirklich löschen?`, 'Alle Einträge dieser Liste gehen verloren. Deine Vorschläge bleiben erhalten.', 'Löschen', () => {
    state.lists = state.lists.filter((l) => l.id !== id);
    save();
    if (currentListId === id) showLists(); else renderLists();
  });
}

function editListDialog(id) {
  const list = id ? getList(id) : null;
  let emoji = list ? list.emoji : EMOJIS[0];
  openSheet(`
    <h2>${list ? 'Liste bearbeiten' : 'Neue Liste'}</h2>
    <label for="f-name">Name</label>
    <input id="f-name" maxlength="60" placeholder="z. B. Rewe, Drogerie, Urlaub packen" value="${list ? esc(list.name) : ''}">
    <label>Symbol</label>
    <div class="emoji-row">${EMOJIS.map((e) => `<button type="button" data-emoji="${e}" class="${e === emoji ? 'selected' : ''}">${e}</button>`).join('')}</div>
    <div class="sheet-actions">
      <button class="btn" data-a="cancel">Abbrechen</button>
      <button class="btn primary" data-a="ok">${list ? 'Speichern' : 'Anlegen'}</button>
    </div>`, {
    cancel: closeSheet,
    ok: () => {
      const name = $('#f-name').value.trim();
      if (!name) { $('#f-name').focus(); return; }
      if (list) {
        list.name = name; list.emoji = emoji;
      } else {
        const nl = { id: uid(), name, emoji, items: [] };
        state.lists.push(nl);
        save(); closeSheet(); openList(nl.id);
        return;
      }
      save(); closeSheet();
      if (currentListId) renderList(); else renderLists();
    },
  });
  $('#sheet').querySelectorAll('[data-emoji]').forEach((b) => {
    b.onclick = () => {
      emoji = b.dataset.emoji;
      $('#sheet').querySelectorAll('[data-emoji]').forEach((x) => x.classList.toggle('selected', x === b));
    };
  });
  const input = $('#f-name');
  input.onkeydown = (e) => { if (e.key === 'Enter') $('#sheet [data-a="ok"]').click(); };
  setTimeout(() => input.focus(), 50);
}

/* ------------------------------------------------------------------
 * Einzelne Liste
 * ------------------------------------------------------------------ */
function renderList() {
  const list = currentList();
  if (!list) { showLists(); return; }
  $('#title').textContent = `${list.emoji || ''} ${list.name}`.trim();

  const open = list.items.filter((i) => !i.done);
  const done = list.items.filter((i) => i.done);

  const ulOpen = $('#items-open');
  ulOpen.innerHTML = '';
  open.forEach((it) => ulOpen.appendChild(itemEl(it)));

  const empty = $('#items-empty');
  empty.hidden = open.length > 0;
  empty.innerHTML = done.length
    ? 'Alles erledigt! 🎉'
    : 'Die Liste ist leer.<br>Tippe oben einen Artikel ein.';

  if (open.length && !state.seenHint) {
    const hint = document.createElement('li');
    hint.className = 'hint';
    hint.innerHTML = 'Tipp: Artikel <b>nach rechts wischen</b> = erledigt,<br><b>nach links wischen</b> = löschen, antippen = bearbeiten.';
    ulOpen.appendChild(hint);
  }

  $('#done-section').hidden = done.length === 0;
  $('#done-count').textContent = `Erledigt (${done.length})`;
  const ulDone = $('#items-done');
  ulDone.innerHTML = '';
  done.forEach((it) => ulDone.appendChild(itemEl(it)));
}

function itemEl(it) {
  const li = document.createElement('li');
  li.className = 'item' + (it.done ? ' done' : '');
  li.dataset.id = it.id;
  li.innerHTML = `
    <div class="item-bg">
      <span class="bg-left-hint">${it.done ? ICON.undo + ' Zurückholen' : ICON.check + ' Erledigt'}</span>
      <span class="bg-right-hint">Löschen ${ICON.trash}</span>
    </div>
    <div class="item-row">
      <span class="item-check"></span>
      <span class="item-name">${esc(it.name)}</span>
      ${it.qty ? `<span class="item-qty">${esc(it.qty)}</span>` : ''}
    </div>`;
  attachSwipe(li, it);
  return li;
}

/* Wischgesten: rechts = erledigt umschalten, links = löschen. Antippen = bearbeiten.
   Ein einfacher Tipp markiert also nie etwas als erledigt. */
function attachSwipe(li, it) {
  const row = li.querySelector('.item-row');
  let startX = 0, startY = 0, dx = 0, active = false, swiping = false, pointerId = null;
  const THRESHOLD = () => Math.min(140, li.offsetWidth * 0.35);

  row.addEventListener('pointerdown', (e) => {
    if (e.button !== 0) return;
    active = true; swiping = false; dx = 0;
    startX = e.clientX; startY = e.clientY; pointerId = e.pointerId;
    row.classList.remove('animating');
  });

  row.addEventListener('pointermove', (e) => {
    if (!active || e.pointerId !== pointerId) return;
    const mx = e.clientX - startX, my = e.clientY - startY;
    if (!swiping) {
      if (Math.abs(my) > 10 && Math.abs(my) > Math.abs(mx)) { active = false; return; } // scrollt
      if (Math.abs(mx) > 10) { swiping = true; row.setPointerCapture(pointerId); }
    }
    if (swiping) {
      dx = mx;
      li.classList.toggle('swipe-left', dx < 0);
      row.style.transform = `translateX(${dx}px)`;
    }
  });

  const end = (e) => {
    if (!active || e.pointerId !== pointerId) return;
    active = false;
    if (!swiping) {
      if (e.type === 'pointerup') editItemDialog(it.id);
      return;
    }
    row.classList.add('animating');
    if (dx > THRESHOLD()) {
      row.style.transform = `translateX(${li.offsetWidth}px)`;
      vibrate(20);
      setTimeout(() => toggleDone(it.id), 180);
    } else if (dx < -THRESHOLD()) {
      row.style.transform = `translateX(${-li.offsetWidth}px)`;
      vibrate(20);
      setTimeout(() => deleteItem(it.id), 180);
    } else {
      row.style.transform = '';
    }
  };
  row.addEventListener('pointerup', end);
  row.addEventListener('pointercancel', end);
}

function vibrate(ms) {
  if (bridge && bridge.vibrate) bridge.vibrate(ms);
  else if (navigator.vibrate) navigator.vibrate(ms);
}

function toggleDone(itemId) {
  const list = currentList();
  const it = list.items.find((i) => i.id === itemId);
  if (!it) return;
  it.done = !it.done;
  // Neu erledigte Artikel nach oben in den Erledigt-Bereich
  list.items = list.items.filter((i) => i !== it);
  if (it.done) list.items.unshift(it); else list.items.push(it);
  state.seenHint = true;
  save(); renderList();
  snackbar(it.done ? `„${it.name}“ erledigt` : `„${it.name}“ zurückgeholt`, 'Rückgängig', () => toggleDone(itemId));
}

function deleteItem(itemId) {
  const list = currentList();
  const idx = list.items.findIndex((i) => i.id === itemId);
  if (idx < 0) return;
  const [it] = list.items.splice(idx, 1);
  state.seenHint = true;
  save(); renderList();
  snackbar(`„${it.name}“ gelöscht`, 'Rückgängig', () => {
    list.items.splice(idx, 0, it);
    save(); renderList();
  });
}

function addItem(name, qty = '') {
  name = name.trim().replace(/\s+/g, ' ');
  if (!name) return;
  const list = currentList();
  const existing = list.items.find((i) => keyOf(i.name) === keyOf(name));
  if (existing && !existing.done) {
    flashItem(existing.id);
    snackbar(`„${existing.name}“ steht schon auf der Liste`);
    return;
  }
  if (existing) {
    // war erledigt -> wieder auf die Liste holen
    existing.done = false;
    if (qty) existing.qty = qty;
    list.items = list.items.filter((i) => i !== existing);
    list.items.push(existing);
  } else {
    list.items.push({ id: uid(), name, qty, done: false });
  }
  remember(name, list.id);
  save(); renderList();
}

function flashItem(id) {
  const el = document.querySelector(`.item[data-id="${id}"]`);
  if (!el) return;
  el.scrollIntoView({ block: 'center', behavior: 'smooth' });
  el.classList.remove('flash'); void el.offsetWidth; el.classList.add('flash');
}

function editItemDialog(itemId) {
  const list = currentList();
  const it = list.items.find((i) => i.id === itemId);
  if (!it) return;
  openSheet(`
    <h2>Artikel bearbeiten</h2>
    <label for="f-item">Name</label>
    <input id="f-item" maxlength="80" value="${esc(it.name)}">
    <label for="f-qty">Menge / Notiz (optional)</label>
    <input id="f-qty" maxlength="40" placeholder="z. B. 2 Stück, 500 g, Bio" value="${esc(it.qty || '')}">
    <button class="btn block" data-a="toggle">${it.done ? ICON.undo + ' Wieder auf die Liste' : ICON.check + ' Als erledigt markieren'}</button>
    <div class="sheet-actions">
      <button class="btn danger" data-a="del" style="margin-right:auto">Löschen</button>
      <button class="btn" data-a="cancel">Abbrechen</button>
      <button class="btn primary" data-a="ok">Speichern</button>
    </div>`, {
    cancel: closeSheet,
    del: () => { closeSheet(); deleteItem(itemId); },
    toggle: () => { closeSheet(); toggleDone(itemId); },
    ok: () => {
      const name = $('#f-item').value.trim().replace(/\s+/g, ' ');
      if (!name) { $('#f-item').focus(); return; }
      if (keyOf(name) !== keyOf(it.name)) remember(name, list.id);
      it.name = name;
      it.qty = $('#f-qty').value.trim();
      save(); closeSheet(); renderList();
    },
  });
}

/* ------------------------------------------------------------------
 * Vorschläge beim Tippen
 * ------------------------------------------------------------------ */
function suggestionsFor(query) {
  const list = currentList();
  const onList = new Set(list.items.filter((i) => !i.done).map((i) => keyOf(i.name)));
  const q = keyOf(query);
  const result = [];
  for (const [key, h] of Object.entries(state.history)) {
    if (onList.has(key)) continue;
    let score;
    if (!q) {
      score = 0;
    } else if (key.startsWith(q)) {
      score = 1000;
    } else if (key.split(/[\s-]+/).some((w) => w.startsWith(q))) {
      score = 600;
    } else if (key.includes(q)) {
      score = 300;
    } else {
      continue;
    }
    if (key === q) score += 500;
    score += Math.min(h.count, 50) + (h.lists[list.id] || 0) * 4 + h.last / 1e13;
    result.push({ h, score });
  }
  result.sort((a, b) => b.score - a.score);
  return result.slice(0, q ? 10 : 14).map((r) => r.h);
}

function highlight(name, q) {
  if (!q) return esc(name);
  const i = name.toLocaleLowerCase('de').indexOf(q.toLocaleLowerCase('de'));
  if (i < 0) return esc(name);
  return esc(name.slice(0, i)) + '<b>' + esc(name.slice(i, i + q.length)) + '</b>' + esc(name.slice(i + q.length));
}

function updateSuggestions() {
  const input = $('#add-input');
  const q = input.value.trim();
  const items = suggestionsFor(q);
  const box = $('#suggestions');
  if (!items.length) { hideSuggestions(); return; }
  const title = q ? 'Vorschläge – antippen zum Hinzufügen' : 'Schon mal gekauft – antippen zum Hinzufügen';
  box.innerHTML = `<div class="suggestions-title">${title}</div><div class="chips">${
    items.map((h) => `<button type="button" class="chip" data-name="${esc(h.name)}">${highlight(h.name, q)}</button>`).join('')
  }</div>`;
  box.hidden = false;
}

function hideSuggestions() {
  $('#suggestions').hidden = true;
}

/* ------------------------------------------------------------------
 * Teilen & Importieren
 * ------------------------------------------------------------------ */
function b64urlEncode(str) {
  const bytes = new TextEncoder().encode(str);
  let bin = '';
  bytes.forEach((b) => { bin += String.fromCharCode(b); });
  return btoa(bin).replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

function b64urlDecode(s) {
  s = s.replace(/-/g, '+').replace(/_/g, '/');
  while (s.length % 4) s += '=';
  const bin = atob(s);
  return new TextDecoder().decode(Uint8Array.from(bin, (c) => c.charCodeAt(0)));
}

function shareText(list) {
  const open = list.items.filter((i) => !i.done);
  const lines = open.map((i) => `• ${i.name}${i.qty ? ` (${i.qty})` : ''}`);
  const payload = { n: list.name, e: list.emoji, i: open.map((i) => (i.qty ? [i.name, i.qty] : [i.name])) };
  return `${list.emoji || '📝'} ${list.name}\n${lines.join('\n') || '(leer)'}\n\n` +
    `Zum Übernehmen in die Einkaufslisten-App diese Nachricht kopieren und in der App „Liste importieren“ wählen:\n` +
    SHARE_PREFIX + b64urlEncode(JSON.stringify(payload));
}

async function shareList(id) {
  const list = getList(id);
  const text = shareText(list);
  closeSheet();
  if (bridge && bridge.share) {
    bridge.share(text);
    return;
  }
  if (navigator.share) {
    try { await navigator.share({ title: list.name, text }); return; } catch (e) { if (e.name === 'AbortError') return; }
  }
  try {
    await navigator.clipboard.writeText(text);
    snackbar('Liste in die Zwischenablage kopiert');
  } catch (e) {
    openSheet(`<h2>Liste teilen</h2><p>Text kopieren und verschicken:</p><textarea readonly>${esc(text)}</textarea>
      <div class="sheet-actions"><button class="btn primary" data-a="close">Fertig</button></div>`, { close: closeSheet });
  }
}

function parseShared(text) {
  const m = String(text || '').match(/EKL1:([A-Za-z0-9_-]+)/);
  if (!m) return null;
  try {
    const p = JSON.parse(b64urlDecode(m[1]));
    if (!p || typeof p.n !== 'string' || !Array.isArray(p.i)) return null;
    return {
      name: p.n.slice(0, 60) || 'Geteilte Liste',
      emoji: typeof p.e === 'string' ? p.e.slice(0, 8) : '📝',
      items: p.i.filter((x) => Array.isArray(x) && typeof x[0] === 'string' && x[0].trim())
        .map((x) => ({ name: x[0].trim().slice(0, 80), qty: typeof x[1] === 'string' ? x[1].slice(0, 40) : '' })),
    };
  } catch (e) {
    return null;
  }
}

/* Fragt, ob die geteilte Liste neu angelegt oder in eine bestehende übernommen werden soll. */
function importShared(text) {
  const data = parseShared(text);
  if (!data) return false;
  const same = state.lists.find((l) => keyOf(l.name) === keyOf(data.name));
  const preview = data.items.slice(0, 8).map((i) => esc(i.name)).join(', ') + (data.items.length > 8 ? ' …' : '');
  openSheet(`
    <h2>Geteilte Liste empfangen</h2>
    <p><b>${esc(data.emoji)} ${esc(data.name)}</b> mit ${data.items.length} Artikel${data.items.length === 1 ? '' : 'n'}${preview ? ':<br>' + preview : ''}</p>
    <div class="menu-list">
      <button data-a="new">➕ Als neue Liste anlegen</button>
      ${same ? `<button data-a="merge">🔀 In „${esc(same.name)}“ übernehmen (fehlende Artikel ergänzen)</button>` : ''}
    </div>
    <div class="sheet-actions"><button class="btn" data-a="cancel">Abbrechen</button></div>`, {
    cancel: closeSheet,
    new: () => {
      const nl = { id: uid(), name: same ? data.name + ' (geteilt)' : data.name, emoji: data.emoji, items: [] };
      data.items.forEach((i) => { nl.items.push({ id: uid(), name: i.name, qty: i.qty, done: false }); remember(i.name, nl.id); });
      state.lists.push(nl);
      save(); closeSheet(); openList(nl.id);
    },
    merge: () => {
      let added = 0;
      data.items.forEach((i) => {
        const ex = same.items.find((x) => keyOf(x.name) === keyOf(i.name));
        if (ex) {
          if (ex.done) { ex.done = false; added++; }
          return;
        }
        same.items.push({ id: uid(), name: i.name, qty: i.qty, done: false });
        remember(i.name, same.id);
        added++;
      });
      save(); closeSheet(); openList(same.id);
      snackbar(`${added} Artikel übernommen`);
    },
  });
  return true;
}

function importDialog() {
  openSheet(`
    <h2>Liste importieren</h2>
    <p>Füge hier die geteilte Nachricht ein (lange tippen → Einfügen).</p>
    <textarea id="f-import" placeholder="EKL1:…"></textarea>
    <div class="sheet-actions">
      <button class="btn" data-a="cancel">Abbrechen</button>
      <button class="btn primary" data-a="ok">Importieren</button>
    </div>`, {
    cancel: closeSheet,
    ok: () => {
      const text = $('#f-import').value;
      if (!importShared(text)) snackbar('Kein gültiger Listen-Code gefunden');
    },
  });
  // Zwischenablage automatisch einfügen, wenn dort ein Code liegt
  const clip = bridge && bridge.getClipboard ? Promise.resolve(bridge.getClipboard())
    : (navigator.clipboard && navigator.clipboard.readText ? navigator.clipboard.readText() : Promise.reject());
  clip.then((t) => { if (t && t.includes(SHARE_PREFIX) && $('#f-import')) $('#f-import').value = t; }).catch(() => {});
}

/* Wird von Android aufgerufen, wenn der App Text über „Teilen“ geschickt wird. */
window.handleSharedText = function (text) {
  if (!importShared(text)) snackbar('Die geteilte Nachricht enthält keine Liste');
};

/* ------------------------------------------------------------------
 * Hauptmenü
 * ------------------------------------------------------------------ */
function mainMenu() {
  const inList = !!currentListId;
  openSheet(`
    <div class="menu-list">
      ${inList ? `
        <button data-a="share">📤 Diese Liste teilen</button>
        <button data-a="edit">✏️ Liste umbenennen</button>
        <button data-a="uncheck">↩️ Alle Erledigten wieder auf die Liste</button>` : ''}
      <button data-a="new">➕ Neue Liste</button>
      <button data-a="import">📥 Liste importieren</button>
      <button data-a="history">💡 Vorschläge verwalten</button>
      ${inList ? '<button data-a="delete" class="danger">🗑️ Liste löschen</button>' : ''}
    </div>`, {
    share: () => shareList(currentListId),
    edit: () => editListDialog(currentListId),
    uncheck: () => {
      const list = currentList();
      list.items.forEach((i) => { i.done = false; });
      save(); closeSheet(); renderList();
    },
    new: () => editListDialog(null),
    import: importDialog,
    history: historyDialog,
    delete: () => deleteListDialog(currentListId),
  });
}

function historyDialog() {
  const render = () => {
    const entries = Object.entries(state.history).sort((a, b) => a[1].name.localeCompare(b[1].name, 'de'));
    openSheet(`
      <h2>Vorschläge verwalten</h2>
      <p>Diese Artikel werden beim Eintippen vorgeschlagen. Tippe auf ✕, um einen Vorschlag zu entfernen.</p>
      ${entries.length ? `<ul class="history-list">${entries.map(([k, h]) => `
        <li><span>${esc(h.name)} <small>· ${h.count}×</small></span>
        <button class="icon-btn" data-del="${esc(k)}" aria-label="Entfernen">${ICON.close}</button></li>`).join('')}</ul>`
        : '<p class="empty">Noch keine Vorschläge gespeichert.</p>'}
      <div class="sheet-actions">
        ${entries.length ? '<button class="btn" data-a="clear" style="margin-right:auto">Alle löschen</button>' : ''}
        <button class="btn primary" data-a="close">Fertig</button>
      </div>`, {
      close: closeSheet,
      clear: () => confirmDialog('Alle Vorschläge löschen?', 'Die Listen selbst bleiben erhalten.', 'Alle löschen', () => {
        state.history = {}; save();
      }),
    });
    $('#sheet').querySelectorAll('[data-del]').forEach((b) => {
      b.onclick = () => { delete state.history[b.dataset.del]; save(); render(); };
    });
  };
  render();
}

/* ------------------------------------------------------------------
 * Dialog & Snackbar
 * ------------------------------------------------------------------ */
function openSheet(html, actions = {}) {
  const sheet = $('#sheet');
  sheet.innerHTML = html;
  sheet.querySelectorAll('[data-a]').forEach((el) => {
    const fn = actions[el.dataset.a];
    if (fn) el.onclick = fn;
  });
  $('#overlay').hidden = false;
}

function closeSheet() {
  $('#overlay').hidden = true;
  $('#sheet').innerHTML = '';
}

function confirmDialog(title, text, okLabel, onOk) {
  openSheet(`
    <h2>${esc(title)}</h2>
    <p>${esc(text)}</p>
    <div class="sheet-actions">
      <button class="btn" data-a="cancel">Abbrechen</button>
      <button class="btn danger" data-a="ok">${esc(okLabel)}</button>
    </div>`, {
    cancel: closeSheet,
    ok: () => { closeSheet(); onOk(); },
  });
}

let snackTimer = null;
function snackbar(text, actionLabel, action) {
  const bar = $('#snackbar');
  $('#snackbar-text').textContent = text;
  const btn = $('#snackbar-action');
  btn.hidden = !actionLabel;
  btn.textContent = actionLabel || '';
  btn.onclick = () => { bar.hidden = true; clearTimeout(snackTimer); if (action) action(); };
  bar.hidden = false;
  clearTimeout(snackTimer);
  snackTimer = setTimeout(() => { bar.hidden = true; }, actionLabel ? 5000 : 2500);
}

/* ------------------------------------------------------------------
 * Ereignisse
 * ------------------------------------------------------------------ */
$('#btn-back').onclick = showLists;
$('#btn-menu').onclick = mainMenu;
$('#btn-new-list').onclick = () => editListDialog(null);
$('#btn-clear-done').onclick = () => {
  const list = currentList();
  const removed = list.items.filter((i) => i.done);
  list.items = list.items.filter((i) => !i.done);
  save(); renderList();
  snackbar(`${removed.length} erledigte Artikel entfernt`, 'Rückgängig', () => {
    list.items.push(...removed);
    save(); renderList();
  });
};
$('#overlay').addEventListener('click', (e) => { if (e.target.id === 'overlay') closeSheet(); });

const addInput = $('#add-input');
$('#add-form').addEventListener('submit', (e) => {
  e.preventDefault();
  addItem(addInput.value);
  addInput.value = '';
  updateSuggestions();
  addInput.focus();
});
addInput.addEventListener('input', updateSuggestions);
addInput.addEventListener('focus', updateSuggestions);
// Vorschlag antippen: "pointerdown" + preventDefault, damit die Tastatur offen bleibt
$('#suggestions').addEventListener('pointerdown', (e) => {
  const chip = e.target.closest('.chip');
  if (!chip) return;
  e.preventDefault();
  addItem(chip.dataset.name);
  addInput.value = '';
  updateSuggestions();
});
document.addEventListener('pointerdown', (e) => {
  if (!e.target.closest('#suggestions') && !e.target.closest('#add-form')) hideSuggestions();
});

/* ------------------------------------------------------------------
 * Start
 * ------------------------------------------------------------------ */
save();
showLists();
if (state.lists.length === 1 && state.lists[0].items.length === 0 && Object.keys(state.history).length === 0) {
  openList(state.lists[0].id);
}
if (bridge && bridge.consumeSharedText) {
  const shared = bridge.consumeSharedText();
  if (shared) window.handleSharedText(shared);
}
if (!bridge && 'serviceWorker' in navigator && location.protocol === 'https:') {
  navigator.serviceWorker.register('sw.js').catch(() => {});
}

const createForm = document.getElementById('create-form');
const createSubmit = document.getElementById('create-submit');
const createMessage = document.getElementById('create-message');
const resultPanel = document.getElementById('result-panel');
const resultShortUrl = document.getElementById('result-short-url');
const resultOriginalUrl = document.getElementById('result-original-url');
const resultCreatedAt = document.getElementById('result-created-at');
const resultExpiresAt = document.getElementById('result-expires-at');
const statsForm = document.getElementById('stats-form');
const statsSubmit = document.getElementById('stats-submit');
const statsMessage = document.getElementById('stats-message');
const statsPanel = document.getElementById('stats-panel');
const statsEmpty = document.getElementById('stats-empty');
const statsCodeInput = document.getElementById('stats-code');
const statsShortCode = document.getElementById('stats-short-code');
const statsOriginalUrl = document.getElementById('stats-original-url');
const statsCreatedAt = document.getElementById('stats-created-at');
const statsExpiresAt = document.getElementById('stats-expires-at');
const statsClickCount = document.getElementById('stats-click-count');
const statsLastAccessed = document.getElementById('stats-last-accessed');
const statsStatus = document.getElementById('stats-status');
const statsCopyBtn = document.getElementById('stats-copy-url');
const deleteForm = document.getElementById('delete-form');
const deleteSubmit = document.getElementById('delete-submit');
const deleteMessage = document.getElementById('delete-message');
const deleteCodeInput = document.getElementById('delete-code');
const originalUrlInput = document.getElementById('original-url');
const expiresAtInput = document.getElementById('expires-at');
const heroButton = document.querySelector('[data-scroll]');
const toastRegion = document.getElementById('toast-region');
const healthText = document.getElementById('health-text');
const healthSubtext = document.getElementById('health-subtext');
const healthDetails = document.getElementById('health-details');
const healthBadge = document.getElementById('health-badge');
const recentLinksContainer = document.getElementById('recent-links');
const recentEmptyState = document.getElementById('recent-empty');
const loadAnalyticsBtn = document.getElementById('load-analytics');

const state = {
  currentShortCode: '',
  currentShortUrl: '',
  recentLinks: [],
};

let activeNotification = null;
let activeNotificationTimer = null;
let activeRequestController = null;
let operationId = 0;

function beginOperation() {
  activeRequestController?.abort();
  activeRequestController = new AbortController();
  operationId += 1;

  return {
    id: operationId,
    signal: activeRequestController.signal,
  };
}

function isCurrentOperation(id) {
  return id === operationId;
}

function clearNotificationTimer() {
  if (activeNotificationTimer) {
    window.clearTimeout(activeNotificationTimer);
    activeNotificationTimer = null;
  }
}

async function hideNotification() {
  if (!activeNotification || !activeNotification.isConnected) {
    activeNotification = null;
    clearNotificationTimer();
    return;
  }

  const notification = activeNotification;
  activeNotification = null;
  clearNotificationTimer();

  notification.remove();
  return Promise.resolve();
}

function showNotification(message, type = 'info') {
  if (!toastRegion) {
    return;
  }

  if (activeNotification && activeNotification.isConnected) {
    activeNotification.remove();
  }
  activeNotification = null;
  clearNotificationTimer();

  const toast = document.createElement('div');
  toast.className = `toast ${type}`;
  toast.setAttribute('role', 'status');
  toast.textContent = message || '';
  toastRegion.appendChild(toast);
  activeNotification = toast;

  if (type !== 'loading') {
    activeNotificationTimer = window.setTimeout(() => {
      hideNotification();
    }, 4200);
  }
}

function setMessage(element, message, type = 'info') {
  if (!element) {
    return;
  }
  element.textContent = message || '';
  element.className = 'status-message';

  if (message) {
    element.classList.add(type);
  }
}

function formatDate(value) {
  if (!value) {
    return 'Never';
  }

  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return 'Unknown';
  }

  return date.toLocaleString();
}

function setBusy(button, isBusy, label) {
  if (!button) {
    return;
  }

  button.disabled = isBusy;
  if (isBusy) {
    button.dataset.originalLabel = button.textContent;
    button.textContent = label;
  } else if (button.dataset.originalLabel) {
    button.textContent = button.dataset.originalLabel;
  }
}

function isSecureUrl(value) {
  try {
    const parsed = new URL(value);
    return parsed.protocol === 'http:' || parsed.protocol === 'https:';
  } catch (_error) {
    return false;
  }
}

async function apiRequest(url, options = {}) {
  const headers = { Accept: 'application/json', ...(options.headers || {}) };
  const response = await fetch(url, { ...options, headers });
  return response;
}

async function parseApiError(response, fallbackMessage) {
  if (!response) {
    return fallbackMessage;
  }

  try {
    const payload = await response.json();
    if (payload && payload.message) {
      return payload.message;
    }
  } catch (_error) {
    // ignore and fall through to friendly defaults
  }

  switch (response.status) {
    case 400:
      return 'Please enter a valid URL.';
    case 404:
      return 'That short code does not exist.';
    case 410:
      return 'This short link has expired.';
    case 429:
      return 'You are creating links too quickly. Try again shortly.';
    case 500:
      return 'The service is temporarily unavailable.';
    default:
      return fallbackMessage;
  }
}

function hideResult(panel) {
  if (panel) {
    panel.classList.add('hidden');
  }
}

function showResult(panel) {
  if (panel) {
    panel.classList.remove('hidden');
  }
}

function updateStatsStatus(isActive) {
  if (isActive) {
    statsStatus.textContent = 'Active';
    statsStatus.className = 'status-badge active';
    return;
  }

  statsStatus.textContent = 'Expired';
  statsStatus.className = 'status-badge expired';
}

function readRecentLinks() {
  const raw = sessionStorage.getItem('linktrim_recent_links');
  if (!raw) {
    state.recentLinks = [];
    return state.recentLinks;
  }

  try {
    const parsed = JSON.parse(raw);
    state.recentLinks = Array.isArray(parsed) ? parsed : [];
  } catch (_error) {
    state.recentLinks = [];
  }

  return state.recentLinks;
}

function persistRecentLinks() {
  sessionStorage.setItem('linktrim_recent_links', JSON.stringify(state.recentLinks.slice(0, 8)));
}

function appendRecentLink(entry) {
  const existing = state.recentLinks.find((item) => item.shortCode === entry.shortCode);
  if (existing) {
    const index = state.recentLinks.indexOf(existing);
    state.recentLinks[index] = entry;
  } else {
    state.recentLinks.unshift(entry);
  }

  state.recentLinks = state.recentLinks.slice(0, 8);
  persistRecentLinks();
  renderRecentLinks();
}

function removeRecentLink(shortCode) {
  state.recentLinks = state.recentLinks.filter((item) => item.shortCode !== shortCode);
  persistRecentLinks();
  renderRecentLinks();
}

function renderRecentLinks() {
  const items = readRecentLinks();
  recentLinksContainer.replaceChildren();

  if (!items.length) {
    recentEmptyState.hidden = false;
    return;
  }

  recentEmptyState.hidden = true;

  items.forEach((item) => {
    const row = document.createElement('div');
    row.className = 'recent-link-item';

    const shortLink = document.createElement('div');
    shortLink.className = 'recent-short';
    shortLink.textContent = item.shortUrl;

    const originalLink = document.createElement('div');
    originalLink.className = 'recent-original';
    originalLink.textContent = item.originalUrl;

    const createdAt = document.createElement('div');
    createdAt.className = 'recent-created';
    createdAt.textContent = formatDate(item.createdAt);

    const actions = document.createElement('div');
    actions.className = 'recent-actions';

    const copyButton = document.createElement('button');
    copyButton.type = 'button';
    copyButton.textContent = 'Copy';
    copyButton.addEventListener('click', () => copyToClipboard(item.shortUrl, 'Short URL copied to the clipboard.'));

    const openButton = document.createElement('button');
    openButton.type = 'button';
    openButton.textContent = 'Open';
    openButton.addEventListener('click', () => {
      window.open(item.shortUrl, '_blank', 'noopener,noreferrer');
    });

    const analyticsButton = document.createElement('button');
    analyticsButton.type = 'button';
    analyticsButton.textContent = 'Analytics';
    analyticsButton.addEventListener('click', () => {
      statsCodeInput.value = item.shortCode;
      handleAnalytics({ preventDefault() {} });
    });

    actions.append(copyButton, openButton, analyticsButton);
    row.append(shortLink, originalLink, createdAt, actions);
    recentLinksContainer.appendChild(row);
  });
}

function renderShortLinkResult(data) {
  const shortUrl = data.shortUrl || '';
  const shortCode = data.shortCode || '';
  state.currentShortCode = shortCode;
  state.currentShortUrl = shortUrl;

  resultShortUrl.textContent = shortUrl || '—';
  resultShortUrl.href = shortUrl || '#';
  resultOriginalUrl.textContent = data.originalUrl || '—';
  resultCreatedAt.textContent = formatDate(data.createdAt);
  resultExpiresAt.textContent = formatDate(data.expiresAt);

  if (shortCode) {
    deleteCodeInput.value = shortCode;
    statsCodeInput.value = shortCode;
  }

  showResult(resultPanel);
  appendRecentLink({ shortCode, shortUrl, originalUrl: data.originalUrl, createdAt: data.createdAt });
}

function renderAnalytics(data) {
  const shortCode = data.shortCode || statsCodeInput.value.trim();
  state.currentShortCode = shortCode;
  state.currentShortUrl = data.shortUrl || `${window.location.origin}/${shortCode}`;

  statsShortCode.textContent = shortCode || '—';
  statsOriginalUrl.textContent = data.originalUrl || '—';
  statsCreatedAt.textContent = formatDate(data.createdAt);
  statsExpiresAt.textContent = formatDate(data.expiresAt);
  statsClickCount.textContent = String(data.clickCount ?? 0);
  statsLastAccessed.textContent = formatDate(data.lastAccessedAt);
  updateStatsStatus(data.active !== false);

  showResult(statsPanel);
  if (statsEmpty) {
    statsEmpty.hidden = true;
  }

  deleteCodeInput.value = shortCode;
  appendRecentLink({ shortCode, shortUrl: state.currentShortUrl, originalUrl: data.originalUrl, createdAt: data.createdAt });
}

async function handleCreateUrl(event) {
  event.preventDefault();

  const originalUrl = originalUrlInput.value.trim();
  if (!originalUrl || !isSecureUrl(originalUrl)) {
    setMessage(createMessage, 'Please enter a valid URL.', 'error');
    originalUrlInput.focus();
    return;
  }

  const expiresAtValue = expiresAtInput.value;
  const payload = { originalUrl };

  if (expiresAtValue) {
    const expirationDate = new Date(expiresAtValue);
    if (Number.isNaN(expirationDate.getTime())) {
      setMessage(createMessage, 'Please enter a valid expiration date and time.', 'error');
      expiresAtInput.focus();
      return;
    }
    payload.expiresAt = expirationDate.toISOString();
  }

  const operation = beginOperation();
  setBusy(createSubmit, true, 'Creating...');
  setMessage(createMessage, 'Creating link...', 'info');

  try {
    hideNotification();
    showNotification('Creating link...', 'loading');

    const response = await apiRequest('/api/v1/urls', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(payload),
      signal: operation.signal,
    });

    if (!isCurrentOperation(operation.id)) {
      return;
    }

    let data = null;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      data = await response.json();
    } else {
      await response.text();
    }

    hideNotification();

    if (!response.ok) {
      const message = data?.message || data?.error || (await parseApiError(response, 'The short link could not be created.'));
      setMessage(createMessage, message, 'error');
      showNotification(message, 'error');
      return;
    }

    renderShortLinkResult(data);
    setMessage(createMessage, 'Your link is ready.', 'success');
    showNotification('Your link is ready.', 'success');
  } catch (error) {
    if (error?.name === 'AbortError') {
      return;
    }

    const message = 'The service could not be reached.';
    hideNotification();
    setMessage(createMessage, message, 'error');
    showNotification(message, 'error');
  } finally {
    setBusy(createSubmit, false, 'Trim URL');
  }
}

async function handleAnalytics(event) {
  event.preventDefault();

  const shortCode = statsCodeInput.value.trim() || state.currentShortCode;
  if (!shortCode) {
    setMessage(statsMessage, 'Please enter a short code to continue.', 'error');
    statsCodeInput.focus();
    return;
  }

  const operation = beginOperation();
  setBusy(statsSubmit, true, 'Loading...');
  setMessage(statsMessage, 'Loading analytics...', 'info');

  try {
    hideNotification();
    showNotification('Loading analytics...', 'loading');

    const response = await apiRequest(`/api/v1/urls/${encodeURIComponent(shortCode)}/stats`, {
      signal: operation.signal,
    });

    if (!isCurrentOperation(operation.id)) {
      return;
    }

    let data = null;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      data = await response.json();
    } else {
      await response.text();
    }

    hideNotification();

    if (!response.ok) {
      const message = data?.message || data?.error || (await parseApiError(response, 'That short code does not exist.'));
      setMessage(statsMessage, message, 'error');
      hideResult(statsPanel);
      showNotification(message, 'error');
      return;
    }

    renderAnalytics(data);
    setMessage(statsMessage, 'Analytics loaded successfully.', 'success');
    showNotification('Analytics loaded successfully.', 'success');
  } catch (error) {
    if (error?.name === 'AbortError') {
      return;
    }

    const message = 'The service could not be reached.';
    hideNotification();
    setMessage(statsMessage, message, 'error');
    hideResult(statsPanel);
    showNotification(message, 'error');
  } finally {
    setBusy(statsSubmit, false, 'View analytics');
  }
}

async function handleDelete(event) {
  event.preventDefault();

  const shortCode = (deleteCodeInput.value || state.currentShortCode).trim();
  if (!shortCode) {
    setMessage(deleteMessage, 'Please enter a short code to delete.', 'error');
    deleteCodeInput.focus();
    return;
  }

  const confirmed = window.confirm(`Delete the short URL for ${shortCode}?`);
  if (!confirmed) {
    return;
  }

  const operation = beginOperation();
  setBusy(deleteSubmit, true, 'Deleting...');
  setMessage(deleteMessage, 'Deleting link...', 'info');

  try {
    hideNotification();
    showNotification('Deleting link...', 'loading');

    const response = await apiRequest(`/api/v1/urls/${encodeURIComponent(shortCode)}`, {
      method: 'DELETE',
      signal: operation.signal,
    });

    if (!isCurrentOperation(operation.id)) {
      return;
    }

    let data = null;
    const contentType = response.headers.get('content-type') || '';
    if (contentType.includes('application/json')) {
      data = await response.json();
    } else {
      await response.text();
    }

    hideNotification();

    if (!response.ok) {
      const message = data?.message || data?.error || (await parseApiError(response, 'The short link could not be deleted.'));
      setMessage(deleteMessage, message, 'error');
      showNotification(message, 'error');
      return;
    }

    hideResult(resultPanel);
    hideResult(statsPanel);
    if (statsEmpty) {
      statsEmpty.hidden = false;
    }
    deleteCodeInput.value = '';
    statsCodeInput.value = '';
    state.currentShortCode = '';
    state.currentShortUrl = '';
    removeRecentLink(shortCode);
    setMessage(deleteMessage, 'The short link was deleted successfully.', 'success');
    showNotification('The short link was deleted successfully.', 'success');
  } catch (error) {
    if (error?.name === 'AbortError') {
      return;
    }

    const message = 'The service could not be reached.';
    hideNotification();
    setMessage(deleteMessage, message, 'error');
    showNotification(message, 'error');
  } finally {
    setBusy(deleteSubmit, false, 'Delete link');
  }
}

async function copyToClipboard(value, successMessage) {
  try {
    if (navigator.clipboard && window.isSecureContext) {
      await navigator.clipboard.writeText(value);
      showNotification(successMessage, 'success');
      return;
    }

    const helper = document.createElement('textarea');
    helper.value = value;
    helper.setAttribute('readonly', 'true');
    helper.style.position = 'fixed';
    helper.style.left = '-9999px';
    document.body.appendChild(helper);
    helper.select();
    document.execCommand('copy');
    document.body.removeChild(helper);
    showNotification(successMessage, 'success');
  } catch (_error) {
    showNotification('Clipboard access is unavailable in this browser.', 'info');
  }
}

function bindCopyActions() {
  document.getElementById('copy-short-url').addEventListener('click', () => {
    if (!state.currentShortUrl) {
      setMessage(createMessage, 'Create a short link first to copy it.', 'info');
      return;
    }
    copyToClipboard(state.currentShortUrl, 'Short link copied to the clipboard.');
  });

  document.getElementById('open-short-url').addEventListener('click', () => {
    if (!state.currentShortUrl) {
      setMessage(createMessage, 'Create a short link first to open it.', 'info');
      return;
    }
    window.open(state.currentShortUrl, '_blank', 'noopener,noreferrer');
  });

  statsCopyBtn.addEventListener('click', () => {
    if (!state.currentShortUrl) {
      setMessage(statsMessage, 'Load a short code before copying the link.', 'info');
      return;
    }
    copyToClipboard(state.currentShortUrl, 'Short link copied to the clipboard.');
  });

  loadAnalyticsBtn.addEventListener('click', () => {
    if (!state.currentShortCode) {
      setMessage(createMessage, 'Create or load a short code first.', 'info');
      return;
    }
    statsCodeInput.value = state.currentShortCode;
    handleAnalytics({ preventDefault() {} });
  });
}

function renderHealthState(payload) {
  const status = payload && payload.status ? payload.status.toUpperCase() : 'UNKNOWN';
  const isOperational = status === 'UP';
  const details = payload && payload.components ? payload.components : {};

  healthText.textContent = isOperational ? 'System operational' : 'Service unavailable';
  healthSubtext.textContent = isOperational ? 'All core services are responding.' : 'The application is not fully healthy right now.';

  healthBadge.style.background = isOperational ? 'rgba(19, 174, 113, 0.12)' : 'rgba(223, 74, 93, 0.12)';
  healthBadge.style.color = isOperational ? 'var(--success)' : 'var(--error)';

  healthDetails.replaceChildren();

  const components = [
    ['PostgreSQL', details.postgresql],
    ['Redis', details.redis],
  ];

  components.forEach(([name, component]) => {
    if (!component || !component.status) {
      return;
    }

    const detail = document.createElement('div');
    detail.className = 'health-detail';

    const dot = document.createElement('span');
    dot.className = 'dot';
    if (component.status === 'DOWN') {
      dot.classList.add('error');
    } else if (component.status === 'UNKNOWN') {
      dot.classList.add('warning');
    }

    const label = document.createElement('span');
    label.textContent = `${name}: ${component.status}`;

    detail.append(dot, label);
    healthDetails.appendChild(detail);
  });
}

async function checkHealth() {
  try {
    const response = await apiRequest('/actuator/health');
    if (!response.ok) {
      renderHealthState({ status: 'DOWN' });
      return;
    }

    const payload = await response.json();
    renderHealthState(payload);
  } catch (_error) {
    renderHealthState({ status: 'DOWN' });
  }
}

function initializeUI() {
  if (heroButton) {
    heroButton.addEventListener('click', () => {
      const target = document.querySelector(heroButton.dataset.scroll);
      if (target) {
        target.scrollIntoView({ behavior: 'smooth', block: 'start' });
      }
    });
  }

  createForm.addEventListener('submit', handleCreateUrl);
  statsForm.addEventListener('submit', handleAnalytics);
  deleteForm.addEventListener('submit', handleDelete);
  bindCopyActions();

  hideResult(resultPanel);
  hideResult(statsPanel);
  if (statsEmpty) {
    statsEmpty.hidden = false;
  }
  renderRecentLinks();
  setMessage(createMessage, 'Create a short link to begin.', 'info');
  setMessage(statsMessage, 'Paste a short code above to view its performance.', 'info');
  setMessage(deleteMessage, 'Use a short code to delete a URL.', 'info');
  checkHealth();
}

initializeUI();

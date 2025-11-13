import { wireCropDialog } from './crop';
import { confirm, prompt } from 'lib/view';

export function initModule(old: { youtube: string; twitch: string; username: string }): any {
  const userSubmitEl = document.querySelector<HTMLButtonElement>('.approval-request-submit')!,
    streamerEl = document.querySelector<HTMLElement>('.streamer-edit')!,
    youTubeEl = streamerEl.querySelector<HTMLInputElement>('#form3-youTube')!,
    twitchEl = streamerEl.querySelector<HTMLInputElement>('#form3-twitch')!,
    nameEl = streamerEl.querySelector<HTMLInputElement>('#form3-name')!,
    modEl = streamerEl.querySelector<HTMLElement>('div.mod'),
    requestedEl = modEl?.querySelector<HTMLInputElement>('#form3-approval_requested'),
    grantedEl = modEl?.querySelector<HTMLInputElement>('#form3-approval_granted'),
    ignoredEl = modEl?.querySelector<HTMLInputElement>('#form3-approval_ignored'),
    wasSubmitEnabled = setSubmitEnabled(),
    wasRequested = requestedEl?.checked ?? false,
    wasGranted = grantedEl?.checked ?? false;

  if (modEl && requestedEl && grantedEl && ignoredEl) {
    userSubmitEl.classList.add('none');

    // reflect eventual toggle state before SUBMIT instead of after
    ignoredEl.addEventListener('change', () => {
      if (ignoredEl.checked) requestedEl.checked = false;
    });
    grantedEl.addEventListener('change', () => {
      requestedEl.checked = false;
      if (grantedEl.checked) ignoredEl.checked = false;
    });
    modEl.querySelectorAll<HTMLButtonElement>('button[type="submit"]')?.forEach(modAction => {
      if (modAction.value === 'approve') return;
      modAction.addEventListener('click', async e => {
        if (!e.isTrusted || !(e.target instanceof HTMLButtonElement)) return;
        e.preventDefault();
        if (
          (wasRequested || wasGranted) &&
          (modAction.value === 'decline' || !(requestedEl?.checked || grantedEl?.checked))
        ) {
          const reason = await prompt('Reason (optional):');
          if (reason === null) return;
          modEl.querySelector<HTMLInputElement>('input[name="approval.reason"]')!.value = reason;
        }
        e.target.click();
      });
    });
  } else {
    // not a mod
    document
      .querySelectorAll<HTMLInputElement>('.streamer-edit input')
      .forEach(i => i.addEventListener('input', setSubmitEnabled));

    userSubmitEl.addEventListener('click', async e => {
      if (!e.isTrusted) return;
      e.preventDefault();

      if (!old.youtube && !old.twitch) return userSubmitEl.click();
      if (youTubeEl.value === old.youtube && twitchEl.value === old.twitch) return userSubmitEl.click();

      if (await confirm(i18n.streamer.twitchOrYouTubeMustBeVerified)) return userSubmitEl.click();
    });
  }
  wireCropDialog({
    aspectRatio: 1,
    post: { url: '/upload/image/streamer', field: 'picture' },
    max: { pixels: 1000 },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
    onCropped: blob => {
      if (!blob) return;
      const img = streamerEl.querySelector<HTMLImageElement>('img.picture')!;
      img.src = URL.createObjectURL(blob);
      img.onload = () => {
        if (wasSubmitEnabled) return userSubmitEl.click();
        URL.revokeObjectURL(img.src);
        setSubmitEnabled();
      };
    },
  });

  const checkStatusBtn = streamerEl.querySelector<HTMLButtonElement>('#check-stream-status');
  const resultsEl = streamerEl.querySelector<HTMLElement>('.stream-status-results');
  if (checkStatusBtn && resultsEl) {
    // Get username from URL parameter or from init data
    const urlMatch = window.location.search.match(/[?&]u=([^&]+)/);
    const username = urlMatch ? urlMatch[1] : old.username;
    if (username) {
      checkStatusBtn.addEventListener('click', async () => {
        checkStatusBtn.disabled = true;
        checkStatusBtn.textContent = 'Checking...';
        resultsEl.style.display = 'none';
        try {
          const res = await fetch(`/streamer/${username}/checkStatus`);
          if (!res.ok) {
            resultsEl.innerHTML = '';
            const div = document.createElement('div');
            div.className = 'status is-red';
            div.setAttribute('data-icon', 'L');
            if (res.status === 404) {
              div.textContent = 'Streamer not found.';
            } else if (res.status === 503) {
              div.textContent = 'Could not check stream status. The streaming service may be temporarily unavailable.';
            } else {
              div.textContent = `Error: HTTP ${res.status}`;
            }
            resultsEl.appendChild(div);
            resultsEl.style.display = 'block';
            return;
          }
          const data = await res.json();
          resultsEl.innerHTML = '';
          if (data.error) {
            const div = document.createElement('div');
            div.className = 'status is-red';
            div.setAttribute('data-icon', 'L');
            div.textContent = data.error;
            resultsEl.appendChild(div);
          } else {
            const issues = data.issues || [];
            const approved = data.approved;
            if (approved) {
              const div = document.createElement('div');
              div.className = 'status is-green';
              div.setAttribute('data-icon', 'E');
              div.textContent = 'Your stream is approved.';
              resultsEl.appendChild(div);
            }
            if (issues.length > 0) {
              issues.forEach((issue: string) => {
                const div = document.createElement('div');
                div.className = 'status is-red';
                div.setAttribute('data-icon', 'L');
                div.textContent = issue;
                resultsEl.appendChild(div);
              });
            } else if (!approved) {
              const div = document.createElement('div');
              div.className = 'status';
              div.setAttribute('data-icon', 'i');
              div.textContent = 'You are not currently live.';
              resultsEl.appendChild(div);
            }
            if (data.title) {
              const div = document.createElement('div');
              div.className = 'status';
              div.style.marginTop = '1em';
              const strong = document.createElement('strong');
              strong.textContent = 'Current title: ';
              div.appendChild(strong);
              div.appendChild(document.createTextNode(data.title));
              resultsEl.appendChild(div);
            }
            if (data.category) {
              const div = document.createElement('div');
              div.className = 'status';
              const strong = document.createElement('strong');
              strong.textContent = 'Category: ';
              div.appendChild(strong);
              div.appendChild(document.createTextNode(data.category));
              resultsEl.appendChild(div);
            }
          }
          resultsEl.style.display = 'block';
        } catch (e) {
          resultsEl.innerHTML = '';
          const div = document.createElement('div');
          div.className = 'status is-red';
          div.setAttribute('data-icon', 'L');
          div.textContent = 'Error checking status. Please try again.';
          resultsEl.appendChild(div);
          resultsEl.style.display = 'block';
        } finally {
          checkStatusBtn.disabled = false;
          checkStatusBtn.textContent = 'Check my channel status';
        }
      });
    }
  }

  function setSubmitEnabled(): boolean {
    const enabled =
      (youTubeEl.value || twitchEl.value) &&
      nameEl.value &&
      nameEl.value.length >= 3 &&
      !streamerEl.querySelector<HTMLImageElement>('img[src$="images/placeholder.png"]'); // TODO do better
    userSubmitEl.disabled = !enabled;
    userSubmitEl.classList.toggle('disabled', !enabled);
    if (enabled) userSubmitEl.title = '';
    return Boolean(enabled);
  }
}

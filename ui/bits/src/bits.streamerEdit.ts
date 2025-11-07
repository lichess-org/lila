import { wireCropDialog } from './crop';
import { confirm, prompt } from 'lib/view/dialogs';

export function initModule(old: { youtube: string; twitch: string }): any {
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

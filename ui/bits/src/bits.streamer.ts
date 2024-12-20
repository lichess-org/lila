import { wireCropDialog } from './crop';
import { confirm } from 'common/dialog';

export function initModule(old: { youtube?: string; twitch?: string }): any {
  const submit = document.querySelector<HTMLButtonElement>('.approval-request-submit')!;
  const streamerEdit = document.querySelector<HTMLElement>('.streamer-edit')!;
  const youTube = streamerEdit.querySelector<HTMLInputElement>('#form3-youTube')!;
  const twitch = streamerEdit.querySelector<HTMLInputElement>('#form3-twitch')!;
  const name = streamerEdit.querySelector<HTMLInputElement>('#form3-name')!;
  const setSubmitEnabled = (): boolean => {
    const enabled =
      (youTube.value || twitch.value) &&
      name.value &&
      name.value.length >= 3 &&
      !streamerEdit.querySelector<HTMLImageElement>('img[src$="images/placeholder.png"]');
    submit.disabled = !enabled;
    submit.classList.toggle('disabled', !enabled);
    if (enabled) submit.title = ''; // remove "You need an image, streamer name, ..." tooltip
    return Boolean(enabled);
  };

  const wasEnabled = setSubmitEnabled();

  document
    .querySelectorAll<HTMLInputElement>('.streamer-edit input')
    .forEach(i => i.addEventListener('input', setSubmitEnabled));

  submit.addEventListener('click', async e => {
    if (!e.isTrusted) return;
    e.preventDefault();

    if (!old.youtube && !old.twitch) return submit.click();
    if (youTube.value === (old.youtube || '') && twitch.value === (old?.twitch || '')) return submit.click();

    if (await confirm(i18n.streamer.twitchOrYouTubeMustBeVerified, i18n.site.ok, i18n.site.cancel))
      return submit.click();
  });

  wireCropDialog({
    aspectRatio: 1,
    post: { url: '/upload/image/streamer', field: 'picture' },
    max: { pixels: 1000 },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
    onCropped: blob => {
      if (!blob) return;
      const img = streamerEdit.querySelector<HTMLImageElement>('img.picture')!;
      img.src = URL.createObjectURL(blob);
      img.onload = () => {
        if (wasEnabled) return submit.click();
        URL.revokeObjectURL(img.src);
        setSubmitEnabled();
      };
    },
  });
}

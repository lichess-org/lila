import { wireCropDialog } from './crop';
import { prompt, choose, alert } from 'lib/view';
import { scopedQuery, frag } from 'lib';

type Platform = 'youtube' | 'twitch';
type MaybeEl = HTMLElement | null;
type OAuthBox = { div: HTMLElement; linkBtn: MaybeEl; unlinkBtn: MaybeEl; url: HTMLAnchorElement | null };
type OAuthBoxes = { [key in Platform]: OAuthBox };

export function initModule(): any {
  const el = scopedQuery(document.querySelector('.streamer-edit')!);
  const platforms: Platform[] = ['twitch', 'youtube'];
  const oauths = platforms.reduce((acc, platform) => {
    const div = el(`.${platform}-link-box`)!;
    const query = scopedQuery(div);
    acc[platform] = { div, linkBtn: query('.link'), unlinkBtn: query('.unlink'), url: query('a') };
    return acc;
  }, {} as OAuthBoxes);
  const userSubmitBtn = el<HTMLButtonElement>('.approval-request-submit')!;
  const nameInput = el<HTMLInputElement>('#form3-name')!;
  const requestedToggleInput = el<HTMLInputElement>('#form3-approval_requested');
  const grantedToggleInput = el<HTMLInputElement>('#form3-approval_granted');
  const ignoredToggleInput = el<HTMLInputElement>('#form3-approval_ignored');
  const wasSubmitEnabled = setSubmitEnabled();
  if (requestedToggleInput && grantedToggleInput && ignoredToggleInput) {
    userSubmitBtn.classList.add('none');

    // reflect eventual toggle state before SUBMIT instead of after
    ignoredToggleInput.addEventListener('change', () => {
      if (ignoredToggleInput.checked) requestedToggleInput.checked = false;
    });
    grantedToggleInput.addEventListener('change', () => {
      requestedToggleInput.checked = false;
      if (grantedToggleInput.checked) ignoredToggleInput.checked = false;
    });
    el('div.mod')
      ?.querySelectorAll<HTMLButtonElement>('button[type="submit"]')
      ?.forEach(modAction => {
        if (modAction.value === 'approve') return;
        modAction.addEventListener('click', async e => {
          if (!e.isTrusted || !(e.target instanceof HTMLButtonElement)) return;
          e.preventDefault();
          if (
            (requestedToggleInput?.checked || grantedToggleInput?.checked) &&
            (modAction.value === 'decline' || !(requestedToggleInput?.checked || grantedToggleInput?.checked))
          ) {
            const reason = await prompt('Reason (optional):');
            if (reason === null) return;
            el<HTMLInputElement>('input[name="approval.reason"]')!.value = reason;
          }
          e.target.click();
        });
      });
  } else {
    // not a mod
    document
      .querySelectorAll<HTMLInputElement>('.streamer-edit input')
      .forEach(i => i.addEventListener('input', setSubmitEnabled));
  }

  Object.entries(oauths).forEach(([platform, els]: [Platform, OAuthBox]) => {
    if (els.linkBtn) els.linkBtn.onclick = () => clickLink(platform);
    if (els.unlinkBtn) els.unlinkBtn.onclick = () => clickUnlink(platform);
  });

  wireCropDialog({
    aspectRatio: 1,
    post: { url: '/upload/image/streamer', field: 'picture' },
    max: { pixels: 1000 },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
    onCropped: blob => {
      if (!blob) return;
      const img = el<HTMLImageElement>('img.picture')!;
      img.src = URL.createObjectURL(blob);
      img.onload = () => {
        if (wasSubmitEnabled) return userSubmitBtn.click();
        URL.revokeObjectURL(img.src);
        setSubmitEnabled();
      };
    },
  });

  $('.youtube-link-box button')
    .prop('disabled', true)
    .addClass('disabled')
    .attr('title', 'Not yet available, please try again soon!'); // until google validates our token

  window.addEventListener('message', async ev => {
    if (ev.origin !== location.origin || !ev.data || !ev.data.ok) return;
    try {
      const box = oauths[ev.data.platform as Platform];
      const href =
        typeof ev.data.result === 'string' ? ev.data.result : await chooseYoutubeChannel(ev.data.result);
      box.url = frag<HTMLAnchorElement>('<a target="_blank" rel="noopener">');
      box.div.append(box.url);
      box.url.href = href;
      box.url.textContent = ev.data.result.replace(/^https?:\/\//, '');
      box.div.classList.add('linked');
    } catch (e: any) {
      alert(e?.message ?? (typeof e === 'string' ? e : JSON.stringify(e)));
    }
    setSubmitEnabled();
  });

  function clickLink(platform: Platform) {
    const [width, height] = platform === 'twitch' ? [520, 496] : [832, 720];
    window.open(
      oauths[platform].linkBtn!.dataset.href + (site.debug ? '?force_verify=true' : ''),
      platform,
      `popup,width=${Math.min(window.innerWidth, width)},height=${Math.min(window.innerHeight, height)}`,
    );
  }

  async function clickUnlink(platform: Platform) {
    const box = oauths[platform];
    const rsp = await fetch(box.unlinkBtn!.dataset.href!, { method: 'POST' });
    if (!rsp.ok) alert('Failed to unlink. Try reloading the page.');
    box.url?.remove();
    box.div.classList.remove('linked');
    box.url = null;
    setSubmitEnabled();
  }

  async function chooseYoutubeChannel(channels: Record<string, string>): Promise<string> {
    if (Object.values(channels).length === 0) {
      throw $trim`
        Could not find a channel. Did you pick your YouTube brand account on the
        "Sign in with Google" screen?`;
    }
    const ch = await choose(i18n.streamer.chooseYoutubeChannel, Object.values(channels), undefined, true);
    const chid = Object.entries(channels).find(([, name]) => name === ch)![0];
    const rsp = await fetch(`/streamer/oauth/youtube/choose/${chid}`, { method: 'POST' });
    if (!rsp.ok) throw new Error(`Server error: ${rsp.statusText}`);
    return await rsp.text();
  }

  function setSubmitEnabled(): boolean {
    const enabled =
      Object.values(oauths).some(box => box.url) &&
      nameInput.value &&
      nameInput.value.length >= 3 &&
      !el<HTMLImageElement>('img[src$="images/placeholder.png"]'); // TODO do better
    userSubmitBtn.disabled = !enabled;
    userSubmitBtn.classList.toggle('disabled', !enabled);
    if (enabled) userSubmitBtn.title = '';
    return Boolean(enabled);
  }
}

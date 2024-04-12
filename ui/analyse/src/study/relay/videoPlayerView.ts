import RelayCtrl from './relayCtrl';
import { looseH as h, VNode } from 'common/snabbdom';

export let player: VideoPlayer;

export function renderVideoPlayer(relay: RelayCtrl): VNode | undefined {
  if (!relay.data.videoUrls) return undefined;
  player ??= new VideoPlayer(relay);
  return h('div#video-player-placeholder', {
    hook: {
      insert: (vnode: VNode) => player.cover(vnode.elm as HTMLElement),
      update: (_, vnode: VNode) => player.cover(vnode.elm as HTMLElement),
    },
  });
}

class VideoPlayer {
  private iframe: HTMLIFrameElement;
  private close: HTMLImageElement;

  animationFrameId: number;

  constructor(readonly relay: RelayCtrl) {
    this.iframe = document.createElement('iframe');

    this.iframe.id = 'video-player';
    this.iframe.src = relay.data.videoUrls![0];
    this.iframe.setAttribute('credentialless', ''); // a feeble mewling ignored by all
    this.iframe.allow = 'autoplay';
    this.close = document.createElement('img');
    this.close.src = site.asset.flairSrc('symbols.cancel');
    this.close.className = 'video-player-close';
    this.close.addEventListener('click', this.relay.hidePinnedImageAndRemember, true);
  }

  cover(el?: HTMLElement) {
    cancelAnimationFrame(this.animationFrameId);
    const wrap = document.getElementById('main-wrap')!;
    if (!el) {
      if (!wrap.contains(this.iframe)) return;
      wrap.removeChild(this.iframe);
      wrap.removeChild(this.close);
    }
    this.animationFrameId = requestAnimationFrame(() => {
      this.iframe.style.display = 'block';
      this.iframe.style.left = `${el!.offsetLeft}px`;
      this.iframe.style.top = `${el!.offsetTop}px`;
      this.iframe.style.width = `${el!.offsetWidth}px`;
      this.iframe.style.height = `${el!.offsetHeight}px`;
      this.close.style.left = `${el!.offsetLeft + el!.offsetWidth - 16}px`;
      this.close.style.top = `${el!.offsetTop - 4}px`;
      if (wrap.contains(this.iframe)) return;
      wrap.appendChild(this.iframe);
      wrap.appendChild(this.close);
    });
  }
}

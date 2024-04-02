import RelayCtrl from './relayCtrl';
import { looseH as h, Redraw, VNode } from 'common/snabbdom';

let player: VideoPlayer;

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

export function onWindowResize(redraw: Redraw) {
  let showingVideo = false;
  window.addEventListener(
    'resize',
    () => {
      const allow = window.getComputedStyle(document.body).getPropertyValue('--allow-video') === 'true';
      const placeholder = document.getElementById('video-player-placeholder') ?? undefined;
      player?.cover(allow ? placeholder : undefined);
      if (showingVideo === allow && !!placeholder) return;
      showingVideo = allow && !!placeholder;
      redraw();
    },
    { passive: true },
  );
}

class VideoPlayer {
  private iframe: HTMLIFrameElement;
  private close: HTMLImageElement;

  animationFrameId: number;

  constructor(readonly relay: RelayCtrl) {
    this.iframe = document.createElement('iframe');

    this.iframe.id = 'video-player';
    this.iframe.src = relay.data.videoUrls![0];
    this.iframe.allow = 'autoplay';
    this.close = document.createElement('img');
    this.close.src = site.asset.flairSrc('symbols.cancel');
    this.close.className = 'video-player-close';
    this.close.addEventListener('click', this.relay.hidePinnedImageAndRemember, true);
  }

  cover(el?: HTMLElement) {
    cancelAnimationFrame(this.animationFrameId);
    if (!el) {
      if (!document.body.contains(this.iframe)) return;
      document.body.removeChild(this.iframe);
      document.body.removeChild(this.close);
    }
    this.animationFrameId = requestAnimationFrame(() => {
      this.iframe.style.display = 'block';
      this.iframe.style.left = `${el!.offsetLeft}px`;
      this.iframe.style.top = `${el!.offsetTop}px`;
      this.iframe.style.width = `${el!.offsetWidth}px`;
      this.iframe.style.height = `${el!.offsetHeight}px`;
      this.close.style.left = `${el!.offsetLeft + el!.offsetWidth - 16}px`;
      this.close.style.top = `${el!.offsetTop - 4}px`;
      if (document.body.contains(this.iframe)) return;
      document.body.appendChild(this.iframe);
      document.body.appendChild(this.close);
    });
  }
}

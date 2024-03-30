import RelayCtrl from './relayCtrl';
import { looseH as h, Redraw, VNode } from 'common/snabbdom';

let player: VideoPlayer;

export function renderVideoPlayer(relay: RelayCtrl): VNode | undefined {
  if (!relay.data.videoUrls) return undefined;
  player ??= new VideoPlayer(relay.data.videoUrls[0]);
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
      const allow = window.getComputedStyle(document.body).getPropertyValue('---allow-video') === 'true';
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
  animationFrameId: number;

  constructor(embedSrc: string) {
    this.iframe = document.createElement('iframe');

    this.iframe.id = 'video-player';
    this.iframe.src = embedSrc;
    this.iframe.allow = 'autoplay';
  }

  cover(el?: HTMLElement) {
    cancelAnimationFrame(this.animationFrameId);
    if (!el) {
      if (document.body.contains(this.iframe)) document.body.removeChild(this.iframe);
      return;
    }
    this.animationFrameId = requestAnimationFrame(() => {
      this.iframe.style.display = 'block';
      this.iframe.style.left = `${el!.offsetLeft}px`;
      this.iframe.style.top = `${el!.offsetTop}px`;
      this.iframe.style.width = `${el!.offsetWidth}px`;
      this.iframe.style.height = `${el!.offsetHeight}px`;
      if (!document.body.contains(this.iframe)) document.body.appendChild(this.iframe);
    });
  }
}

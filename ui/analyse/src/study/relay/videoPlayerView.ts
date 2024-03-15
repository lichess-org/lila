import RelayCtrl from './relayCtrl';
import { looseH as h, VNode } from 'common/snabbdom';

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

export function resizeVideoPlayer(): void {
  player?.cover(document.getElementById('video-player-placeholder') ?? undefined);
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
    const placement = {
      left: el.offsetLeft,
      top: el.offsetTop,
      width: el.offsetWidth,
      height: el.offsetHeight,
    };
    this.animationFrameId = requestAnimationFrame(() => {
      this.iframe.style.display = 'block';
      this.iframe.style.left = `${placement.left}px`;
      this.iframe.style.top = `${placement.top}px`;
      this.iframe.style.width = `${placement.width}px`;
      this.iframe.style.height = `${placement.height}px`;
      if (!document.body.contains(this.iframe)) document.body.appendChild(this.iframe);
    });
  }
}

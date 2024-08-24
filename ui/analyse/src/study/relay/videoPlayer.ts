import { looseH as h, Redraw, VNode } from 'common/snabbdom';
import RelayCtrl from './relayCtrl';
import { allowVideo } from './relayView';

export class VideoPlayer {
  private iframe: HTMLIFrameElement;
  private close: HTMLImageElement;

  animationFrameId: number;

  constructor(
    private relay: RelayCtrl,
    private url: string,
    private autoplay: boolean,
    private redraw: Redraw,
  ) {
    this.iframe = document.createElement('iframe');

    this.iframe.id = 'video-player';
    this.iframe.setAttribute('credentialless', ''); // a feeble mewling ignored by all
    if (this.autoplay) {
      this.url += '&autoplay=1';
      this.iframe.allow = 'autoplay';
    } else {
      this.url += '&autoplay=false'; // needs to be "false" for twitch
    }
    this.iframe.src = this.url;
    this.iframe.setAttribute('credentialless', 'credentialless');
    this.close = document.createElement('img');
    this.close.src = site.asset.flairSrc('symbols.cancel');
    this.close.className = 'video-player-close';
    this.close.addEventListener('click', this.relay.closeVideoPlayer, true);

    if (relay.data.videoUrls) this.onWindowResize();
  }

  cover = (el?: HTMLElement) => {
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
  };

  onWindowResize = () => {
    let showingVideo = false;
    window.addEventListener(
      'resize',
      () => {
        const allow = allowVideo();
        const placeholder = document.getElementById('video-player-placeholder') ?? undefined;
        this.cover(allow ? placeholder : undefined);
        if (showingVideo === allow && !!placeholder) return;
        showingVideo = allow && !!placeholder;
        this.redraw();
      },
      { passive: true },
    );
  };
}

export function renderVideoPlayer(relay: RelayCtrl): VNode | undefined {
  const player = relay.videoPlayer;
  return player
    ? h('div#video-player-placeholder', {
        hook: {
          insert: (vnode: VNode) => player.cover(vnode.elm as HTMLElement),
          update: (_, vnode: VNode) => player.cover(vnode.elm as HTMLElement),
        },
      })
    : undefined;
}

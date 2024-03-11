import { h, VNode } from 'snabbdom';
import RelayCtrl from './relayCtrl';

let iframeEl: HTMLIFrameElement;

export function videoPlayerView(relay: RelayCtrl): VNode | null {
  const embedSrc = relay.data.videoEmbedSrc;
  if (!embedSrc) return null;
  if (!iframeEl) init(embedSrc);
  if (!document.body.contains(iframeEl)) document.body.appendChild(iframeEl);
  return h('div.video-player-placeholder', {
    hook: {
      insert: (vnode: VNode) => place(vnode.elm as HTMLElement),
      update: (_, vnode: VNode) => place(vnode.elm as HTMLElement),
    },
  });
}

function place(el: HTMLElement | null) {
  iframeEl.style.display = 'block';
  iframeEl.style.left = `${el?.offsetLeft}px`;
  iframeEl.style.top = `${el?.offsetTop}px`;
  iframeEl.style.width = `${el?.offsetWidth}px`;
  iframeEl.style.height = `${el?.offsetHeight}px`;
}

function init(embedSrc: string) {
  iframeEl = document.createElement('iframe');
  iframeEl.id = 'video-player';
  iframeEl.src = embedSrc;
  iframeEl.allow = 'autoplay';
  window.addEventListener('resize', () => place(document.body.querySelector('.video-player-placeholder')), {
    passive: true,
  });
}

import { looseH as h, MaybeVNode, VNode } from 'common/snabbdom';
import AnalyseCtrl from '../ctrl';

let iframeEl: HTMLIFrameElement;

export function streamPlayerView(ctrl: AnalyseCtrl): MaybeVNode {
  const embedSrc = ctrl.study?.relay?.data.streamEmbedUrl;
  if (!embedSrc) return null;
  if (!iframeEl) init(embedSrc);
  if (!document.body.contains(iframeEl)) document.body.appendChild(iframeEl);
  return h('div.stream-player-placeholder', {
    hook: {
      insert: (vnode: VNode) => place(vnode),
      update: (_, vnode: VNode) => place(vnode),
    },
  });
}

function place(vnode: VNode) {
  const el = vnode.elm as HTMLElement;
  iframeEl.style.display = '';
  iframeEl.style.zIndex = '1000';
  iframeEl.style.left = `${el.offsetLeft}px`;
  iframeEl.style.top = `${el.offsetTop}px`;
  iframeEl.style.width = `${el.offsetWidth}px`;
  iframeEl.style.height = `${el.offsetHeight}px`;
  iframeEl.style.border = 'none';
  iframeEl.style.willChange = 'transform';
}

function init(embedSrc: string) {
  iframeEl = document.createElement('iframe');
  iframeEl.src = embedSrc;
  iframeEl.allow = 'autoplay';
  iframeEl.style.position = 'absolute';
  iframeEl.style.display = 'none';
}

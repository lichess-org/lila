import { h, VNode, Hooks, Attrs } from 'snabbdom';
import { fixCrazySan } from 'chess';

export const emptyRedButton = 'button.button.button-red.button-empty';

const longPressDuration = 610; // used in bindMobileTapHold

export function clearSelection() {
  window.getSelection()?.removeAllRanges();
}

export function plyColor(ply: number): Color {
  return ply % 2 === 0 ? 'white' : 'black';
}

export function bindMobileMousedown(el: HTMLElement, f: (e: Event) => any, redraw?: () => void) {
  for (const mousedownEvent of ['touchstart', 'mousedown']) {
    el.addEventListener(mousedownEvent, e => {
      f(e);
      e.preventDefault();
      if (redraw) redraw();
    });
  }
}

export function bindMobileTapHold(el: HTMLElement, f: (e: Event) => any, redraw?: () => void) {
  let longPressCountdown;

  el.addEventListener('touchstart', e => {
    longPressCountdown = setTimeout(() => {
      f(e);
      if (redraw) redraw();
    }, longPressDuration);
  });

  el.addEventListener('touchmove', () => {
    clearTimeout(longPressCountdown);
  });

  el.addEventListener('touchcancel', () => {
    clearTimeout(longPressCountdown);
  });

  el.addEventListener('touchend', () => {
    clearTimeout(longPressCountdown);
  });
}

function listenTo(el: HTMLElement, eventName: string, f: (e: Event) => any, redraw?: () => void) {
  el.addEventListener(eventName, e => {
    const res = f(e);
    if (res === false) e.preventDefault();
    if (redraw) redraw();
    return res;
  });
}

export function bind(eventName: string, f: (e: Event) => any, redraw?: () => void): Hooks {
  return onInsert(el => listenTo(el, eventName, f, redraw));
}

export function bindSubmit(f: (e: Event) => any, redraw?: () => void): Hooks {
  return bind(
    'submit',
    e => {
      e.preventDefault();
      return f(e);
    },
    redraw
  );
}

export function onInsert<A extends HTMLElement>(f: (element: A) => void): Hooks {
  return {
    insert: vnode => f(vnode.elm as A),
  };
}

export function readOnlyProp<A>(value: A): () => A {
  return function (): A {
    return value;
  };
}

export function dataIcon(icon: string): Attrs {
  return {
    'data-icon': icon,
  };
}

export function iconTag(icon: string) {
  return h('i', { attrs: dataIcon(icon) });
}

export function plyToTurn(ply: number): number {
  return Math.floor((ply - 1) / 2) + 1;
}

export function nodeFullName(node: Tree.Node) {
  if (node.san) return plyToTurn(node.ply) + (node.ply % 2 === 1 ? '.' : '...') + ' ' + fixCrazySan(node.san);
  return 'Initial position';
}

export function plural(noun: string, nb: number): string {
  return nb + ' ' + (nb === 1 ? noun : noun + 's');
}

export function titleNameToId(titleName: string): string {
  const split = titleName.split(' ');
  return (split.length === 1 ? split[0] : split[1]).toLowerCase();
}

export function spinner(): VNode {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' },
      }),
    ]),
  ]);
}

export function innerHTML<A>(a: A, toHtml: (a: A) => string): Hooks {
  return {
    insert(vnode) {
      (vnode.elm as HTMLElement).innerHTML = toHtml(a);
      vnode.data!.cachedA = a;
    },
    postpatch(old, vnode) {
      if (old.data!.cachedA !== a) {
        (vnode.elm as HTMLElement).innerHTML = toHtml(a);
      }
      vnode.data!.cachedA = a;
    },
  };
}

export function richHTML(text: string, newLines = true): Hooks {
  return innerHTML(text, t => enrichText(t, newLines));
}

export function baseUrl() {
  return `${window.location.protocol}//${window.location.host}`;
}

export function toYouTubeEmbed(url: string): string | undefined {
  const embedUrl = toYouTubeEmbedUrl(url);
  if (embedUrl)
    return `<div class="embed"><iframe width="100%" src="${embedUrl}" frameborder="0" allow="accelerometer; autoplay; encrypted-media; gyroscope; picture-in-picture" allowfullscreen></iframe></div>`;
  return undefined;
}

function toYouTubeEmbedUrl(url: string) {
  if (!url) return;
  var m = url.match(
    /(?:https?:\/\/)?(?:www\.)?(?:youtube\.com|youtu\.be)\/(?:watch)?(?:\?v=)?([^"&?\/ ]{11})(?:\?|&|)(\S*)/i
  );
  if (!m) return;
  var start = 0;
  m[2].split('&').forEach(function (p) {
    var s = p.split('=');
    if (s[0] === 't' || s[0] === 'start') {
      if (s[1].match(/^\d+$/)) start = parseInt(s[1]);
      else {
        const n = s[1].match(/(?:(\d+)h)?(?:(\d+)m)?(?:(\d+)s)?/)!;
        start = (parseInt(n[1]) || 0) * 3600 + (parseInt(n[2]) || 0) * 60 + (parseInt(n[3]) || 0);
      }
    }
  });
  var params = 'modestbranding=1&rel=0&controls=2&iv_load_policy=3' + (start ? '&start=' + start : '');
  return 'https://www.youtube.com/embed/' + m[1] + '?' + params;
}

export function toTwitchEmbed(url: string): string | undefined {
  const embedUrl = toTwitchEmbedUrl(url);
  if (embedUrl)
    return `<div class="embed"><iframe width="100%" src="${embedUrl}" frameborder=0 allowfullscreen></iframe></div>`;
  return undefined;
}

function toTwitchEmbedUrl(url: string) {
  if (!url) return;
  const m = url.match(/(?:https?:\/\/)?(?:www\.)?(?:twitch.tv)\/([^"&?/ ]+)/i);
  if (m) return `https://player.twitch.tv/?channel=${m[1]}&parent=${location.hostname}&autoplay=false`;
  return undefined;
}

const newLineRegex = /\n/g;

function toLink(url: string) {
  const show = url.replace(/https?:\/\//, '');
  return `<a target="_blank" rel="nofollow noopener noreferrer" href="${url}">${show}</a>`;
}

export function enrichText(text: string, allowNewlines = true): string {
  let html = autolink(lichess.escapeHtml(text), toLink);
  if (allowNewlines) html = html.replace(newLineRegex, '<br>');
  return html;
}

// from https://github.com/bryanwoods/autolink-js/blob/master/autolink.js
const linkRegex = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;

export function autolink(str: string, callback: (str: string) => string): string {
  return str.replace(linkRegex, (_, space, url) => space + callback(url));
}

export function option(value: string, current: string | undefined, name: string) {
  return h(
    'option',
    {
      attrs: {
        value: value,
        selected: value === current,
      },
    },
    name
  );
}

export function scrollTo(el: HTMLElement | undefined, target: HTMLElement | null) {
  if (el && target) el.scrollTop = target.offsetTop - el.offsetHeight / 2 + target.offsetHeight / 2;
}

export function treeReconstruct(parts: any): Tree.Node {
  const root = parts[0],
    nb = parts.length;
  let node = root,
    i: number;
  root.id = '';
  for (i = 1; i < nb; i++) {
    const n = parts[i];
    if (node.children) node.children.unshift(n);
    else node.children = [n];
    node = n;
  }
  node.children = node.children || [];
  return root;
}

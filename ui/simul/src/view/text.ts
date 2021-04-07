import { VNode, Hooks } from 'snabbdom';

export function richHTML(text: string): Hooks {
  return {
    insert(vnode: VNode) {
      (vnode.elm as HTMLElement).innerHTML = toHtml(text);
      vnode.data!.cachedText = text;
    },
    postpatch(old: VNode, vnode: VNode) {
      if (old.data!.cachedText !== text) (vnode.elm as HTMLElement).innerHTML = toHtml(text);
      vnode.data!.cachedText = text;
    },
  };
}

const toHtml = (text: string) => autolink(lichess.escapeHtml(text)).replace(newLineRegex, '<br>');

const linkRegex = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
const newLineRegex = /\n/g;

const autolink = (str: string) => str.replace(linkRegex, (_, space, url) => space + toLink(url));

const toLink = (url: string) =>
  `<a target="_blank" rel="nofollow noopener noreferrer" href="${url}">${url.replace(/https?:\/\//, '')}</a>`;

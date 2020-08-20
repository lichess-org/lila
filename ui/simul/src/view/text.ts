import { VNode } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'

export function richHTML(text: string): Hooks {
  return innerHTML(text, enrichText);
}

function innerHTML<A>(a: A, toHtml: (a: A) => string): Hooks {
  return {
    insert(vnode: VNode) {
      (vnode.elm as HTMLElement).innerHTML = toHtml(a);
      vnode.data!.cachedA = a;
    },
    postpatch(old: VNode, vnode: VNode) {
      if (old.data!.cachedA !== a)
        (vnode.elm as HTMLElement).innerHTML = toHtml(a);

      vnode.data!.cachedA = a;
    }
  };
}

const enrichText = (text: string) =>
  autolink(window.lichess.escapeHtml(text)).replace(newLineRegex, '<br>');

const linkRegex = /(^|[\s\n]|<[A-Za-z]*\/?>)((?:https?|ftp):\/\/[\-A-Z0-9+\u0026\u2019@#\/%?=()~_|!:,.;]*[\-A-Z0-9+\u0026@#\/%=~()_|])/gi;
const newLineRegex = /\n/g;

const autolink = (str: string) =>
  str.replace(linkRegex, (_, space, url) => space + toLink(url));

const toLink = (url: string) =>
  `<a target="_blank" rel="nofollow noopener noreferrer" href="${url}">${url.replace(/https?:\/\//, '')}</a>`;

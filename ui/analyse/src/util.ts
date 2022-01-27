import { h } from 'snabbdom';
import { fixCrazySan } from 'chess';
import { dataIcon } from 'common/snabbdom';

export { autolink, innerHTML, enrichText, richHTML, toYouTubeEmbed, toTwitchEmbed } from 'common/richText';

export const emptyRedButton = 'button.button.button-red.button-empty';

const longPressDuration = 610; // used in bindMobileTapHold

export function plyColor(ply: number): Color {
  return ply % 2 === 0 ? 'white' : 'black';
}

export function bindMobileTapHold(el: HTMLElement, f: (e: Event) => unknown, redraw?: () => void) {
  let longPressCountdown: number;

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

export function readOnlyProp<A>(value: A): () => A {
  return function (): A {
    return value;
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

export function baseUrl() {
  return `${window.location.protocol}//${window.location.host}`;
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

export function treeReconstruct(parts: Tree.Node[]): Tree.Node {
  const root = parts[0],
    nb = parts.length;
  let node = root;
  root.id = '';
  for (let i = 1; i < nb; i++) {
    const n = parts[i];
    if (node.children) node.children.unshift(n);
    else node.children = [n];
    node = n;
  }
  node.children = node.children || [];
  return root;
}

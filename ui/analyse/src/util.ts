import { dataIcon } from 'common/snabbdom';
import { notationsWithColor } from 'shogi/notation';
import { type VNode, h } from 'snabbdom';

export const emptyRedButton = 'button.button.button-red.button-empty';

export function plyColor(ply: number): Color {
  return ply % 2 === 0 ? 'sente' : 'gote';
}

export function readOnlyProp<A>(value: A): () => A {
  return (): A => value;
}

export function iconTag(icon: string): VNode {
  return h('i', { attrs: dataIcon(icon) });
}

export function nodeFullName(node: Tree.Node): VNode {
  if (node.notation)
    return h('span', [
      `${node.ply}. `,
      h(
        `span${notationsWithColor() ? `.color-icon.${node.ply % 2 ? 'sente' : 'gote'}` : ''}`,
        node.notation,
      ),
    ]);
  return h('span', 'Initial position');
}

export function plural(noun: string, nb: number): string {
  return `${nb} ${nb === 1 ? noun : `${noun}s`}`;
}

export function titleNameToId(titleName: string): string {
  const split = titleName.split(' ');
  return (split.length === 1 ? split[0] : split[1]).toLowerCase();
}

export function baseUrl() {
  return `${window.location.protocol}//${window.location.host}`;
}

export function option(value: string, current: string | undefined, name: string): VNode {
  return h(
    'option',
    {
      attrs: {
        value: value,
        selected: value === current,
      },
    },
    name,
  );
}

export function scrollTo(el: HTMLElement | undefined, target: HTMLElement | null): void {
  if (el && target) el.scrollTop = target.offsetTop - el.offsetHeight / 2 + target.offsetHeight / 2;
}

import { fixCrazySan, plyToTurn } from 'chess';
import { attributesModule, classModule, eventListenersModule, init, h, VNodeData } from 'snabbdom';

export const patch = init([classModule, attributesModule, eventListenersModule]);

export const emptyRedButton = 'button.button.button-red.button-empty';

export const baseUrl = () => `${window.location.protocol}//${window.location.host}`;

export function nodeFullName(node: Tree.Node) {
  if (node.san) return plyToTurn(node.ply) + (node.ply % 2 === 1 ? '.' : '...') + ' ' + fixCrazySan(node.san);
  return 'Initial position';
}

export const plural = (noun: string, nb: number): string => nb + ' ' + (nb === 1 ? noun : noun + 's');

export function titleNameToId(titleName: string): string {
  const split = titleName.split(' ');
  return (split.length === 1 ? split[0] : split[1]).toLowerCase();
}

export const option = (value: string, current: string | undefined, name: string, data?: VNodeData) =>
  h('option', { attrs: { value: value, selected: value === current }, ...data }, name);

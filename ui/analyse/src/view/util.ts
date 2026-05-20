import {
  attributesModule,
  classModule,
  propsModule,
  eventListenersModule,
  init,
  type VNodeData,
} from 'snabbdom';

import { fixCrazySan, plyToTurn } from 'lib/game/chess';
import type { TreeNode } from 'lib/tree/types';
import { hl } from 'lib/view';

import type { Federation } from '@/study/interfaces';

export const patch = init([classModule, attributesModule, propsModule, eventListenersModule]);

export const emptyRedButton = 'button.button.button-red.button-empty';

export const baseUrl = () => `${window.location.protocol}//${window.location.host}`;

export const nodeFullName = (node: TreeNode): string =>
  node.san
    ? plyToTurn(node.ply) + (node.ply % 2 === 1 ? '.' : '...') + ' ' + fixCrazySan(node.san)
    : 'Initial position';

export const plural = (noun: string, nb: number): string => nb + ' ' + (nb === 1 ? noun : noun + 's');

export function titleNameToId(titleName: string): string {
  const split = titleName.split(' ');
  return (split.length === 1 ? split[0] : split[1]).toLowerCase();
}

export const option = (value: string, current: string | undefined, name: string, data?: VNodeData) =>
  hl('option', { attrs: { value: value, selected: value === current }, ...data }, name);

export const playerFedFlag = (fed?: Federation) =>
  fed &&
  hl('img.mini-game__flag', {
    attrs: {
      src: site.asset.fideFedSrc(fed.id),
      title: `Federation: ${fed.i18nName}`,
    },
  });

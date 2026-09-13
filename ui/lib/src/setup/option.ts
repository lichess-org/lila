import { h, type VNode } from 'snabbdom';

export const option = ({ key, name }: { key: string; name: string }, selectedKey: string): VNode =>
  h('option', { attrs: { value: key, selected: key === selectedKey } }, name);

export const aiLevels: number[] = [1, 2, 3, 4, 5, 6, 7, 8];

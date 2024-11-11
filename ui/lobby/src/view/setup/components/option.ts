import { h, type VNode } from 'snabbdom';

export const option = ({ key, name }: { key: string; name: string }, selectedKey: string): VNode =>
  h('option', { attrs: { value: key, selected: key === selectedKey } }, name);

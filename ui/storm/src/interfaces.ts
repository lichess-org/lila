import { VNode } from 'snabbdom/vnode'

export type MaybeVNode = VNode | string | null | undefined;
export type MaybeVNodes = MaybeVNode[];
export type Redraw = () => void;

export interface StormOpts {
  data: StormData;
  i18n: any;
}

export interface StormData {
  puzzles: StormPuzzle[];
}

export interface StormPuzzle {
  id: string;
  fen: string;
  line: string;
}

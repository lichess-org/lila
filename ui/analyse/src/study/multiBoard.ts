import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Prop } from 'common';
import { StudyChapterMeta } from './interfaces';

export class MultiBoardCtrl {

  open: boolean = false;
  toggle = () => this.open = !this.open;

  constructor(readonly chapters: Prop<StudyChapterMeta[]>) {
  }
}

export function view(ctrl: MultiBoardCtrl): VNode | undefined {

  if (!ctrl.open) return;

  return h('div.multiboard', 'multiboard!');
}

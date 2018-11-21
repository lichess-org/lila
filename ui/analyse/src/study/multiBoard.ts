import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Prop } from 'common';
import { StudyChapterMeta } from './interfaces';

export class MultiBoardCtrl {

  constructor(readonly chapters: Prop<StudyChapterMeta[]>) {
  }
}

export function view(ctrl: MultiBoardCtrl): VNode | undefined {

  console.log(ctrl.chapters());

  return h('div.multiboard', 'multiboard!');
}

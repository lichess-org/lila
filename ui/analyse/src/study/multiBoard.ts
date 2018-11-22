import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { ChapterPreview } from './interfaces';
import { multiBoard as xhrLoad } from './studyXhr';

interface MultiBoardData {
  previews: [ChapterPreview]
}

export class MultiBoardCtrl {

  data?: MultiBoardData;

  constructor(readonly studyId: string, readonly redraw: () => void) {
  }

  getData() {
    if (!this.data) xhrLoad(this.studyId).then(d => {
      this.data = d;
      this.redraw();
    });
    return this.data;
  };
}

export function view(ctrl: MultiBoardCtrl): VNode | undefined {

  const data = ctrl.getData();

  if (!data) 
  console.log(data.previews);

  return h('div.multi_board', 'multiboard!');
}

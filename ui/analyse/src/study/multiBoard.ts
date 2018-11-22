import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Draughtsground } from 'draughtsground';
import { ChapterPreview } from './interfaces';
import { multiBoard as xhrLoad } from './studyXhr';
import { spinner } from '../util';

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
  if (!data) return h('div.multi_board', spinner());

  return h('div.multi_board',
    data.previews.map(preview => {
      return h('a.mini_board', [
        makeCg(preview)
      ])
    })
  );
}

function makeCg(preview: ChapterPreview) {
  return h('div.cg-board-wrap', {
    hook: {
      insert(vnode) {
        const lm = preview.lastMove;
        Draughtsground(vnode.elm as HTMLElement, {
          coordinates: false,
          drawable: { enabled: false, visible: false },
          resizable: false,
          viewOnly: true,
          orientation: preview.orientation,
          fen: preview.fen,
          lastMove: lm && ([lm[0] + lm[1], lm[2] + lm[3]] as Key[])
        });
      }
    }
  }, [ h('div.cg-board') ])
}

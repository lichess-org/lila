import { EditorConfig } from './interfaces';
import EditorCtrl from './ctrl';
import view from './view';

import { init } from 'snabbdom';
import { VNode } from 'snabbdom/vnode';
import klass from 'snabbdom/modules/class';
import attributes from 'snabbdom/modules/attributes';
import eventlisteners from 'snabbdom/modules/eventlisteners';

import { menuHover } from 'common/menuHover';
import { Chessground } from 'chessground';

menuHover();

const patch = init([klass, attributes, eventlisteners]);

export default function(element, config: EditorConfig) {
  let vnode: VNode, ctrl: EditorCtrl;

  const redraw = () => {
    vnode = patch(vnode, view(ctrl));
  };

  ctrl = new EditorCtrl(config, redraw);
  vnode = patch(element, view(ctrl));

  return {
    getFen: ctrl.computeFen.bind(ctrl),
    setOrientation: ctrl.setOrientation.bind(ctrl)
  };
};

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;

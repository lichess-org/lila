import { init } from 'snabbdom';
import makeCtrl from './woodpeckerCtrl';
import view from './view/woodpecker';
import { PuzzleOpts } from './interfaces';

export interface WoodpeckerOpts extends PuzzleOpts {
  puzzleCount: number;
}

export default function (opts: WoodpeckerOpts) {
  const ctrl = makeCtrl(opts);
  const element = document.querySelector('.puzzle__board') as HTMLElement;
  const patch = init([
    require('snabbdom/modules/class').default,
    require('snabbdom/modules/attributes').default,
  ]);
  
  let vnode = patch(element, view(ctrl));
  
  ctrl.redraw = () => {
    vnode = patch(vnode, view(ctrl));
  };
  
  return ctrl;
}
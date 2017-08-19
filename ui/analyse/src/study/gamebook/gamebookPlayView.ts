import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import GamebookPlayCtrl from './gamebookPlayCtrl';
// import AnalyseCtrl from '../../ctrl';
import { bind, dataIcon, innerHTML } from '../../util';
import { enrichText } from '../studyComments';
// import { MaybeVNodes } from '../../interfaces';
// import { throttle } from 'common';

export function render(ctrl: GamebookPlayCtrl): VNode {

  const root = ctrl.root,
  node: Tree.Node = root.node,
  gb: Tree.Gamebook = node.gamebook || {},
  isMyMove = root.turnColor() === root.data.orientation,
  comment = (root.node.comments || [])[0];

  return h('div.gamebook', {
    hook: { insert: _ => window.lichess.loadCss('/assets/stylesheets/gamebook.play.css') }
  }, [
    h('div.comment', h('div.content', {
      hook: comment && innerHTML(comment.text, text => enrichText(text, true))
    })),
    h('img.mascot', {
      attrs: {
        width: 120,
        height: 120,
        src: window.lichess.assetUrl(`/assets/images/mascot/${ctrl.mascot.current}.svg`),
        title: 'Click to choose your teacher'
      },
      hook: bind('click', ctrl.mascot.switch, ctrl.redraw)
    }),
    h('div.soapbox', [
      h('div.turn', isMyMove ? 'Your turn' : 'Opponent turn')
    ])
  ]);
}

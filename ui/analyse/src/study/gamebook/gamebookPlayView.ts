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
    h('div.comment', {
      hook: comment && innerHTML(comment.text, text => enrichText(text, true))
    }),
    h('div.say'),
    h('img.mascot', {
      attrs: {
        width: 120,
        height: 120,
        src: window.lichess.assetUrl(`/assets/images/mascot/${ctrl.mascot}.svg`),
        title: 'Click to choose your teacher'
      },
      hook: bind('click', ctrl.switchMascot)
    }),
    h('div.act', [
      gb.hint ? h('a.hint', [
        h('i', { attrs: dataIcon('') }),
        'Get a hint'
      ]) : h('span.hint'),
      h('a.solution', [
        h('i', { attrs: dataIcon('G') }),
        'View the solution'
      ])
    ]),
    h('span.turn', isMyMove ? 'Your turn' : 'Opponent turn')
  ]);
}

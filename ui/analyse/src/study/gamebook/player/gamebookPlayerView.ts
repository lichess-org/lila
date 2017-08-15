import { h } from 'snabbdom'
import AnalyseController from '../../../ctrl';
import { innerHTML } from '../../../util';
import { enrichText } from '../../studyComments';
// import { MaybeVNodes } from '../../../interfaces';
import { VNode } from 'snabbdom/vnode'
// import { throttle } from 'common';

export default function(ctrl: AnalyseController): VNode {

  const isMyMove = ctrl.turnColor() === ctrl.data.orientation,
  comment = (ctrl.node.comments || [])[0];

  return h('div.gamebook', {
    hook: {
      insert: _ => window.lichess.loadCss('/assets/stylesheets/gamebook.player.css')
    }
  }, [
    h('div.player', [
      h('div.turn', isMyMove ? 'Your move' : 'Opponent move'),
      h('div.comment', {
        hook: comment && innerHTML(comment.text, text => enrichText(text, true))
      })
    ])
  ]);
}

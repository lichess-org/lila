import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import GamebookPlayCtrl from './gamebookPlayCtrl';
// import AnalyseCtrl from '../../ctrl';
import { bind, dataIcon, innerHTML } from '../../util';
import { enrichText } from '../studyComments';
import { State } from './gamebookPlayCtrl';
// import { MaybeVNodes } from '../../interfaces';
// import { throttle } from 'common';

const defaultComments = {
  play: 'What would you play in this position?',
  good: 'Yes indeed, good move!',
  bad: 'This is not the move you are looking for.',
  end: 'And that is all she wrote.'
};

export function render(ctrl: GamebookPlayCtrl): VNode {

  const root = ctrl.root,
  state = ctrl.state;
  // state.feedback = 'bad';

  const comment = state.comment || defaultComments[state.feedback],
  isMyMove = ['good', 'bad'].indexOf(state.feedback) > -1;

  return h('div.gamebook', {
    hook: { insert: _ => window.lichess.loadCss('/assets/stylesheets/gamebook.play.css') }
  }, [
    h('div.comment', h('div.content', {
      hook: innerHTML(comment, text => enrichText(text, true))
    })),
    h('img.mascot', {
      attrs: {
        width: 120,
        height: 120,
        src: ctrl.mascot.url(),
        title: 'Click to choose your teacher'
      },
      hook: bind('click', ctrl.mascot.switch, ctrl.redraw)
    }),
    renderFeedback(ctrl, state)
  ]);
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback;
  if (fb === 'bad') return h('div.feedback.act.bad', {
    hook: bind('click', ctrl.retry, ctrl.redraw)
  }, [
    h('i', { attrs: dataIcon('P') }),
    h('span', 'Retry')
  ]);
  if (fb === 'good') return h('div.feedback.act.good', {
    hook: bind('click', ctrl.next, ctrl.redraw)
  }, [
    h('i', { attrs: dataIcon('G') }),
    h('span', 'Next')
  ]);
  if (fb === 'end') return h('div.feedback.end', [
    h('i', { attrs: dataIcon('E') }),
    h('span', 'Gamebook complete')
  ]);
  return h('div.feedback.' + fb,
    h('span', fb === 'play' ? 'Your turn' : 'Opponent turn')
  );
}

import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'
import { Hooks } from 'snabbdom/hooks'
import GamebookPlayCtrl from './gamebookPlayCtrl';
import { bind, dataIcon, iconTag, enrichText, innerHTML } from '../../util';
import { State } from './gamebookPlayCtrl';

const defaultComments = {
  play: 'whatWouldYouPlay',
  end: 'lessonCompleted'
};

export function render(ctrl: GamebookPlayCtrl): VNode {

  const state = ctrl.state,
    comment = state.comment || ctrl.root.trans.noarg(defaultComments[state.feedback]);

  return h('div.gamebook', {
    hook: { insert: _ => window.lidraughts.loadCss('/assets/stylesheets/gamebook.play.css', ctrl.root.embed ? { sameDomain: true, noVersion: true } : {}) }
  }, [
    comment ? h('div.comment', {
      class: { hinted: state.showHint }
    }, [
      h('div.content', { hook: richHTML(comment) }),
      hintZone(ctrl)
    ]) : undefined,
    h('div.floor', [
      renderFeedback(ctrl, state),
      !ctrl.root.embed ? h('img.mascot', {
        attrs: {
          width: 120,
          height: 120,
          src: window.lidraughts.assetUrl('/assets/images/mascot/octopus.svg')
        }
      }) : null
    ])
  ]);
}

function hintZone(ctrl: GamebookPlayCtrl) {
  const state = ctrl.state,
  clickHook = () => ({
    hook: bind('click', ctrl.hint, ctrl.redraw)
  });
  if (state.showHint) return h('div', clickHook(), [
    h('div.hint', { hook: richHTML(state.hint!) })
  ]);
  if (state.hint) return h('a.hint', clickHook(), ctrl.root.trans.noarg('getAHint'));
}

function renderFeedback(ctrl: GamebookPlayCtrl, state: State) {
  const fb = state.feedback;
  if (fb === 'bad') return h('div.feedback.act.bad' + (state.comment ? '.com' : ''), {
    hook: bind('click', ctrl.retry)
  }, [
    iconTag('P'),
    h('span', ctrl.root.trans.noarg('retry'))
  ]);
  if (fb === 'good' && state.comment) return h('div.feedback.act.good.com', {
    hook: bind('click', ctrl.next)
  }, [
    h('span.text', { attrs: dataIcon('G') }, ctrl.root.trans.noarg('next')),
    h('kbd', '<space>')
  ]);
  if (fb === 'end') return renderEnd(ctrl);
  return h('div.feedback.info.' + fb + (state.init ? '.init' : ''),
    h('span', fb === 'play' ? ctrl.root.trans.noarg('yourTurn') : ctrl.root.trans.noarg('goodMove'))
  );
}

function renderEnd(ctrl: GamebookPlayCtrl) {
  const study = ctrl.root.study!,
  nextChapter = !ctrl.root.embed && study.nextChapter();
  return h('div.feedback.end', [
    nextChapter ? h('a.next.text', {
      attrs: dataIcon('G'),
      hook: bind('click', () => study.setChapter(nextChapter.id))
    }, ctrl.root.trans.noarg('nextChapter')) : undefined,
    h('a.retry', {
      attrs: dataIcon('P'),
      hook: bind('click', () => ctrl.root.userJump(''), ctrl.redraw)
    }, ctrl.root.trans.noarg('playAgain')),
    !ctrl.root.embed ? h('a.analyse', {
      attrs: dataIcon('A'),
      hook: bind('click', () => study.setGamebookOverride('analyse'), ctrl.redraw)
    }, ctrl.root.trans.noarg('analyse')) : null
  ]);
}

function richHTML(text: string): Hooks {
  return innerHTML(text, text => enrichText(text, true));
}

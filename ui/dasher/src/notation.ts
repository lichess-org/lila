import { Notation } from 'common/notation';
import { VNode, h } from 'snabbdom';
import { Close, Redraw, bind, header } from './util';

export interface NotationData {
  current: Notation;
  list: Notation[];
}

export interface NotationCtrl {
  set(n: Notation): void;
  data: NotationData;
  redraw: Redraw;
  trans: Trans;
  close: Close;
}

export function ctrl(data: NotationData, trans: Trans, redraw: Redraw, close: Close): NotationCtrl {
  return {
    set(n: Notation) {
      data.current = n;
      $.post('/pref/notation', { notation: n }, () => {
        // we need to reload the page to see changes
        window.lishogi.reload();
      }).fail(() => window.lishogi.announce({ msg: 'Failed to save notation preference' }));
      redraw();
    },
    data,
    redraw,
    trans,
    close,
  };
}

export function view(ctrl: NotationCtrl): VNode {
  return h('div.sub.notation.', [
    header(ctrl.trans('notationSystem'), ctrl.close),
    h('div.content', [h('div.selector', ctrl.data.list.map(notationView(ctrl, ctrl.data.current)))]),
  ]);
}

function notationView(ctrl: NotationCtrl, current: Notation) {
  return (n: Notation) =>
    h(
      'a.text',
      {
        hook: bind('click', () => ctrl.set(n)),
        class: { active: current === n },
        attrs: { 'data-icon': 'E' },
      },
      notationDisplay(ctrl, n)
    );
}

function notationDisplay(ctrl: NotationCtrl, notation: Notation): string {
  switch (notation) {
    case Notation.Western:
      return ctrl.trans('westernNotation') + ' (76)';
    case Notation.WesternEngine:
      return ctrl.trans('westernNotation') + ' (7f)';
    case Notation.Japanese:
      return ctrl.trans('japaneseNotation');
    case Notation.Kawasaki:
      return ctrl.trans('kitaoKawasakiNotation');
  }
}

import { Notation } from 'shogi/notation';
import { VNode, h } from 'snabbdom';
import { Close, Redraw, bind, header } from './util';
import { i18n } from 'i18n';

export interface NotationData {
  current: Notation;
  list: Notation[];
}

export interface NotationCtrl {
  set(n: Notation): void;
  data: NotationData;
  redraw: Redraw;
  close: Close;
}

export function ctrl(data: NotationData, redraw: Redraw, close: Close): NotationCtrl {
  return {
    set(n: Notation) {
      data.current = n;
      window.lishogi.xhr
        .text('POST', '/pref/notation', { formData: { notation: n } })
        .then(window.lishogi.reload, () =>
          window.lishogi.announce({ msg: 'Failed to save notation preference' }),
        );
      redraw();
    },
    data,
    redraw,
    close,
  };
}

export function view(ctrl: NotationCtrl): VNode {
  return h('div.sub.notation.', [
    header(i18n('notationSystem'), ctrl.close),
    h('div.content', [
      h('div.selector', ctrl.data.list.map(notationView(ctrl, ctrl.data.current))),
    ]),
  ]);
}

function notationView(ctrl: NotationCtrl, current: Notation) {
  return (n: Notation) =>
    h(
      'a.text',
      {
        hook: bind('click', () => ctrl.set(n)),
        class: { active: current === n },
        attrs: {
          title: notationExample(n),
          'data-icon': 'E',
        },
      },
      notationDisplay(n),
    );
}

function notationExample(notation: Notation): string {
  switch (notation) {
    case Notation.Western:
      return 'P-76';
    case Notation.WesternEngine:
      return 'P-7f';
    case Notation.Japanese:
      return '７六歩';
    case Notation.Kawasaki:
      return '歩-76';
    case Notation.Kif:
      return '７六歩(77)';
    case Notation.Usi:
      return '7g7f';
    case Notation.Yorozuya:
      return '７六歩';
  }
}

function notationDisplay(notation: Notation): string {
  switch (notation) {
    case Notation.Western:
      return i18n('preferences:westernNotation') + ' (76)';
    case Notation.WesternEngine:
      return i18n('preferences:westernNotation') + ' (7f)';
    case Notation.Japanese:
      return i18n('preferences:japaneseNotation');
    case Notation.Kawasaki:
      return i18n('preferences:kitaoKawasakiNotation');
    case Notation.Kif:
      return i18n('preferences:kifNotation');
    case Notation.Usi:
      return 'USI';
    case Notation.Yorozuya:
      return i18n('preferences:yorozuyaNotation');
  }
}

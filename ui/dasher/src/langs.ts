import { h, VNode } from 'snabbdom';

import { Close, header } from './util';

type Code = string;

export interface Lang {
  0: Code;
  1: string;
}

export interface LangsData {
  current: Code;
  accepted: Code[];
  list: Lang[];
}

export interface LangsCtrl {
  list(): Lang[];
  current: Code;
  accepted: Set<Code>;
  trans: Trans;
  close: Close;
}

export function ctrl(data: LangsData, trans: Trans, close: Close): LangsCtrl {
  const accepted = new Set(data.accepted);
  return {
    list() {
      return [...data.list.filter(lang => accepted.has(lang[0])), ...data.list];
    },
    current: data.current,
    accepted,
    trans,
    close,
  };
}

export function view(ctrl: LangsCtrl): VNode {
  return h('div.sub.langs', [
    header(ctrl.trans.noarg('language'), ctrl.close),
    h(
      'form',
      {
        attrs: { method: 'post', action: '/translation/select' },
      },
      ctrl.list().map(langView(ctrl.current, ctrl.accepted))
    ),
    h(
      'a.help.text',
      {
        attrs: {
          href: 'https://crowdin.com/project/lichess',
          'data-icon': '',
        },
      },
      'Help translate Lichess'
    ),
  ]);
}

function langView(current: Code, accepted: Set<Code>) {
  return (l: Lang) =>
    h(
      'button' + (current === l[0] ? '.current' : '') + (accepted.has(l[0]) ? '.accepted' : ''),
      {
        attrs: {
          type: 'submit',
          name: 'lang',
          value: l[0],
          title: l[0],
        },
      },
      l[1]
    );
}

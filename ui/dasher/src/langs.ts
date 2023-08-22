import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { Close, header } from './util';

type Code = string;
type Name = string;

export type Lang = [Code, Name];

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
      ctrl.list().map(langView(ctrl.current, ctrl.accepted)),
    ),
    h(
      'a.help.text',
      {
        attrs: {
          href: 'https://crowdin.com/project/lichess',
          'data-icon': licon.Heart,
        },
      },
      'Help translate Lichess',
    ),
  ]);
}

const langView =
  (current: Code, accepted: Set<Code>) =>
  ([code, name]: Lang) =>
    h(
      'button' + (current === code ? '.current' : '') + (accepted.has(code) ? '.accepted' : ''),
      {
        attrs: {
          type: 'submit',
          name: 'lang',
          value: code,
          title: code,
        },
      },
      name,
    );

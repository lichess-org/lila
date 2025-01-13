import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import { type Close, header } from './util';

type Code = string;

interface Lang {
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
  close: Close;
}

export function ctrl(data: LangsData, close: Close): LangsCtrl {
  const accepted = new Set(data.accepted);
  return {
    list() {
      return [
        ...data.list.filter(lang => accepted.has(lang[0])),
        ...data.list.filter(lang => !accepted.has(lang[0])),
      ];
    },
    current: data.current,
    accepted,
    close,
  };
}

export function view(ctrl: LangsCtrl): VNode {
  return h('div.sub.langs', [
    header(i18n('language'), ctrl.close),
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
          href: 'https://crowdin.com/project/lishogi',
          'data-icon': '',
        },
      },
      'Help translate Lishogi',
    ),
  ]);
}

function langView(current: Code, accepted: Set<Code>) {
  return (l: Lang) =>
    h(
      `button${current === l[0] ? '.current' : ''}${accepted.has(l[0]) ? '.accepted' : ''}`,
      {
        attrs: {
          type: 'submit',
          name: 'lang',
          value: l[0],
          title: l[0],
        },
      },
      l[1],
    );
}

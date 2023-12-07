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

export class LangsCtrl {
  accepted: Set<Code>;
  constructor(
    readonly data: LangsData,
    readonly trans: Trans,
    readonly close: Close,
  ) {
    this.accepted = new Set(data.accepted);
  }
  list = () => [...this.data.list.filter(lang => this.accepted.has(lang[0])), ...this.data.list];
}

export const view = (ctrl: LangsCtrl): VNode =>
  h('div.sub.langs', [
    header(ctrl.trans.noarg('language'), ctrl.close),
    h(
      'form',
      {
        attrs: { method: 'post', action: '/translation/select' },
      },
      ctrl.list().map(langView(ctrl.data.current, ctrl.accepted)),
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

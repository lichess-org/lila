import { h, type VNode } from 'snabbdom';

import { licon } from 'lib/licon';

import { PaneCtrl } from './interfaces';
import { header } from './util';

type Code = string;
type Name = string;

export type Lang = [Code, Name];

export interface LangsData {
  current: Code;
  accepted: Code[];
  list: Lang[];
}

export class LangsCtrl extends PaneCtrl {
  render = (): VNode =>
    h('div.sub.langs', [
      header(i18n.site.language, this.close),
      h(
        'form',
        { attrs: { method: 'post', action: '/translation/select' } },
        this.list().map(([code, name]: Lang) =>
          h(
            'button',
            {
              class: {
                current: this.isCurrent(code),
                accepted: this.isAccepted(code),
              },
              attrs: { type: 'submit', name: 'lang', value: code, title: code },
            },
            name,
          ),
        ),
      ),
      h(
        'a.help.text',
        { attrs: { href: 'https://crowdin.com/project/lichess', 'data-icon': licon.Heart } },
        'Help translate Lichess',
      ),
    ]);

  private get data() {
    return this.root.data.lang;
  }

  private readonly isCurrent = (code: Code) => this.data.current === code;
  private readonly isAccepted = (code: Code) => this.data.accepted.includes(code);

  private readonly list = () => [
    ...this.data.list.filter(([code, _]) => this.isCurrent(code) || this.isAccepted(code)),
    ...this.data.list,
  ];
}

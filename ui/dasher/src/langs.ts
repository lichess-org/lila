import { h, type VNode } from 'snabbdom';

import * as licon from 'lib/licon';
import { onInsert } from 'lib/view';

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
            'button' +
              (this.data.current === code ? '.current' : '') +
              (this.data.accepted.includes(code) ? '.accepted' : ''),
            {
              attrs: { type: 'submit', name: 'lang', value: code, title: code },
              hook: this.data.current === code ? onInsert(el => el.scrollIntoView({ block: 'center' })) : {},
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

  private readonly list = () => [
    ...this.data.list.filter(lang => this.data.accepted.includes(lang[0])),
    ...this.data.list,
  ];
}

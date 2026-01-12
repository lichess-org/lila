import { h, type VNode } from 'snabbdom';
import * as licon from 'lib/licon';
import { header } from './util';
import { type DasherCtrl, PaneCtrl } from './interfaces';

type Code = string;
type Name = string;

export type Lang = [Code, Name];

export interface LangsData {
  current: Code;
  accepted: Code[];
  list: Lang[];
}

export class LangsCtrl extends PaneCtrl {
  constructor(root: DasherCtrl) {
    super(root);
  }

  render = (): VNode =>
    h('div.sub.langs', [
      header(i18n.site.language, this.close),
      h(
        'form',
        {
          attrs: { method: 'post', action: '/translation/select' },
          hook: { insert: vnode => this.autoScrollToCurrent(vnode.elm as HTMLElement) },
        },
        this.list().map(([code, name]: Lang) =>
          h(
            'button' +
              (this.data.current === code ? '.current' : '') +
              (this.data.accepted.includes(code) ? '.accepted' : ''),
            { attrs: { type: 'submit', name: 'lang', value: code, title: code } },
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

  private list = () => [
    ...this.data.list.filter(lang => this.data.accepted.includes(lang[0])),
    ...this.data.list,
  ];

  private autoScrollToCurrent(form: HTMLElement): void {
    const current = form.querySelector('button.current') as HTMLElement | null;
    if (current && !this.visible(current, form))
      form.scrollTop = Math.max(0, current.offsetTop - form.clientHeight / 2 + current.offsetHeight / 2);
  }

  private visible(button: HTMLElement, form: HTMLElement): boolean {
    const top = button.offsetTop,
      viewTop = form.scrollTop;
    return top >= viewTop && top + button.offsetHeight <= viewTop + form.clientHeight;
  }
}

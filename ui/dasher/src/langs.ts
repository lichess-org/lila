import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, Prop, prop, spinner, bind } from './util'
import { get } from './xhr'

export interface Lang {
  0: Code,
  1: string
}

type Code = string;

export interface LangsCtrl {
  data: Prop<Lang[] | undefined>
  current: Code,
  accepted: Code[],
  load(): void
  close: Close
}

export function ctrl(current: Code, accepted: Code[], redraw: Redraw, close: Close): LangsCtrl {

  const data: Prop<Lang[] | undefined> = prop(undefined);

  return {
    data,
    current,
    accepted,
    load() {
      get(window.lichess.assetUrl('/assets/trans/refs.json'), true).then(d => {
        data(d);
        redraw();
      });
    },
    close
  };
}

export function view(ctrl: LangsCtrl): VNode {

  const d = ctrl.data();

  if (!d) {
    ctrl.load();
    return spinner();
  }

  return h('div.sub.langs', [
    h('a.head.text', {
      attrs: { 'data-icon': 'I' },
      hook: bind('click', ctrl.close)
    }, 'Language'),
    h('form', {
      attrs: { method: 'post', action: '/translation/select' }
    }, [
      h('ul', d.map(langView(ctrl.current, ctrl.accepted)))
    ])
  ]);
}

function langView(current: Code, accepted: Code[]) {
  return (l: Lang) => h('li', [
    h('button', {
      attrs: {
        type: 'submit',
        name: 'lang',
        value: l[0]
      },
    }, l[1])
  ]);
}

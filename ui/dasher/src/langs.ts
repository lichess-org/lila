import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, spinner, bind } from './util'
import { get } from './xhr'

export interface Lang {
  0: Code,
  1: string
}

type Code = string;

export interface LangsData {
  current: Code
  accepted: Code[]
}

export interface LangsCtrl {
  data: LangsData
  dict(): Lang[] | undefined
  load(): void
  close: Close
}

export function ctrl(data: LangsData, redraw: Redraw, close: Close): LangsCtrl {

  let dict: Lang[] | undefined;

  return {
    data,
    dict() { return dict },
    load() {
      get(window.lichess.assetUrl('/assets/trans/refs.json'), true).then(d => {
        const accs: Lang[] = [];
        const others: Lang[] = [];
        d.forEach((l: Lang) => {
          if (data.accepted.indexOf(l[0]) > -1) accs.push(l);
          else others.push(l);
        });
        dict = accs.concat(others) as Lang[];
        redraw();
      });
    },
    close
  };
}

export function view(ctrl: LangsCtrl): VNode {

  const dict = ctrl.dict();
  if (!dict) ctrl.load();

  return h('div.sub.langs', [
    h('a.head.text', {
      attrs: { 'data-icon': 'I' },
      hook: bind('click', ctrl.close)
    }, 'Language'),
    dict ? h('form', {
      attrs: { method: 'post', action: '/translation/select' }
    }, langLinks(ctrl, dict)) : spinner()
  ]);
}

function langLinks(ctrl: LangsCtrl, dict: Lang[]) {
  const links = dict.map(langView(ctrl.data.current, ctrl.data.accepted));
  links.push(h('a', {
    attrs: { href: '/translation/contribute' }
  }, 'Help translate lichess'));
  return links;
}

function langView(current: Code, accepted: Code[]) {
  return (l: Lang) =>
  h('button' + (current === l[0] ? '.current' : '') + (accepted.indexOf(l[0]) > -1 ? '.accepted' : ''), {
    attrs: {
      type: 'submit',
      name: 'lang',
      value: l[0]
    },
  }, l[1]);
}

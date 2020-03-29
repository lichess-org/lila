import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Redraw, Close, spinner, header } from './util'
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
  list(): Lang[] | undefined
  load(): void
  trans: Trans
  close: Close
}

export function ctrl(data: LangsData, trans: Trans, redraw: Redraw, close: Close): LangsCtrl {

  let list: Lang[] | undefined;

  return {
    data,
    list: () => list,
    load() {
      get(window.lichess.assetUrl('trans/refs.json'), true).then(d => {
        const accs: Lang[] = [];
        const others: Lang[] = [];
        d.forEach((l: Lang) => {
          if (data.accepted.includes(l[0])) accs.push(l);
          else others.push(l);
        });
        list = accs.concat(others) as Lang[];
        redraw();
      });
    },
    trans,
    close
  };
}

export function view(ctrl: LangsCtrl): VNode {

  const list = ctrl.list();
  if (!list) ctrl.load();

  return h('div.sub.langs', [
    header(ctrl.trans.noarg('language'), ctrl.close),
    list ? h('form', {
      attrs: { method: 'post', action: '/translation/select' }
    }, langLinks(ctrl, list)) : spinner()
  ]);
}

function langLinks(ctrl: LangsCtrl, list: Lang[]) {
  const links = list.map(langView(ctrl.data.current, ctrl.data.accepted));
  links.push(h('a', {
    attrs: { href: 'https://crowdin.com/project/lichess' }
  }, 'Help translate lichess'));
  return links;
}

function langView(current: Code, accepted: Code[]) {
  return (l: Lang) =>
  h('button' + (current === l[0] ? '.current' : '') + (accepted.includes(l[0]) ? '.accepted' : ''), {
    attrs: {
      type: 'submit',
      name: 'lang',
      value: l[0],
      title: l[0]
    },
  }, l[1]);
}

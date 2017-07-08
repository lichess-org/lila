import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { DasherCtrl, DasherData, Mode } from './dasher'
import { view as pingView } from './ping'
import { bind } from './util'

export default function(ctrl: DasherCtrl): VNode {

  const d = ctrl.data, trans = ctrl.trans;

  function userLinks(): Array<VNode | null> | null {
    return d.user ? [
      h(
        'a.user_link.online.text.is-green',
        linkCfg(`/@/${d.user.name}`, d.user.patron ? '' : ''),
        trans.noarg('profile')),

      d.kid ? null : h(
        'a.text',
        linkCfg('/inbox', 'e'),
        trans.noarg('inbox')),

      h(
        'a.text',
        linkCfg('/account/preferences/game-display', '%', ctrl.opts.playing ? {target: '_blank'} : undefined),
        trans.noarg('preferences')),

      !d.coach ? null : h(
        'a.text',
        linkCfg('/coach/edit', ':'),
        'Coach manager'),

      h(
        'a.text',
        linkCfg('/logout', 'w'),
        trans.noarg('logOut'))
    ] : null;
  }

  function anonLinks() {
    return [
      h('a.text',
        linkCfg('/login', 'E'),
        trans('signIn')),
      h('a.text',
        linkCfg('/signup', 'F'),
        trans('signUp'))
    ];
  }

  const langs = h(
    'a.sub',
    modeCfg(ctrl, 'langs'),
    trans.noarg('language'))

  const sound = h(
    'a.sub',
    modeCfg(ctrl, 'sound'),
    trans.noarg('sound'))

  const background = h(
    'a.sub',
    modeCfg(ctrl, 'background'),
    trans.noarg('background'))

  const board = h(
    'a.sub',
    modeCfg(ctrl, 'board'),
    trans.noarg('boardGeometry'))

  const theme = h(
    'a.sub',
    modeCfg(ctrl, 'theme'),
    trans.noarg('boardTheme'))

  const piece = h(
    'a.sub',
    modeCfg(ctrl, 'piece'),
    trans.noarg('pieceSet'))

  return h('div', [
    h('div.links', userLinks() || anonLinks()),
    h('div.subs', [
      langs,
      sound,
      background,
      board,
      theme,
      piece
    ]),
    pingView(ctrl.ping)
  ]);
}

function linkCfg(href: string, icon: string, more: any = undefined): any {
  const cfg: any = {
    attrs: {
      href,
      'data-icon': icon
    }
  };
  if (more) for(let i in more) cfg.attrs[i] = more[i];
  return cfg;
}

function modeCfg(ctrl: DasherCtrl, m: Mode): any {
  return {
    hook: bind('click', () => ctrl.setMode(m)),
    attrs: { 'data-icon': 'H' }
  };
}

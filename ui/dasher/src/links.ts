import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { DasherCtrl, DasherData, Mode } from './dasher'
import { view as pingView } from './ping'
import { bind } from './util'

export default function(ctrl: DasherCtrl): VNode {

  const d = ctrl.data, trans = ctrl.trans, noarg = trans.noarg;

  function userLinks(): Array<VNode | null> | null {
    return d.user ? [
      h(
        'a.user_link.online.text.is-green',
        linkCfg(`/@/${d.user.name}`, d.user.patron ? '' : ''),
        noarg('profile')),

      d.kid ? null : h(
        'a.text',
        linkCfg('/inbox', 'e'),
        noarg('inbox')),

      h(
        'a.text',
        linkCfg('/account/preferences/game-display', '%', ctrl.opts.playing ? {target: '_blank'} : undefined),
        noarg('preferences')),

      !d.coach ? null : h(
        'a.text',
        linkCfg('/coach/edit', ':'),
        'Coach manager'),

      h(
        'a.text',
        linkCfg('/logout', 'w'),
        noarg('logOut'))
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
    noarg('language'))

  const sound = h(
    'a.sub',
    modeCfg(ctrl, 'sound'),
    noarg('sound'))

  const background = h(
    'a.sub',
    modeCfg(ctrl, 'background'),
    noarg('background'))

  const board = h(
    'a.sub',
    modeCfg(ctrl, 'board'),
    noarg('boardGeometry'))

  const theme = h(
    'a.sub',
    modeCfg(ctrl, 'theme'),
    noarg('boardTheme'))

  const piece = h(
    'a.sub',
    modeCfg(ctrl, 'piece'),
    noarg('pieceSet'))

  const zenToggle = ctrl.opts.playing ? h('div.zen.selector', [
    h('a', {
      class: { active: !!ctrl.data.zen },
      attrs: { 'data-icon': ctrl.data.zen ? 'E' : 'K' },
      hook: bind('click', ctrl.toggleZen)
    }, 'Zen mode')
  ]) : null;

  return h('div', [
    h('div.links', userLinks() || anonLinks()),
    h('div.subs', [
      langs,
      sound,
      background,
      board,
      theme,
      piece,
      zenToggle
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

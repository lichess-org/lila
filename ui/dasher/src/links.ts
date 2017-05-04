import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { DasherCtrl, DasherData, Mode } from './dasher'
import { view as pingView } from './ping'
import { bind } from './util'

export default function(ctrl: DasherCtrl): VNode {

  const d = ctrl.data, trans = ctrl.trans;

  const profile = h(
    'a.user_link.online.text.is-green',
    linkCfg(`/@/${d.user.name}`, d.user.patron ? '' : ''),
    trans.noarg('profile'));

  const inbox = d.kid ? null : h(
    'a.text',
    linkCfg('/inbox', 'e'),
    trans.noarg('inbox'));

  const prefs = h(
    'a.text',
    linkCfg('/pref/game-display', '%', ctrl.opts.playing ? {target: '_blank'} : undefined),
    trans.noarg('preferences'));

  const coach = !d.coach ? null : h(
    'a.text',
    linkCfg('/coach/edit', ':'),
    'Coach manager');

  const logout = h(
    'a.text',
    linkCfg('/logout', 'w'),
    trans.noarg('logOut'));

  const langs = h(
    'a.sub',
    modeCfg(ctrl, 'langs'),
    'Language')

  const sound = h(
    'a.sub',
    modeCfg(ctrl, 'sound'),
    trans.noarg('sound'))

  const background = h(
    'a.sub',
    modeCfg(ctrl, 'background'),
    'Background')

  const board = h(
    'a.sub',
    modeCfg(ctrl, 'board'),
    'Board geometry')

  const theme = h(
    'a.sub',
    modeCfg(ctrl, 'theme'),
    'Board theme')

  return h('div', [
    h('div.links', [
      profile,
      inbox,
      prefs,
      coach,
      logout
    ]),
    h('div.subs', [
      langs,
      sound,
      background,
      board,
      theme
    ]),
    pingView(ctrl.ping)
  ]);
}

function linkCfg(href: string, icon: string, more: any = undefined): any {
  const cfg: any = {
    attrs: {
      href: href,
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

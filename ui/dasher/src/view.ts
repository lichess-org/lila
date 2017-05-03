import { h } from 'snabbdom'
import { VNode } from 'snabbdom/vnode'

import { Ctrl, DasherData } from './interfaces'
import { view as pingView } from './ping'

export default function(ctrl: Ctrl): VNode {
  let d = ctrl.data();
  return h('div#dasher_app.dropdown',
    d && !ctrl.initiating() ? renderLinks(ctrl, d) : [h('div.initiating', spinner())]);
}

function renderLinks(ctrl: Ctrl, d: DasherData) {

  const trans = ctrl.trans();

  const profile = h(
    'a.user_link.online.text',
    linkCfg(`/@/${d.name}`, d.patron ? '' : ''),
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

  return [
    h('div.links', [
      profile,
      inbox,
      prefs,
      coach,
      logout
    ]),
    pingView(ctrl.ping)
  ];
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

function spinner() {
  return h('div.spinner', [
    h('svg', { attrs: { viewBox: '0 0 40 40' } }, [
      h('circle', {
        attrs: { cx: 20, cy: 20, r: 18, fill: 'none' }
      })])]);
}

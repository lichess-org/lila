import { i18n } from 'i18n';
import { type VNode, h } from 'snabbdom';
import type { DasherCtrl, Mode } from './dasher';
import { view as pingView } from './ping';
import { bind } from './util';

export default function (ctrl: DasherCtrl): VNode {
  const d = ctrl.data;

  function userLinks(): VNode | null {
    return d.user
      ? h('div.links', [
          h(
            'a.user-link.online.text.is-green',
            linkCfg(`/@/${d.user.name}`, d.user.patron ? '' : ''),
            i18n('profile'),
          ),

          d.inbox ? h('a.text', linkCfg('/inbox', 'e'), i18n('inbox')) : null,

          h(
            'a.text',
            linkCfg(
              '/account/preferences/game-display',
              '%',
              ctrl.opts.playing ? { target: '_blank' } : undefined,
            ),
            i18n('preferences:preferences'),
          ),

          h(
            'a.text',
            linkCfg(
              '/insights/' + d.user.name,
              '7',
              ctrl.opts.playing ? { target: '_blank' } : undefined,
            ),
            i18n('insights:insights'),
          ),

          !d.coach ? null : h('a.text', linkCfg('/coach/edit', ':'), i18n('coachManager')),

          !d.streamer ? null : h('a.text', linkCfg('/streamer/edit', ''), i18n('streamerManager')),

          h(
            'form.logout',
            {
              attrs: { method: 'post', action: '/logout' },
            },
            [
              h(
                'button.text',
                {
                  attrs: {
                    type: 'submit',
                    'data-icon': 'w',
                  },
                },
                i18n('logOut'),
              ),
            ],
          ),
        ])
      : null;
  }

  const langs = h('a.sub', modeCfg(ctrl, 'langs'), i18n('language'));

  const sound = h('a.sub', modeCfg(ctrl, 'sound'), i18n('sound'));

  const background = h('a.sub', modeCfg(ctrl, 'background'), i18n('background'));

  const theme = h('a.sub', modeCfg(ctrl, 'theme'), i18n('boardTheme'));

  const piece = h('a.sub', modeCfg(ctrl, 'piece'), i18n('pieceSet'));

  const notation = h('a.sub', modeCfg(ctrl, 'notation'), i18n('notationSystem'));

  const zenToggle = ctrl.opts.playing
    ? h('div.zen.selector', [
        h(
          'a.text',
          {
            attrs: {
              'data-icon': 'K',
              title: 'Keyboard: z',
            },
            hook: bind('click', () => window.lishogi.pubsub.emit('zen')),
          },
          i18n('preferences:zenMode'),
        ),
      ])
    : null;

  return h('div', [
    userLinks(),
    h('div.subs', [langs, sound, background, theme, piece, notation, zenToggle]),
    pingView(ctrl.ping),
  ]);
}

function linkCfg(href: string, icon: string, more: any = undefined): any {
  const cfg: any = {
    attrs: {
      href,
      'data-icon': icon,
    },
  };
  if (more) for (const i in more) cfg.attrs[i] = more[i];
  return cfg;
}

function modeCfg(ctrl: DasherCtrl, m: Mode): any {
  return {
    hook: bind('click', () => ctrl.setMode(m)),
    attrs: { 'data-icon': 'H' },
  };
}

import { h, VNode } from 'snabbdom';
import * as licon from 'common/licon';
import { DasherCtrl, Mode } from './dasher';
import { view as pingView } from './ping';
import { bind } from './util';

export default function (ctrl: DasherCtrl): VNode {
  const d = ctrl.data,
    trans = ctrl.trans,
    noarg = trans.noarg;

  function userLinks(): VNode | null {
    return d.user
      ? h('div.links', [
          h(
            'a.user-link.online.text.is-green',
            linkCfg(`/@/${d.user.name}`, d.user.patron ? licon.Wings : licon.Disc),
            noarg('profile')
          ),

          h('a.text', linkCfg('/inbox', licon.Envelope), noarg('inbox')),

          h(
            'a.text',
            linkCfg(
              '/account/preferences/display',
              licon.Gear,
              ctrl.opts.playing ? { target: '_blank', rel: 'noopener' } : undefined
            ),
            noarg('preferences')
          ),

          !d.coach ? null : h('a.text', linkCfg('/coach/edit', licon.GraduateCap), noarg('coachManager')),

          !d.streamer ? null : h('a.text', linkCfg('/streamer/edit', licon.Mic), noarg('streamerManager')),

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
                    'data-icon': licon.Power,
                  },
                },
                noarg('logOut')
              ),
            ]
          ),
        ])
      : null;
  }

  const langs = h('button.sub', modeCfg(ctrl, 'langs'), noarg('language'));

  const sound = h('button.sub', modeCfg(ctrl, 'sound'), noarg('sound'));

  const background = h('button.sub', modeCfg(ctrl, 'background'), noarg('background'));

  const board = h('button.sub', modeCfg(ctrl, 'board'), noarg('boardGeometry'));

  const theme = h('button.sub', modeCfg(ctrl, 'theme'), noarg('boardTheme'));

  const piece = h('button.sub', modeCfg(ctrl, 'piece'), noarg('pieceSet'));

  const zenToggle = ctrl.opts.zenable
    ? h('div.zen.selector', [
        h(
          'button.text',
          {
            attrs: {
              'data-icon': licon.DiscBigOutline,
              title: 'Keyboard: z',
              type: 'button',
            },
            hook: bind('click', () => lichess.pubsub.emit('zen')),
          },
          noarg('zenMode')
        ),
      ])
    : null;

  return h('div', [
    userLinks(),
    h('div.subs', [langs, sound, background, board, theme, piece, zenToggle]),
    pingView(ctrl.ping),
  ]);
}

const linkCfg = (href: string, icon: string, more?: Record<string, string>) => ({
  attrs: {
    href,
    'data-icon': icon,
    ...(more || {}),
  },
});

function modeCfg(ctrl: DasherCtrl, m: Mode): any {
  return {
    hook: bind('click', () => ctrl.setMode(m)),
    attrs: { 'data-icon': licon.GreaterThan, type: 'button' },
  };
}

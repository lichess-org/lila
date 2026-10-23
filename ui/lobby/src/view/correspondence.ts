import { perfName } from 'lib/game/perf';
import perfIcons from 'lib/game/perfIcons';
import { bind, confirm, tr, td, span, div, button, table, thead, tbody, th, icon } from 'lib/view';
import { profileUrl } from 'lib/view/userLink';

import type LobbyController from '@/ctrl';
import type { Seek } from '@/interfaces';

function renderSeek(ctrl: LobbyController, seek: Seek) {
  const isJoinAction = seek.action === 'joinSeek';
  return tr(
    `.seek.${isJoinAction ? 'join' : 'cancel'}`,
    {
      key: seek.id,
      role: 'button',
      tabindex: 0,
      title: isJoinAction ? `${i18n.site.joinTheGame} - ${perfName(seek.perf.key)}` : i18n.site.cancel,
      'data-id': seek.id,
    },
    [
      td(
        seek.rating ? span('.ulpt', { 'data-href': profileUrl(seek.username) }, seek.username) : 'Anonymous',
      ),
      td(seek.rating && ctrl.opts.showRatings ? seek.rating + (seek.provisional ? '?' : '') : ''),
      td(seek.days ? i18n.site.nbDays(seek.days) : '∞'),
      td([icon(perfIcons[seek.perf.key])('.varicon'), seek.mode === 1 ? i18n.site.rated : i18n.site.casual]),
    ],
  );
}

function createSeek(ctrl: LobbyController) {
  if (!ctrl.me || ctrl.data.seeks.length >= 8) return undefined;

  return div('.create', [
    button(
      '.button',
      {
        hook: bind(
          'click',
          () => ctrl.setupCtrl.openModal('hook', { variant: 'standard', timeMode: 'correspondence' }),
          ctrl.redraw,
        ),
      },
      i18n.site.createAGame,
    ),
  ]);
}

export default function (ctrl: LobbyController) {
  const activateSeek = async (target: HTMLElement) => {
    const el = target.closest?.('tr[data-id]') as HTMLElement | null;
    if (!el) return;
    if (!ctrl.me) {
      if (await confirm(i18n.site.youNeedAnAccountToDoThat, i18n.site.signUp, i18n.site.cancel))
        location.href = '/signup';
      return;
    }
    return ctrl.clickSeek(el.dataset['id']!);
  };
  return [
    table('.hooks__list', [
      thead(tr((['player', 'rating', 'time', 'mode'] as const).map(k => th(i18n.site[k])))),
      tbody(
        {
          hook: {
            insert: (vnode: any) => {
              bind('click', e => activateSeek(e.target as HTMLElement)).insert?.(vnode);
              (vnode.elm as HTMLElement).addEventListener('keydown', e => {
                if (e.key !== 'Enter' && e.key !== ' ') return;
                e.preventDefault();
                activateSeek(e.target as HTMLElement);
              });
            },
          },
        },
        ctrl.data.seeks.map(s => renderSeek(ctrl, s)),
      ),
    ]),
    createSeek(ctrl),
  ];
}

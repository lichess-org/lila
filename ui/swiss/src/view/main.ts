import * as licon from 'lib/licon';
import { spinnerVdom as spinner } from 'lib/view/controls';
import { type VNode, dataIcon, bind, onInsert, type LooseVNodes, hl } from 'lib/snabbdom';
import { numberRow } from './util';
import type SwissCtrl from '../ctrl';
import { players, renderPager } from '../pagination';
import type { SwissData, Pager } from '../interfaces';
import header from './header';
import standing from './standing';
import * as boards from './boards';
import podium from './podium';
import playerInfo from './playerInfo';
import flatpickr from 'flatpickr';
import { use24h } from 'lib/i18n';
import { once } from 'lib/storage';
import { initMiniGames } from 'lib/view/miniBoard';
import { watchers } from 'lib/view/watchers';
import standaloneChat from 'lib/chat/standalone';
import { prompt } from 'lib/view/dialogs';

export default function (ctrl: SwissCtrl) {
  const d = ctrl.data;
  const content =
    d.status === 'created' ? created(ctrl) : d.status === 'started' ? started(ctrl) : finished(ctrl);
  return hl('main.' + ctrl.opts.classes, { hook: { postpatch: () => initMiniGames() } }, [
    hl('aside.swiss__side', {
      hook: onInsert(el => {
        $(el).replaceWith(ctrl.opts.$side);
        ctrl.opts.chat && standaloneChat(ctrl.opts.chat);
      }),
    }),
    hl('div.swiss__underchat', {
      hook: onInsert(el => $(el).replaceWith($('.swiss__underchat.none').removeClass('none'))),
    }),
    playerInfo(ctrl) || stats(ctrl) || boards.top(d.boards, ctrl.opts),
    hl('div.swiss__main', [hl('div.box.swiss__main-' + d.status, content), boards.many(d.boards, ctrl.opts)]),
    ctrl.opts.chat && hl('div.chat__members.none', { hook: onInsert(watchers) }),
  ]);
}

function created(ctrl: SwissCtrl): LooseVNodes {
  const pag = players(ctrl);
  return [
    header(ctrl),
    nextRound(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
    ctrl.data.quote &&
      hl('blockquote.pull-quote', [hl('p', ctrl.data.quote.text), hl('footer', ctrl.data.quote.author)]),
  ];
}

const notice = (ctrl: SwissCtrl) => {
  const d = ctrl.data;
  return (
    d.me &&
    !d.me.absent &&
    d.status === 'started' &&
    d.nextRound &&
    hl('div.swiss__notice.bar-glider', i18n.site.standByX(d.me.name))
  );
};

function started(ctrl: SwissCtrl): LooseVNodes {
  const pag = players(ctrl);
  return [
    header(ctrl),
    joinTheGame(ctrl) || notice(ctrl),
    nextRound(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

function finished(ctrl: SwissCtrl): LooseVNodes {
  const pag = players(ctrl);
  return [
    hl('div.podium-wrap', [confetti(ctrl.data), header(ctrl), podium(ctrl)]),
    controls(ctrl, pag),
    standing(ctrl, pag, 'finished'),
  ];
}

function controls(ctrl: SwissCtrl, pag: Pager): VNode {
  return hl('div.swiss__controls', [hl('div.pager', renderPager(ctrl, pag)), joinButton(ctrl)]);
}

function nextRound(ctrl: SwissCtrl): VNode | undefined {
  if (!ctrl.opts.schedule || ctrl.data.nbOngoing || ctrl.data.round === 0) return;
  return hl(
    'form.schedule-next-round',
    {
      class: { required: !ctrl.data.nextRound },
      attrs: { action: `/api/swiss/${ctrl.data.id}/schedule-next-round`, method: 'post' },
    },
    [
      hl('input', {
        attrs: { name: 'date', placeholder: 'Schedule the next round', value: ctrl.data.nextRound?.at || '' },
        hook: onInsert((el: HTMLInputElement) =>
          flatpickr(el, {
            minDate: 'today',
            maxDate: new Date(Date.now() + 1000 * 3600 * 24 * 31),
            dateFormat: 'Z',
            altInput: true,
            altFormat: 'Y-m-d h:i K',
            enableTime: true,
            monthSelectorType: 'static',
            onClose() {
              (el.parentNode as HTMLFormElement).submit();
            },
            time_24hr: use24h(),
          }),
        ),
      }),
    ],
  );
}

function joinButton(ctrl: SwissCtrl): VNode | undefined {
  const d = ctrl.data;
  if (!ctrl.opts.userId)
    return hl(
      'a.fbt.text.highlight',
      { attrs: { href: '/login?referrer=' + window.location.pathname, 'data-icon': licon.PlayTriangle } },
      i18n.site.signIn,
    );

  if (d.joinTeam)
    return hl(
      'a.fbt.text.highlight',
      { attrs: { href: `/team/${d.joinTeam}`, 'data-icon': licon.Group } },
      i18n.team.joinTeam,
    );

  if (d.canJoin)
    return ctrl.joinSpinner
      ? spinner()
      : hl(
          'button.fbt.text.highlight',
          {
            attrs: dataIcon(licon.PlayTriangle),
            hook: bind(
              'click',
              async () => {
                if (d.password) {
                  const p = await prompt(i18n.site.tournamentEntryCode);
                  if (p !== null) ctrl.join(p);
                } else ctrl.join();
              },
              ctrl.redraw,
            ),
          },
          i18n.site.join,
        );

  if (d.me && d.status != 'finished')
    return d.me.absent
      ? ctrl.joinSpinner
        ? spinner()
        : hl(
            'button.fbt.text.highlight',
            { attrs: dataIcon(licon.PlayTriangle), hook: bind('click', _ => ctrl.join(), ctrl.redraw) },
            i18n.site.join,
          )
      : ctrl.joinSpinner
        ? spinner()
        : hl(
            'button.fbt.text',
            { attrs: dataIcon(licon.FlagOutline), hook: bind('click', ctrl.withdraw, ctrl.redraw) },
            i18n.site.withdraw,
          );

  return;
}

function joinTheGame(ctrl: SwissCtrl) {
  const gameId = ctrl.data.me?.gameId;
  return (
    gameId &&
    hl('a.swiss__ur-playing.button.is.is-after', { attrs: { href: '/' + gameId } }, [
      i18n.site.youArePlaying,
      hl('br'),
      i18n.site.joinTheGame,
    ])
  );
}

function confetti(data: SwissData) {
  return (
    data.me &&
    data.isRecentlyFinished &&
    once('tournament.end.canvas.' + data.id) &&
    hl('canvas#confetti', {
      hook: {
        insert: _ => site.asset.loadEsm('bits.confetti'),
      },
    })
  );
}

function stats(ctrl: SwissCtrl) {
  const s = ctrl.data.stats,
    slots = ctrl.data.round * ctrl.data.nbPlayers;
  if (!s) return undefined;
  return hl('div.swiss__stats', [
    hl('h2', i18n.site.tournamentComplete),
    hl('table', [
      ctrl.opts.showRatings ? numberRow(i18n.site.averageElo, s.averageRating, 'raw') : null,
      numberRow(i18n.site.gamesPlayed, s.games),
      numberRow(i18n.site.whiteWins, [s.whiteWins, slots], 'percent'),
      numberRow(i18n.site.blackWins, [s.blackWins, slots], 'percent'),
      numberRow(i18n.site.drawRate, [s.draws, slots], 'percent'),
      numberRow(i18n.swiss.byes, [s.byes, slots], 'percent'),
      numberRow(i18n.swiss.absences, [s.absences, slots], 'percent'),
    ]),
    hl('div.swiss__stats__links', [
      hl(
        'a',
        { attrs: { href: `/swiss/${ctrl.data.id}/round/1` } },
        i18n.swiss.viewAllXRounds(ctrl.data.round),
      ),
      hl('br'),
      hl(
        'a.text',
        { attrs: { 'data-icon': licon.Download, href: `/swiss/${ctrl.data.id}.trf`, download: true } },
        'Download TRF file',
      ),
      hl(
        'a.text',
        { attrs: { 'data-icon': licon.Download, href: `/api/swiss/${ctrl.data.id}/games`, download: true } },
        i18n.site.downloadAllGames,
      ),
      hl(
        'a.text',
        {
          attrs: { 'data-icon': licon.Download, href: `/api/swiss/${ctrl.data.id}/results`, download: true },
        },
        'Download results as NDJSON',
      ),
      hl(
        'a.text',
        {
          attrs: {
            'data-icon': licon.Download,
            href: `/api/swiss/${ctrl.data.id}/results?as=csv`,
            download: true,
          },
        },
        'Download results as CSV',
      ),
      hl('br'),
      hl(
        'a.text',
        { attrs: { 'data-icon': licon.InfoCircle, href: 'https://lichess.org/api#tag/Swiss-tournaments' } },
        'Swiss API documentation',
      ),
    ]),
  ]);
}

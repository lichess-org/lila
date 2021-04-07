import { h } from 'snabbdom';
import { VNode } from 'snabbdom';
import { spinner, dataIcon, bind, onInsert, numberRow } from './util';
import SwissCtrl from '../ctrl';
import * as pagination from '../pagination';
import { MaybeVNodes, SwissData, Pager } from '../interfaces';
import header from './header';
import standing from './standing';
import * as boards from './boards';
import podium from './podium';
import playerInfo from './playerInfo';

export default function (ctrl: SwissCtrl) {
  const d = ctrl.data;
  const content = d.status == 'created' ? created(ctrl) : d.status == 'started' ? started(ctrl) : finished(ctrl);
  return h(
    'main.' + ctrl.opts.classes,
    {
      hook: {
        postpatch() {
          lichess.miniGame.initAll();
        },
      },
    },
    [
      h('aside.swiss__side', {
        hook: onInsert(el => {
          $(el).replaceWith(ctrl.opts.$side);
          ctrl.opts.chat && lichess.makeChat(ctrl.opts.chat);
        }),
      }),
      h('div.swiss__underchat', {
        hook: onInsert(el => {
          $(el).replaceWith($('.swiss__underchat.none').removeClass('none'));
        }),
      }),
      playerInfo(ctrl) || stats(ctrl) || boards.top(d.boards),
      h('div.swiss__main', [h('div.box.swiss__main-' + d.status, content), boards.many(d.boards)]),
      ctrl.opts.chat
        ? h('div.chat__members.none', [
            h('span.number', '\xa0'),
            ' ',
            ctrl.trans.noarg('spectators'),
            ' ',
            h('span.list'),
          ])
        : null,
    ]
  );
}

function created(ctrl: SwissCtrl): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    header(ctrl),
    nextRound(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'created'),
    ctrl.data.quote
      ? h('blockquote.pull-quote', [h('p', ctrl.data.quote.text), h('footer', ctrl.data.quote.author)])
      : undefined,
  ];
}

const notice = (ctrl: SwissCtrl): VNode | undefined => {
  const d = ctrl.data;
  return d.me && !d.me.absent && d.status == 'started' && d.nextRound
    ? h('div.swiss__notice.bar-glider', ctrl.trans('standByX', d.me.name))
    : undefined;
};

function started(ctrl: SwissCtrl): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    header(ctrl),
    joinTheGame(ctrl) || notice(ctrl),
    nextRound(ctrl),
    controls(ctrl, pag),
    standing(ctrl, pag, 'started'),
  ];
}

function finished(ctrl: SwissCtrl): MaybeVNodes {
  const pag = pagination.players(ctrl);
  return [
    h('div.podium-wrap', [confetti(ctrl.data), header(ctrl), podium(ctrl)]),
    controls(ctrl, pag),
    standing(ctrl, pag, 'finished'),
  ];
}

function controls(ctrl: SwissCtrl, pag: Pager): VNode {
  return h('div.swiss__controls', [h('div.pager', pagination.renderPager(ctrl, pag)), joinButton(ctrl)]);
}

function nextRound(ctrl: SwissCtrl): VNode | undefined {
  if (!ctrl.opts.schedule || ctrl.data.nbOngoing || ctrl.data.round == 0) return;
  return h(
    'form.schedule-next-round',
    {
      class: {
        required: !ctrl.data.nextRound,
      },
      attrs: {
        action: `/swiss/${ctrl.data.id}/schedule-next-round`,
        method: 'post',
      },
    },
    [
      h('input', {
        attrs: {
          name: 'date',
          placeholder: 'Schedule the next round',
          value: ctrl.data.nextRound?.at || '',
        },
        hook: onInsert((el: HTMLInputElement) =>
          window['LichessFlatpickr'](el, {
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
          })
        ),
      }),
    ]
  );
}

function joinButton(ctrl: SwissCtrl): VNode | undefined {
  const d = ctrl.data;
  if (!ctrl.opts.userId)
    return h(
      'a.fbt.text.highlight',
      {
        attrs: {
          href: '/login?referrer=' + window.location.pathname,
          'data-icon': 'G',
        },
      },
      ctrl.trans('signIn')
    );

  if (d.joinTeam)
    return h(
      'a.fbt.text.highlight',
      {
        attrs: {
          href: `/team/${d.joinTeam}`,
          'data-icon': 'f',
        },
      },
      'Join the team'
    );

  if (d.canJoin)
    return ctrl.joinSpinner
      ? spinner()
      : h(
          'button.fbt.text.highlight',
          {
            attrs: dataIcon('G'),
            hook: bind(
              'click',
              _ => {
                if (d.password) {
                  const p = prompt(ctrl.trans.noarg('password'));
                  if (p !== null) ctrl.join(p);
                } else ctrl.join();
              },
              ctrl.redraw
            ),
          },
          ctrl.trans.noarg('join')
        );

  if (d.me && d.status != 'finished')
    return d.me.absent
      ? ctrl.joinSpinner
        ? spinner()
        : h(
            'button.fbt.text.highlight',
            {
              attrs: dataIcon('G'),
              hook: bind('click', _ => ctrl.join(), ctrl.redraw),
            },
            ctrl.trans.noarg('join')
          )
      : ctrl.joinSpinner
      ? spinner()
      : h(
          'button.fbt.text',
          {
            attrs: dataIcon('b'),
            hook: bind('click', ctrl.withdraw, ctrl.redraw),
          },
          ctrl.trans.noarg('withdraw')
        );

  return;
}

function joinTheGame(ctrl: SwissCtrl) {
  const gameId = ctrl.data.me?.gameId;
  return gameId
    ? h(
        'a.swiss__ur-playing.button.is.is-after',
        {
          attrs: { href: '/' + gameId },
        },
        [ctrl.trans('youArePlaying'), h('br'), ctrl.trans('joinTheGame')]
      )
    : undefined;
}

function confetti(data: SwissData): VNode | undefined {
  return data.me && data.isRecentlyFinished && lichess.once('tournament.end.canvas.' + data.id)
    ? h('canvas#confetti', {
        hook: {
          insert: _ => lichess.loadScript('javascripts/confetti.js'),
        },
      })
    : undefined;
}

function stats(ctrl: SwissCtrl): VNode | undefined {
  const s = ctrl.data.stats,
    noarg = ctrl.trans.noarg,
    slots = ctrl.data.round * ctrl.data.nbPlayers;
  return s
    ? h('div.swiss__stats', [
        h('h2', noarg('tournamentComplete')),
        h('table', [
          numberRow(noarg('averageElo'), s.averageRating, 'raw'),
          numberRow(noarg('gamesPlayed'), s.games),
          numberRow(noarg('whiteWins'), [s.whiteWins, slots], 'percent'),
          numberRow(noarg('blackWins'), [s.blackWins, slots], 'percent'),
          numberRow(noarg('draws'), [s.draws, slots], 'percent'),
          numberRow('Byes', [s.byes, slots], 'percent'),
          numberRow('Absences', [s.absences, slots], 'percent'),
        ]),
        h('div.swiss__stats__links', [
          h(
            'a',
            {
              attrs: {
                href: `/swiss/${ctrl.data.id}/round/1`,
              },
            },
            ctrl.trans('viewAllXRounds', ctrl.data.round)
          ),
          h('br'),
          h(
            'a.text',
            {
              attrs: {
                'data-icon': 'x',
                href: `/swiss/${ctrl.data.id}.trf`,
                download: true,
              },
            },
            'Download TRF file'
          ),
          h(
            'a.text',
            {
              attrs: {
                'data-icon': 'x',
                href: `/api/swiss/${ctrl.data.id}/games`,
                download: true,
              },
            },
            'Download all games'
          ),
          h(
            'a.text',
            {
              attrs: {
                'data-icon': 'x',
                href: `/api/swiss/${ctrl.data.id}/results`,
                download: true,
              },
            },
            'Download results'
          ),
          h('br'),
          h(
            'a.text',
            {
              attrs: {
                'data-icon': '',
                href: 'https://lichess.org/api#tag/Swiss-tournaments',
              },
            },
            'Swiss API documentation'
          ),
        ]),
      ])
    : undefined;
}

import { bind, MaybeVNode } from 'common/snabbdom';
import { ids } from 'game/status';
import { VNode, h } from 'snabbdom';
import TournamentController from '../ctrl';
import * as buttons from './button';
import { player as renderPlayer } from './util';
import { Arrangement } from '../interfaces';

function tableClick(ctrl: TournamentController): (e: Event) => void {
  return (e: Event) => {
    const target = e.target as HTMLElement;
    const players = target.dataset.p;
    if (players) {
      ctrl.showArrangement(ctrl.findOrCreateArrangement(players.split(';')));
    }
  };
}

function preloadUserTips(el: HTMLElement) {
  window.lishogi.powertip.manualUserIn(el);
}

function playerName(ctrl: TournamentController, player) {
  const userId = player.name.toLowerCase();
  return h(
    'div',
    {
      key: userId,
    },
    [
      player.withdraw
        ? h('i', {
            attrs: {
              'data-icon': 'Z',
              title: ctrl.trans.noarg('pause'),
            },
          })
        : undefined,
      renderPlayer(player, false, true),
    ]
  );
}

function button(text: string, icon: string, cls: string, el: MaybeVNode = undefined): VNode {
  return h(
    'button.fbt.is.' + cls,
    {
      attrs: {
        'data-icon': icon,
        title: text,
      },
    },
    el
  );
}

export function controls(ctrl: TournamentController): VNode {
  if (ctrl.arrangement)
    return h('div.tour__controls.arr', [
      h(
        'div.pager',
        { hook: bind('click', () => ctrl.showArrangement(undefined)) },
        button(ctrl.trans.noarg('back'), 'I', 'back.text', ctrl.trans.noarg('back'))
      ),
      h('div.switch', [
        h('label.label', { attrs: { for: 'f-UTC' } }, 'UTC'),
        h('input.cmn-toggle', {
          attrs: {
            id: 'f-UTC',
            type: 'checkbox',
            checked: ctrl.utc,
          },
          hook: bind(
            'change',
            e => {
              const val = (e.target as HTMLInputElement).checked;
              window.lishogi.storage.set('robin.utc', val ? '1' : '0');
              ctrl.utc = val;
            },
            ctrl.redraw
          ),
        }),
        h('label', { attrs: { for: 'f-UTC' } }),
      ]),
    ]);
  else
    return h(
      'div.tour__controls',
      {
        hook: {
          insert: () => {
            arrowControls(ctrl);
          },
        },
      },
      [
        h('div.pager', [
          button(ctrl.trans.noarg('first'), 'W', 'first'),
          button(ctrl.trans.noarg('previous'), 'Y', 'prev'),
          button(ctrl.trans.noarg('next'), 'X', 'next'),
          button(ctrl.trans.noarg('last'), 'V', 'last'),
          ctrl.data.me ? button('Scroll to yourself', '7', 'scroll') : null,
        ]),
        buttons.joinWithdraw(ctrl),
      ]
    );
}

export function standing(ctrl: TournamentController, klass?: string): VNode {
  const maxScore = Math.max(...ctrl.data.standing.players.map(p => p.score || 0)),
    size = ctrl.data.standing.players.length;
  return h('div.r-table-wrap' + (klass ? '.' + klass : '') + (size === 0 ? '.none' : ''), [
    h(
      'div.r-table-wrap-players',
      h('table', [
        h('thead', h('tr', [h('th', '#'), h('th', 'Player')])),
        h(
          'tbody',
          {
            hook: {
              insert: vnode => preloadUserTips(vnode.elm as HTMLElement),
              update(_, vnode) {
                preloadUserTips(vnode.elm as HTMLElement);
              },
            },
          },
          ctrl.data.standing.players.map((player, i) =>
            h('tr', { class: { me: ctrl.opts.userId === player.name.toLowerCase(), long: player.name.length > 15 } }, [
              h('td', i + 1),
              h('td.player-name', playerName(ctrl, player)),
            ])
          )
        ),
      ])
    ),
    h(
      'div.r-table-wrap-arrs',
      h('table', [
        h(
          'thead',
          h(
            'tr',
            ctrl.data.standing.players.map((player, i) => h('th', { attrs: { title: player.name } }, i + 1))
          )
        ),
        h(
          'tbody',
          { hook: bind('click', tableClick(ctrl)) },
          ctrl.data.standing.players.map((player, i) =>
            h(
              'tr',
              ctrl.data.standing.players.map((player2, j) => {
                const arr = ctrl.findArrangement([player.id, player2.id]);
                return h(
                  'td',
                  {
                    attrs: {
                      title: `${player.name} vs ${player2.name}`,
                      'data-p': player.id + ';' + player2.id,
                    },
                    class: {
                      same: i === j,
                    },
                  },
                  !!arr?.status
                    ? h('div', {
                        class: {
                          p: arr.status == ids.started,
                          d: arr.status >= ids.mate && !arr.winner,
                          w: arr.winner === player.id,
                          l: arr.winner === player2.id,
                        },
                      })
                    : null
                );
              })
            )
          )
        ),
      ])
    ),
    h(
      'div.r-table-wrap-scores',
      h('table', [
        h('thead', h('tr', h('th', 'Σ'))),
        h(
          'tbody',
          ctrl.data.standing.players.map(player =>
            h('tr', h('td', { class: { winner: !!maxScore && maxScore === player.score } }, player.score || 0))
          )
        ),
      ])
    ),
  ]);
}

function arrowControls(ctrl: TournamentController) {
  const container = document.querySelector('.r-table-wrap-arrs') as HTMLElement,
    table = container.querySelector('table') as HTMLElement,
    controls = document.querySelector('.tour__controls') as HTMLElement,
    firstArrow = controls.querySelector('button.first') as HTMLElement,
    prevArrow = controls.querySelector('button.prev') as HTMLElement,
    nextArrow = controls.querySelector('button.next') as HTMLElement,
    lastArrow = controls.querySelector('button.last') as HTMLElement,
    scroll = controls.querySelector('button.scroll') as HTMLElement | undefined;

  function updateArrowState() {
    const canScrollLeft = container.scrollLeft > 0,
      canScrollRight = Math.round(container.scrollLeft) < container.scrollWidth - container.clientWidth - 1;

    firstArrow.classList.toggle('disabled', !canScrollLeft);
    prevArrow.classList.toggle('disabled', !canScrollLeft);

    nextArrow.classList.toggle('disabled', !canScrollRight);
    lastArrow.classList.toggle('disabled', !canScrollRight);
  }

  function calculateColumnWidth() {
    const tableWidth = table.offsetWidth;
    return tableWidth / ctrl.data.standing.players.length;
  }

  function scrollLeft(max = false) {
    const columnWidth = calculateColumnWidth();
    const scrollDistance = max
      ? -container.scrollLeft
      : -((Math.floor(container.clientWidth / columnWidth) - 1) * columnWidth);

    container.scrollBy({
      left: scrollDistance,
      behavior: 'smooth',
    });
  }

  function scrollRight(max = false) {
    const columnWidth = calculateColumnWidth();
    const maxScrollRight = container.scrollWidth - container.clientWidth;
    const scrollDistance = max
      ? maxScrollRight - container.scrollLeft
      : (Math.floor(container.clientWidth / columnWidth) - 1) * columnWidth;

    container.scrollBy({
      left: scrollDistance,
      behavior: 'smooth',
    });
  }

  function scrollIntoView() {
    const me = document.querySelector(`tr.me`) as HTMLElement;
    if (me) {
      me.classList.add('highlight');
      me.scrollIntoView({
        behavior: 'smooth',
        block: 'center',
        inline: 'center',
      });
      setTimeout(() => {
        me.classList.remove('highlight');
      }, 2000);
    }
  }

  firstArrow.addEventListener('click', () => scrollLeft(true));
  prevArrow.addEventListener('click', () => scrollLeft(false));
  nextArrow.addEventListener('click', () => scrollRight(false));
  lastArrow.addEventListener('click', () => scrollRight(true));
  if (scroll) scroll.addEventListener('click', () => scrollIntoView());

  container.addEventListener('scroll', updateArrowState);
  window.addEventListener('resize', updateArrowState);

  updateArrowState();
}

function arrangementHasUser(a: Arrangement, userId: string): boolean {
  return a.user1.id === userId || a.user2.id === userId;
}

export function yourCurrent(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => arrangementHasUser(a, ctrl.opts.userId) && a.status === ids.started)
    .sort((a, b) => a.scheduledAt! - b.scheduledAt!)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-current', [h('h2.arrs-title', 'Your current games'), h('div.arrs-grid', arrs)])
    : null;
}

export function yourUpcoming(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => arrangementHasUser(a, ctrl.opts.userId) && a.scheduledAt && !a.gameId)
    .sort((a, b) => a.scheduledAt! - b.scheduledAt!)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-upcoming', [h('h2.arrs-title', 'Your upcoming games'), h('div.arrs-grid', arrs)])
    : null;
}

export function playing(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => a.status === ids.started)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-playing', [h('h2.arrs-title', 'Playing right now'), h('div.arrs-grid', arrs)])
    : null;
}

export function recents(ctrl: TournamentController): MaybeVNode {
  const arrs = (ctrl.data.standing.arrangements as Arrangement[])
    .filter(a => a.status && a.status >= ids.mate)
    .slice(0, 3)
    .map(a => arrangementThumbnail(ctrl, a));
  return arrs.some(a => !!a)
    ? h('div.arrs.arrs-recents', [h('h2.arrs-title', 'Recently played games'), h('div.arrs-grid', arrs)])
    : null;
}

function arrangementThumbnail(ctrl: TournamentController, a: Arrangement): MaybeVNode {
  const players = ctrl.data.standing.players.filter(p => arrangementHasUser(a, p.name.toLowerCase())),
    date = a.scheduledAt ? new Date(a.scheduledAt) : undefined;

  if (players.length === 2)
    return h('div.arr-thumb-wrap', { hook: bind('click', _ => ctrl.showArrangement(a)) }, [
      h('div.arr-players', players[0].name + ' - ' + players[1].name),
      date
        ? h(
            'div.arr-time',
            h('time.timeago', { attrs: { datetime: date.getTime() } }, window.lishogi.timeago.format(date))
          )
        : null,
    ]);
  else null;
}

export function howDoesThisWork(): VNode {
  return h('div.tour__faq.r-how', [
    h('h2', 'Rules'),
    h('div', [
      h(
        'p',
        'Lorem ipsum dolor sit amet, consectetur adipiscing elit. Phasellus ac quam nec leo facilisis dignissim. Sed fringilla mi vel augue consequat, at cursus dolor laoreet.'
      ),
      h(
        'p',
        'Suspendisse potenti. Integer quis orci at sapien viverra malesuada. Donec gravida, eros ac facilisis laoreet, justo arcu malesuada urna, ut vehicula orci lectus ac lacus.'
      ),
      h(
        'p',
        'Proin volutpat sapien vel augue interdum, id feugiat mi ultrices. Ut tristique ipsum ac arcu vehicula, ut eleifend odio tempor. Aliquam erat volutpat.'
      ),
      h(
        'p',
        'Maecenas ultricies magna sit amet lectus volutpat, a luctus libero fermentum. Nam et nisl non magna eleifend venenatis. Cras ut turpis elit. Fusce auctor sem at urna commodo, at placerat tortor ultrices.'
      ),
    ]),
  ]);
}

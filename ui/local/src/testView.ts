import * as co from 'chessops';
import { looseH as h, VNode, onInsert, bind } from 'common/snabbdom';
import { LocalDialog } from './setupDialog';
import { EditBotDialog } from './editBotDialog';
import { ZerofishBot } from './zerofishBot';
import { TestCtrl } from './testCtrl';

site.asset.loadCssPath('local.test');

export function renderTestView(ctrl: TestCtrl): VNode {
  return h('div.test-side', [
    results(ctrl),
    h('hr'),
    h('div', player(ctrl, co.opposite(ctrl.bottomColor))),
    h('div.spacer'),
    startingFen(ctrl),
    h('div.bot-settings', controls(ctrl)),
    h('div.spacer'),
    h('div', [player(ctrl, ctrl.bottomColor)]),
  ]);
}

function player(ctrl: TestCtrl, color: Color): VNode {
  const players = ctrl.root.botCtrl.players;
  const p = players[color];
  const imgUrl = p?.imageUrl ?? site.asset.url(`lifat/bots/images/${color}-torso.webp`);
  const isLight = document.documentElement.classList.contains('light');
  const buttonClass = {
    white: isLight ? '.button-metal' : '.button-inverse',
    black: isLight ? '.button-inverse' : '.button-metal',
  };
  return h(`div.${color}.player`, [
    h('img', { attrs: { src: imgUrl, width: 120, height: 120 } }),
    p &&
      !p?.isRankBot &&
      h('div.bot-actions', [
        p instanceof ZerofishBot &&
          h(
            'button.button' + buttonClass[color],
            {
              hook: onInsert(el =>
                el.addEventListener('click', () => {
                  new EditBotDialog(ctrl.root.botCtrl.zerofishBots, color, p.uid);
                }),
              ),
            },
            'Edit',
          ),
        h(
          'button.button' + buttonClass[color],
          {
            hook: onInsert(el =>
              el.addEventListener('click', () => {
                ctrl.play({
                  type: 'rank',
                  players: [p.uid],
                  iterations: 1, //parseInt($('#num-games').val() as string) || 1,
                  time: '1+0',
                });
              }),
            ),
          },
          'Rank',
        ),
      ]),
    h('div.stats', [
      h('span.totals.strong', p?.name ? p.name + (p?.rating ? ` ${p.rating}` : '') : `${color} Player`),
      //h('span.totals', players[color]?.ratings.get('classical') ?? '0'),
      h('span.totals', ctrl.resultsText(color)),
    ]),
  ]);
}

function controls(ctrl: TestCtrl) {
  const players = ctrl.root.botCtrl.players;
  const hasUser = !players.white || !players.black;
  const stopped = ctrl.stopped;
  const matchInProgress = ctrl.matchInProgress;
  return [
    h('span', [
      h(`button#reset.button.button-metal`, {
        hook: onInsert(el =>
          el.addEventListener('click', () => {
            ctrl.stop();
            ctrl.root.resetToSetup();
            ctrl.root.redraw();
          }),
        ),
      }),
      h(
        'button#new.button.button-metal',
        {
          hook: onInsert(el =>
            el.addEventListener('click', () => {
              const setup = { white: players.white?.uid, black: players.black?.uid };
              new LocalDialog(ctrl.root.botCtrl.bots, setup);
            }),
          ),
        },
        'New',
      ),
      h('input#num-games', { attrs: { type: 'number', min: '1', max: '1000', value: '1' } }),
      h(
        `button#go.button.button-empty${
          hasUser ? '.play.disabled' : matchInProgress && !stopped ? '.pause' : '.play'
        }`,
        { hook: onInsert(el => el.addEventListener('click', () => clickPlayPause(ctrl))) },
      ),
    ]),
    h('span', [
      h(
        'button.button.button-metal',
        {
          hook: onInsert(el =>
            el.addEventListener('click', () => {
              ctrl.play({
                type: 'roundRobin',
                players: Object.values(ctrl.root.botCtrl.bots).map(p => p.uid),
                iterations: 1,
                time: '1+0',
              });
            }),
          ),
        },
        'Tournament',
      ),
    ]),
  ];
}

function clickPlayPause(ctrl: TestCtrl) {
  const players = ctrl.root.botCtrl.players;
  if (!ctrl.stopped) {
    ctrl.stop();
  } else {
    if (ctrl.gameInProgress) ctrl.play();
    else
      ctrl.play({
        type: 'matchup',
        players: [players.white?.uid ?? '#terrence', players.black?.uid ?? '#terrence'],
        iterations: parseInt($('#num-games').val() as string) || 1,
        time: '1+0',
      });
  }
  ctrl.root.redraw();
}

function results(ctrl: TestCtrl) {
  return h('span', [
    h(
      'button#results.button-link',
      { hook: onInsert(el => el.addEventListener('click', () => downloadResults(ctrl))) },
      'Download results',
    ),
    h(
      'button#clear.button-link',
      { hook: onInsert(el => el.addEventListener('click', () => clearResults(ctrl))) },
      'Clear results',
    ),
  ]);
}

async function downloadResults(ctrl: TestCtrl) {
  const results = [{}];
  /*for (const key of await ctrl.store.list()) {
    const result = await ctrl.store.get(key);
    results.push(result);
    console.log(result);
  }*/

  const blob = new Blob([JSON.stringify(results)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'results.json';
  a.click();
  URL.revokeObjectURL(url);
}

function clearResults(ctrl: TestCtrl) {
  if (!confirm('Clear all results?')) return;
  //ctrl.store.clear();
  ctrl.redraw();
}

function startingFen(ctrl: TestCtrl): VNode {
  return h('input.starting-fen', {
    attrs: { placeholder: co.fen.INITIAL_BOARD_FEN, spellcheck: 'false' },
    hook: bind('input', e => {
      const el = e.target as HTMLInputElement;
      if (!el.value) ctrl.startingFen = co.fen.INITIAL_FEN;
      else if (co.fen.parseFen(el.value).isOk) ctrl.startingFen = el.value;
      else {
        el.style.backgroundColor = 'red';
        return;
      }
      el.style.backgroundColor = '';
      ctrl.root.resetBoard(ctrl.startingFen);
      if (!ctrl.isStopped && !ctrl.root.isUserTurn) ctrl.root.botMove();
      ctrl.redraw();
    }),
    props: { value: ctrl.startingFen },
  });
}

//const formatHashSize = (v: number): string => (v < 1000 ? v + 'MB' : Math.round(v / 1024) + 'GB');

function renderSettings(ctrl: TestCtrl): VNode | null {
  // const noarg = (text: string) => text,
  //   engCtrl = ctrl.engines;

  // function searchTick() {
  //   return Math.max(
  //     0,
  //     searchTicks.findIndex(([tickMs]) => tickMs >= ctrl.params.movetime),
  //   );
  // }

  return h('div.bot-settings', [
    /*(id => {
      return h('div.setting', [
        h('label', 'Move time'),
        h('input#' + id, {
          attrs: { type: 'range', min: 0, max: searchTicks.length - 1, step: 1 },
          hook: rangeConfig(searchTick, n => {
            ctrl.params.movetime = searchTicks[n][0];
            ctrl.storeParams();
            ctrl.redraw();
          }),
        }),
        h('div.range_value', searchTicks[searchTick()][1]),
      ]);
    })('engine-search-ms'),*/
    /*(id => {
      return h('div.setting', [
        h('label', 'N-fold draw'),
        h('input#' + id, {
          attrs: { type: 'range', min: 0, max: 12, step: 3 },
          hook: rangeConfig(
            () => ctrl.script.nfold ?? 3,
            x => {
              ctrl.params.nfold = x;
              ctrl.storeParams();
              ctrl.redraw();
            },
          ),
        }),
        h('div.range_value', ctrl.params.nfold === 0 ? 'no draw' : `${ctrl.params.nfold ?? 3} moves`),
      ]);
    })('draw-after') */ /*
    hasFeature('sharedMem')
      ? (id => {
          return h('div.setting', [
            h('label', { attrs: { for: id } }, noarg('Threads')),
            h('input#' + id, {
              attrs: {
                type: 'range',
                min: 1,
                max: navigator.hardwareConcurrency,
                step: 1,
              },
              hook: rangeConfig(
                () => Math.min(ctrl.params.threads, navigator.hardwareConcurrency),
                x => {
                  ctrl.params.threads = x;
                  ctrl.storeParams();
                  ctrl.redraw();
                },
              ),
            }),
            h('div.range_value', `${ctrl.params.threads} / ${navigator.hardwareConcurrency}`),
          ]);
        })('analyse-threads')
      : null,
    (id =>
      h('div.setting', [
        h('label', { attrs: { for: id } }, noarg('Memory')),
        h('input#' + id, {
          attrs: {
            type: 'range',
            min: 4,
            max: Math.floor(Math.log2(engCtrl.active?.maxHash ?? 4)),
            step: 1,
          },
          hook: rangeConfig(
            () => {
              return Math.floor(Math.log2(ctrl.params.hash));
            },
            v => {
              ctrl.params.hash = Math.pow(2, v);
              ctrl.storeParams();
              ctrl.redraw();
            },
          ),
        }),
        h('div.range_value', formatHashSize(ctrl.params.hash)),
      ]))('analyse-memory'),*/
    ...controls(ctrl),
  ]);
}

/*function botSelection(ctrl: TestCtrl, color: Color): VNode {
  const bots = ctrl.root.botCtrl.bots;
  const players = ctrl.root.botCtrl.players;
  return h(
    'select.select-bot',
    {
      hook: bind('change', e => {
        const value = (e.target as HTMLSelectElement).value;
        if (players[color]?.uid === value || (!value && !players[color])) return;
        ctrl.totals = { gamesLeft: ctrl.totals?.gamesLeft ?? 1, white: 0, black: 0, draw: 0, error: 0 };
        ctrl.root.botCtrl.setBot(color, !value ? undefined : value);
        ctrl.storeParams();
        ctrl.redraw();
      }),
      props: {
        value: players[color]?.uid,
      },
    },
    [
      h('option', { attrs: { value: '', selected: players[color] === undefined } }, 'You'),
      ...Object.values(bots).map(bot => {
        //console.log(color, players[color]?.uid, '==', bot.uid);
        return h(
          'option',
          {
            attrs: {
              value: bot.uid,
              selected: players[color]?.uid === bot.uid,
            },
          },
          bot.name,
        );
      }),
    ],
  );
}
*/

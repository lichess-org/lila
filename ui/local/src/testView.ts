import * as co from 'chessops';
import { looseH as h, VNode, onInsert, bind } from 'common/snabbdom';
import { LocalDialog } from './setupDialog';
import { storedBooleanProp } from 'common/storage';
import { domDialog } from 'common/dialog';
import { EditDialog } from './editor/editDialog';
import { ZerofishBot } from './zerofishBot';
import { BotCtrl } from './botCtrl';
import { TestCtrl } from './testCtrl';
import { GameCtrl } from './gameCtrl';
import { Libot } from './types';
import { playerResults, playersWithResults } from './testUtil';

site.asset.loadCssPath('local.test');

interface TestContext {
  testCtrl: TestCtrl;
  botCtrl: BotCtrl;
  gameCtrl: GameCtrl;
}

function testContext(testCtrl: TestCtrl): TestContext {
  return {
    testCtrl,
    botCtrl: testCtrl.botCtrl,
    gameCtrl: testCtrl.gameCtrl,
  };
}
export function renderTestView(testCtrl: TestCtrl): VNode {
  const ctx = testContext(testCtrl);
  return h('div.test-side', [
    results(ctx),
    h('hr'),
    h('div', player(ctx, co.opposite(testCtrl.bottomColor))),
    h('div.spacer'),
    fen(ctx),
    testCtrl.script.type !== 'matchup' ? progress(ctx) : dashboard(ctx),
    h('div.spacer'),
    h('div', [player(ctx, testCtrl.bottomColor)]),
  ]);
}

function player(ctx: TestContext, color: Color): VNode {
  const { testCtrl, botCtrl } = ctx;
  const p = botCtrl[color];
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
            { hook: onInsert(el => el.addEventListener('click', () => editBot(ctx, color))) },
            'Edit',
          ),
        (p.glicko?.rd ?? 350) > 60 &&
          h(
            'button.button' + buttonClass[color],
            {
              hook: onInsert(el =>
                el.addEventListener('click', () => {
                  testCtrl.run({
                    type: 'rank',
                    players: [p.uid, ...botCtrl.rankBots.map(b => b.uid)],
                    time: '1+0',
                  });
                }),
              ),
            },
            'Rank',
          ),
      ]),
    h('div.stats', [
      h('span.totals.strong', p?.name ? `${p.name} ${p.ratingText}` : `Player ${color}`),
      h('span.totals', playerResults(testCtrl.script.results, botCtrl[color]?.uid)),
    ]),
  ]);
}

function editBot({ testCtrl, botCtrl }: TestContext, color: Color) {
  const selected = botCtrl[color]?.uid;
  if (!selected) return;
  new EditDialog(botCtrl, testCtrl.gameCtrl, color, (uid: string) => {
    // if (selected !== botCtrl[color]?.uid) return;
    // selected = uid;
    // botCtrl[color] = botCtrl.bot(uid);
    // testCtrl.gameCtrl.setup[color] = uid;
    // testCtrl.reset(false);
    //console.log('bot changed', color, uid, testCtrl.script, testCtrl.gameCtrl.setup);
    testCtrl.redraw();
  })
    .show()
    .then(testCtrl.redraw);
}

function dashboard(ctx: TestContext) {
  const { testCtrl, botCtrl } = ctx;
  return h('div.test-dashboard', [
    h('span', [
      h(`button.refresh.button.button-metal`, {
        hook: onInsert(el =>
          el.addEventListener('click', () => {
            testCtrl.stop();
            testCtrl.gameCtrl.resetToSetup();
            testCtrl.gameCtrl.redraw();
          }),
        ),
      }),
      h(
        'button#new.button.button-metal',
        {
          hook: onInsert(el =>
            el.addEventListener('click', () => {
              const setup = { white: botCtrl.white?.uid, black: botCtrl.black?.uid };
              new LocalDialog(botCtrl.bots, setup);
            }),
          ),
        },
        'setup',
      ),
      h('input.num-games', { attrs: { type: 'number', min: '1', max: '1000', value: '1' } }),
      renderPlayPause(ctx),
    ]),
    h('span', [
      h(
        'button.button.button-metal',
        { hook: onInsert(el => el.addEventListener('click', () => roundRobin(ctx))) },
        'round robin',
      ),
    ]),
  ]);
}

function progress(ctx: TestContext) {
  const { testCtrl, botCtrl } = ctx;
  return h('div.test-progress', [
    h('h3', [
      testCtrl.script.type === 'rank'
        ? 'Ranking...'
        : `Game ${testCtrl.script.results.length + 1} of ${testCtrl.script.games.length}`,
      renderPlayPause(ctx),
      h(`button.button.reset.button-metal`, {
        hook: onInsert(el => el.addEventListener('click', () => testCtrl.reset())),
      }),
    ]),
    h(
      'div.results',
      playersWithResults(testCtrl.script).map(p => {
        const bot = botCtrl.bot(p)!;
        return h('div', `${bot?.name ?? p} ${playerResults(testCtrl.script.results, p)} ${bot.ratingText}`);
      }),
    ),
  ]);
}

function renderPlayPause(ctx: TestContext): VNode {
  const { testCtrl } = ctx;
  return h(
    `button.play-pause.button.button-metal${
      testCtrl.hasUser
        ? '.play.disabled'
        : testCtrl.testInProgress && !testCtrl.isStopped
        ? '.pause'
        : '.play'
    }`,
    { hook: onInsert(el => el.addEventListener('click', () => clickPlayPause(ctx))) },
  );
}

function clickPlayPause({ testCtrl }: TestContext) {
  if (testCtrl.hasUser) return;
  if (!testCtrl.isStopped) testCtrl.stop();
  else {
    if (testCtrl.gameInProgress) testCtrl.run();
    else {
      testCtrl.run(
        { type: 'matchup', players: [testCtrl.white.uid, testCtrl.black.uid], time: '1+0' },
        parseInt($('.num-games').val() as string) || 1,
      );
    }
  }
  testCtrl.gameCtrl.redraw();
}

function results(ctx: TestContext) {
  return h('span', [
    h(
      'button.results-action.button-link',
      { hook: onInsert(el => el.addEventListener('click', () => downloadResults(ctx))) },
      'Download results',
    ),
    h(
      'button.results-action.button-link',
      { hook: onInsert(el => el.addEventListener('click', () => clearResults(ctx))) },
      'Clear results',
    ),
  ]);
}

async function downloadResults(ctx: TestContext) {
  const results = [{}];

  const blob = new Blob([JSON.stringify(results)], { type: 'application/json' });
  const url = URL.createObjectURL(blob);
  const a = document.createElement('a');
  a.href = url;
  a.download = 'results.json';
  a.click();
  URL.revokeObjectURL(url);
}

function clearResults({ testCtrl }: TestContext) {
  if (!confirm('Clear all results?')) return;
  //testCtrl.store.clear();
  testCtrl.redraw();
}

function fen({ testCtrl, gameCtrl }: TestContext): VNode {
  return h('input.fen', {
    attrs: { value: gameCtrl.fen, spellcheck: 'false' },
    hook: bind('input', e => {
      let fen = co.fen.INITIAL_FEN;
      const el = e.target as HTMLInputElement;
      if (!el.value || co.fen.parseFen(el.value).isOk) fen = el.value || co.fen.INITIAL_FEN;
      else {
        el.style.backgroundColor = 'red';
        return;
      }
      el.style.backgroundColor = '';
      if (fen) {
        gameCtrl.resetBoard(fen);
        if (!testCtrl.isStopped && !gameCtrl.isUserTurn) gameCtrl.botMove();
        testCtrl.redraw();
      }
    }),
    props: { value: testCtrl.startingFen },
  });
}

function roundRobin({ testCtrl, botCtrl }: TestContext) {
  domDialog({
    class: 'tournament-dialog',
    htmlText: `<h2>Round robin</h2><h3>Select participants</h3>
    <ul>${[...Object.values(botCtrl.bots), ...botCtrl.rankBots]
      .map(p => {
        const checked = storedBooleanProp(`local.test.tournament-${p.uid.slice(1)}`, true)();
        return `<li><input type="checkbox" id="${p.uid.slice(1)}" ${checked ? 'checked=""' : ''} value="${
          p.uid
        }">
        <label for='${p.uid.slice(1)}'>${p.name} ${p.ratingText}</label></li>`;
      })
      .join('')}</ul>
    <span style="display: flex; gap: 1em;">Repeat: <input type="number" maxLength="3" value="1"><button class="button" id="start-tournament">Start</button></span>`,
    actions: [
      {
        selector: '#start-tournament',
        listener: dlg => {
          const participants = Array.from(dlg.view.querySelectorAll('input:checked')).map(
            (el: HTMLInputElement) => el.value,
          );
          if (participants.length < 2) return;
          const iterationField = dlg.view.querySelector('input[type="number"]') as HTMLInputElement;
          const iterations = parseInt(iterationField.value);
          testCtrl.run(
            {
              type: 'roundRobin',
              players: participants,
              time: '1+0',
            },
            isNaN(iterations) ? 1 : iterations,
          );
          dlg.close();
        },
      },
      {
        selector: 'input[type="checkbox"]',
        event: 'change',
        listener: (_, __, e) => {
          const el = e.target as HTMLInputElement;
          storedBooleanProp(`local.test.tournament-${el.value.slice(1)}`, true)(el.checked);
        },
      },
    ],
    show: 'modal',
  });
}

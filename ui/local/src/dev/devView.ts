import * as co from 'chessops';
import { looseH as h, VNode, onInsert, bind } from 'common/snabbdom';
import * as licon from 'common/licon';
import { storedBooleanProp } from 'common/storage';
import { domDialog } from 'common/dialog';
import { EditDialog } from './editDialog';
import { ZerofishBot } from '../zerofishBot';
import { playerResults, playersWithResults } from './util';
import { handOfCards, type Drop, type HandOfCards } from '../handOfCards';
import { domIdToUid, uidToDomId, type BotCtrl } from '../botCtrl';
import type { DevCtrl } from './devCtrl';
import type { GameCtrl } from '../gameCtrl';
import { rangeTicks } from '../gameView';
import { defined } from 'common';
import { LocalSetup } from '../types';

interface DevContext {
  devCtrl: DevCtrl;
  botCtrl: BotCtrl;
  gameCtrl: GameCtrl;
}

function devContext(devCtrl: DevCtrl): DevContext {
  return {
    devCtrl,
    gameCtrl: devCtrl.gameCtrl,
    botCtrl: devCtrl.gameCtrl.botCtrl,
  };
}

export function renderDevView(devCtrl: DevCtrl): VNode {
  const ctx = devContext(devCtrl);
  console.log(ctx.gameCtrl.cgOrientation);
  return h('div.dev-side.dev-view', [
    h('div', player(ctx, co.opposite(ctx.gameCtrl.cgOrientation))),
    dashboard(ctx),
    progress(ctx),
    h('div', player(ctx, ctx.gameCtrl.cgOrientation)),
  ]);
}

function player(ctx: DevContext, color: Color): VNode {
  const { devCtrl, botCtrl } = ctx;
  const p = botCtrl[color];
  const imgUrl = botCtrl.imageUrl(p) ?? site.asset.url(`/lifat/bots/misc/${color}-torso.webp`);
  const isLight = document.documentElement.classList.contains('light');
  const buttonClass = {
    white: isLight ? '.button-metal' : '.button-inverse',
    black: isLight ? '.button-inverse' : '.button-metal',
  };
  return h(
    `div.${color}.player`,
    {
      hook: onInsert(el => el.addEventListener('click', () => showBotSelector(ctx, el))),
    },
    [
      botCtrl[color] &&
        h('button.button.button-empty.button-red.icon-btn.upper-right', {
          attrs: { 'data-click': 'remove', 'data-icon': licon.Cancel },
          hook: bind('click', e => {
            reset(ctx, { ...botCtrl.uids, [color]: undefined });
            e.stopPropagation();
          }),
        }),
      h('img', { attrs: { src: imgUrl } }),
      p &&
        !('level' in p) &&
        h('div.bot-actions', [
          p instanceof ZerofishBot &&
            h(
              'button.button' + buttonClass[color],
              {
                hook: onInsert(el =>
                  el.addEventListener('click', e => {
                    editBot(ctx, color);
                    e.stopPropagation();
                  }),
                ),
              },
              'Edit',
            ),
          h(
            'button.button' + buttonClass[color],
            {
              hook: onInsert(el =>
                el.addEventListener('click', e => {
                  p.glicko = undefined;
                  e.stopPropagation();
                  devCtrl.run({
                    type: 'rate',
                    players: [p.uid, ...botCtrl.rateBots.map(b => b.uid)],
                  });
                }),
              ),
            },
            'rate',
          ),
        ]),
      h('div.stats', [
        h('span.totals.strong', p?.name ? `${p.name} ${p.ratingText}` : `Player ${color}`),
        p instanceof ZerofishBot && h('span.totals', p.statsText),
        h('span.totals', playerResults(devCtrl.log, botCtrl[color]?.uid)),
      ]),
    ],
  );
}

async function editBot({ devCtrl, botCtrl }: DevContext, color: Color) {
  const selected = botCtrl[color]?.uid;
  if (!selected) return;
  await new EditDialog(botCtrl, devCtrl.gameCtrl, color, () => devCtrl.redraw()).show();
  devCtrl.redraw();
}

function clockOptions(ctx: DevContext) {
  return h('span', [
    'clock',
    ...(['initial', 'increment'] as const).map(type => {
      const val = ctx.gameCtrl.setup[type] ?? 0;
      return h(
        `select.${type}`,
        {
          hook: onInsert(el =>
            el.addEventListener('change', () =>
              reset(ctx, { [type]: parseInt((el as HTMLSelectElement).value) }),
            ),
          ),
        },
        [
          ...rangeTicks[type].map(([secs, label]) =>
            h('option', { attrs: { value: secs, selected: secs === val } }, label),
          ),
        ],
      );
    }),
  ]);
}

function reset({ gameCtrl }: DevContext, params: Partial<LocalSetup>): void {
  gameCtrl.reset(params);
  localStorage.setItem('local.dev.setup', JSON.stringify(gameCtrl.setup));
  gameCtrl.redraw();
}

function dashboard(ctx: DevContext) {
  const { gameCtrl, devCtrl } = ctx;

  return h('div.dev-dashboard', [
    fen(ctx),
    clockOptions(ctx),
    h('span', [
      h('div', [
        h('label', { attrs: { title: 'instantly deduct move times and disable animations, sound' } }, [
          h('input', {
            attrs: { type: 'checkbox', checked: devCtrl.hurry },
            hook: bind('change', e => {
              devCtrl.hurry = (e.target as HTMLInputElement).checked;
              localStorage.setItem('local.dev.hurry', devCtrl.hurry ? '1' : '0');
            }),
          }),
          'hurry',
        ]),
        h(
          'label',
          { attrs: { title: 'make moves for either side, including bots. but premoves are disabled' } },
          [
            h('input', {
              attrs: { type: 'checkbox', checked: devCtrl.sandbox },
              hook: bind('change', e => {
                devCtrl.sandbox = (e.target as HTMLInputElement).checked;
                localStorage.setItem('local.dev.sandbox', devCtrl.sandbox ? '1' : '0');
                gameCtrl.proxy.updateCg();
              }),
            }),
            'sandbox',
          ],
        ),
      ]),
      h('label', [
        'games',
        h('input.num-games', {
          attrs: { type: 'text', value: '1' },
          hook: bind('input', e => {
            const el = e.target as HTMLInputElement;
            const val = Number(el.value);
            el.classList.toggle('invalid', val < 1 || val > 1000 || isNaN(val));
          }),
        }),
      ]),
    ]),
    h('span', [
      h(
        'button.button.button-metal',
        { hook: onInsert(el => el.addEventListener('click', () => roundRobin(ctx))) },
        'round robin',
      ),
      h('div.spacer'),
      h(`button.refresh.button.button-metal`, {
        hook: onInsert(el =>
          el.addEventListener('click', () => {
            gameCtrl.reset();
            gameCtrl.redraw();
          }),
        ),
      }),
      renderPlayPause(ctx),
    ]),
  ]);
}

function progress(ctx: DevContext) {
  const { devCtrl, botCtrl } = ctx;
  return h('div.dev-progress', [
    h(
      'div.results',
      playersWithResults(devCtrl.log).map(p => {
        const bot = botCtrl.bot(p)!;
        return h('div', `${bot?.name ?? p} ${bot.ratingText} ${playerResults(devCtrl.log, p)}`);
      }),
    ),
  ]);
}

function renderPlayPause({ devCtrl, botCtrl, gameCtrl }: DevContext): VNode {
  const disabled = gameCtrl.isUserTurn;
  const paused = gameCtrl.isStopped || gameCtrl.live.end;
  return h(
    `button.play-pause.button.button-metal${disabled ? '.play.disabled' : paused ? '.play' : '.pause'}`,
    {
      hook: onInsert(el =>
        el.addEventListener('click', () => {
          if (devCtrl.hasUser && gameCtrl.isStopped) gameCtrl.start();
          else if (!paused) gameCtrl.stop();
          else {
            if (devCtrl.gameInProgress) gameCtrl.start();
            else {
              const numGamesField = document.querySelector('.num-games') as HTMLInputElement;
              if (numGamesField.classList.contains('invalid')) {
                numGamesField.focus();
                return;
              }
              const numGames = Number(numGamesField.value);
              devCtrl.run({ type: 'matchup', players: [botCtrl.white!.uid, botCtrl.black!.uid] }, numGames);
            }
          }
          devCtrl.gameCtrl.redraw();
        }),
      ),
    },
  );
}

function fen(ctx: DevContext): VNode {
  const { devCtrl, gameCtrl } = ctx;
  return h('input.fen', {
    attrs: {
      type: 'text',
      value: gameCtrl.fen === co.fen.INITIAL_FEN ? '' : gameCtrl.fen,
      spellcheck: 'false',
      placeholder: co.fen.INITIAL_FEN,
    },
    hook: bind('input', e => {
      let fen = co.fen.INITIAL_FEN;
      const el = e.target as HTMLInputElement;
      if (!el.value || co.fen.parseFen(el.value).isOk) fen = el.value || co.fen.INITIAL_FEN;
      else {
        el.classList.add('invalid');
        return;
      }
      el.classList.remove('invalid');
      if (fen) reset(ctx, { fen });
    }),
    props: { value: devCtrl.startingFen },
  });
}

function roundRobin({ devCtrl, botCtrl }: DevContext) {
  domDialog({
    class: 'round-robin-dialog',
    htmlText: `<h2>Round robin</h2><h3>Select participants</h3>
    <ul>${[...Object.values(botCtrl.bots), ...botCtrl.rateBots]
      .map(p => {
        const checked = isNaN(parseInt(p.uid.slice(1)))
          ? storedBooleanProp(`local.dev.tournament-${p.uid.slice(1)}`, true)()
          : false;
        return `<li><input type="checkbox" id="${p.uid.slice(1)}" ${checked ? 'checked=""' : ''} value="${
          p.uid
        }">
        <label for='${p.uid.slice(1)}'>${p.name} ${p.ratingText}</label></li>`;
      })
      .join('')}</ul>
    <span>Repeat: <input type="number" maxLength="3" value="1"><button class="button" id="start-tournament">Start</button></span>`,
    actions: [
      {
        selector: '#start-tournament',
        listener: (_, dlg) => {
          const participants = Array.from(dlg.view.querySelectorAll('input:checked')).map(
            (el: HTMLInputElement) => el.value,
          );
          if (participants.length < 2) return;
          const iterationField = dlg.view.querySelector('input[type="number"]') as HTMLInputElement;
          const iterations = parseInt(iterationField.value);
          devCtrl.run(
            {
              type: 'roundRobin',
              players: participants,
            },
            isNaN(iterations) ? 1 : iterations,
          );
          dlg.close();
        },
      },
      {
        selector: 'input[type="checkbox"]',
        event: 'change',
        listener: e => {
          const el = e.target as HTMLInputElement;
          if (!isNaN(parseInt(el.value.slice(1)))) return;
          storedBooleanProp(`local.dev.tournament-${el.value.slice(1)}`, true)(el.checked);
        },
      },
    ],
    show: 'modal',
  });
}

let botSelector: HandOfCards | undefined;

function showBotSelector(ctx: DevContext, clickedEl: HTMLElement) {
  const { botCtrl, gameCtrl } = ctx;
  const cardData = [...Object.values(botCtrl.bots).map(b => botCtrl.card(b))].filter(defined);
  const main = document.querySelector('main') as HTMLElement;
  const drops: Drop[] = [];
  main.classList.add('with-cards');

  document.querySelectorAll('main .player')?.forEach(el => {
    const selected = uidToDomId(botCtrl[el.classList.contains('white') ? 'white' : 'black']?.uid);
    drops.push({ el: el as HTMLElement, selected });
  });
  gameCtrl.stop();
  botSelector?.remove();
  botSelector = handOfCards({
    getView: () => main,
    getDrops: () => drops,
    getCardData: () => cardData,
    select: (el, domId) => {
      const uid = domIdToUid(domId);
      const color = (el ?? clickedEl).classList.contains('white') ? 'white' : 'black';
      reset(ctx, { ...botCtrl.uids, [color]: uid });
    },
    onRemove: () => {
      main.classList.remove('with-cards');
      botSelector = undefined;
    },
    orientation: 'left',
    transient: true,
    autoResize: true,
  });
}

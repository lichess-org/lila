import * as co from 'chessops';
import { type VNode, hl, onInsert, bind } from 'lib/snabbdom';
import * as licon from 'lib/licon';
import { storedBooleanProp, storedIntProp } from 'lib/storage';
import { domDialog } from 'lib/view/dialog';
import { EditDialog } from './editDialog';
import { Bot } from 'lib/bot/bot';
import { resultsString, playersWithResults, rangeTicks } from './devUtil';
import { type Drop, type HandOfCards, handOfCards } from './handOfCards';
import { showSetupDialog } from './setupDialog';
import { domIdToUid, uidToDomId } from './devBotCtrl';
import { definedMap } from 'lib/algo';
import type { LocalSpeed, LocalSetup } from 'lib/bot/types';
import { env } from './devEnv';

export function renderDevSide(): VNode {
  return hl('div.dev-side.dev-view', [
    hl('div', player(co.opposite(env.game.screenOrientation))),
    dashboard(),
    progress(),
    hl('div', player(env.game.screenOrientation)),
  ]);
}

function player(color: Color): VNode {
  const p = env.bot[color] as Bot | undefined;
  const imgUrl = env.bot.imageUrl(p) ?? `/assets/lifat/bots/image/${color}-torso.webp`;
  const isLight = document.documentElement.classList.contains('light');
  const buttonClass = {
    white: isLight ? '.button-metal' : '.button-inverse',
    black: isLight ? '.button-inverse' : '.button-metal',
  };
  return hl(
    `div.player`,
    {
      attrs: { 'data-color': color },
      hook: onInsert(el => el.addEventListener('click', () => showBotSelector(el))),
    },
    [
      env.bot[color] &&
        hl(`button.upper-right`, {
          attrs: { 'data-action': 'remove', 'data-icon': licon.Cancel },
          hook: bind('click', e => {
            reset({ ...env.bot.uids, [color]: undefined });
            e.stopPropagation();
          }),
        }),
      hl('img', { attrs: { src: imgUrl } }),
      (!(env.bot.white || env.bot.black) || (p && !('level' in p))) &&
        hl('div.bot-actions', [
          //p instanceof Bot &&
          hl(
            'button.button' + buttonClass[color],
            {
              hook: onInsert(el =>
                el.addEventListener('click', e => {
                  editBot(color);
                  e.stopPropagation();
                }),
              ),
            },
            'Edit',
          ),
          p &&
            !('level' in p) &&
            hl(
              'button.button' + buttonClass[color],
              {
                hook: onInsert(el =>
                  el.addEventListener('click', e => {
                    const bot = env.bot[color] as Bot;
                    if (!bot) return;
                    env.dev.setRating(bot.uid, env.game.speed, { r: 1500, rd: 350 });
                    e.stopPropagation();
                    env.dev.run({
                      type: 'rate',
                      players: [bot.uid, ...env.bot.rateBots.map(b => b.uid)],
                    });
                  }),
                ),
              },
              'rate',
            ),
        ]),
      hl('div.stats', [
        hl('span', env.game.nameOf(color)),
        p && ratingSpan(p),
        p instanceof Bot && hl('span.stats', p.statsText),
        hl('span', resultsString(env.dev.log, env.bot[color]?.uid)),
      ]),
    ],
  );
}

function ratingText(uid: string, speed: LocalSpeed): string {
  const glicko = env.dev.getRating(uid, speed);
  return `${glicko.r}${glicko.rd > 80 ? '?' : ''}`;
}

function ratingSpan(p: Bot): VNode {
  const glicko = env.dev.getRating(p.uid, env.game.speed);
  return hl('span.stats', [
    hl('i', { attrs: { 'data-icon': speedIcon(env.game.speed) } }),
    `${glicko.r}${glicko.rd > 80 ? '?' : ''}`,
  ]);
}

function speedIcon(speed: LocalSpeed = env.game.speed): string {
  switch (speed) {
    case 'classical':
      return licon.Turtle;
    case 'rapid':
      return licon.Rabbit;
    case 'blitz':
      return licon.Fire;
    case 'bullet':
    case 'ultraBullet':
      return licon.Bullet;
  }
}
async function editBot(color: Color) {
  await new EditDialog(color).show();
  env.redraw();
}

function clockOptions() {
  return hl('span', [
    (['initial', 'increment'] as const).map(type => {
      return hl('label', [
        type === 'initial' ? 'clk' : 'inc',
        hl(
          `select.${type}`,
          {
            hook: onInsert(el =>
              el.addEventListener('change', () => {
                const newVal = Number((el as HTMLSelectElement).value);
                reset({ [type]: newVal });
              }),
            ),
          },
          rangeTicks[type].map(([secs, label]) =>
            hl('option', { attrs: { value: secs, selected: secs === env.game[type] } }, label),
          ),
        ),
      ]);
    }),
  ]);
}

function reset(params: Partial<LocalSetup>): void {
  env.game.load(params);
  localStorage.setItem('botdev.setup', JSON.stringify(env.game.live.setup));
  env.redraw();
}

function dashboard() {
  return hl('div.dev-dashboard', [
    fen(),
    clockOptions(),
    hl('span', [
      hl('div', [
        hl('label', { attrs: { title: 'instantly deduct bot move times. disable animations and sound' } }, [
          hl('input', {
            attrs: { type: 'checkbox', checked: env.dev.hurryProp() },
            hook: bind('change', e => env.dev.hurryProp((e.target as HTMLInputElement).checked)),
          }),
          'hurry',
        ]),
      ]),
      hl('label', [
        'games',
        hl('input.num-games', {
          attrs: { type: 'text', value: storedIntProp('botdev.numGames', 1)() },
          hook: bind('input', e => {
            const el = e.target as HTMLInputElement;
            const val = Number(el.value);
            const valid = val >= 1 && val <= 1000 && !isNaN(val);
            el.classList.toggle('invalid', !valid);
            if (valid) localStorage.setItem('botdev.numGames', `${val}`);
          }),
        }),
      ]),
    ]),
    hl('span', [
      hl(
        'button.button.button-metal',
        { hook: bind('click', () => showSetupDialog(env.game.live.setup)) },
        'setup',
      ),
      hl('button.button.button-metal', { hook: bind('click', () => roundRobin()) }, 'tour'),
      hl('div.spacer'),
      hl('button.button.button-metal', {
        attrs: { 'data-icon': licon.ShareIos },
        hook: bind('click', () => report()),
      }),
      hl(`button.board-action.button.button-metal`, {
        attrs: { 'data-icon': licon.Switch },
        hook: bind('click', () => {
          env.game.load({ white: env.bot.uids.black, black: env.bot.uids.white });
          env.redraw();
        }),
      }),
      hl(`button.board-action.button.button-metal`, {
        attrs: { 'data-icon': licon.Reload },
        hook: onInsert(el =>
          el.addEventListener('click', () => {
            env.game.load(undefined);
            env.redraw();
          }),
        ),
      }),
      renderPlayPause(),
    ]),
  ]);
}

function progress() {
  return hl('div.dev-progress', [
    hl('div.results', [
      env.dev.log.length > 0 &&
        hl('button.button.button-empty.button-red.icon-btn.upper-right', {
          attrs: { 'data-icon': licon.Cancel },
          hook: bind('click', () => {
            env.dev.log = [];
            env.redraw();
          }),
        }),
      playersWithResults(env.dev.log).map(p => {
        const bot = env.bot.info(p)!;
        return hl(
          'div',
          `${bot?.name ?? p} ${ratingText(p, env.game.speed)} ${resultsString(env.dev.log, p)}`,
        );
      }),
    ]),
  ]);
}

function renderPlayPause(): VNode {
  const boardTurn = env.game.rewind?.turn ?? env.game.live.turn;
  const disabled = !env.bot[boardTurn];
  const paused = env.game.isStopped || env.game.rewind || env.game.live.finished;
  return hl(
    `button.play-pause.button.button-metal${disabled ? '.play.disabled' : paused ? '.play' : '.pause'}`,
    {
      hook: onInsert(el =>
        el.addEventListener('click', () => {
          if (env.dev.hasUser && env.game.isStopped) env.game.start();
          else if (!paused) {
            env.game.stop();
            env.redraw();
          } else {
            if (env.dev.gameInProgress) env.game.start();
            else {
              const numGamesField = document.querySelector('.num-games') as HTMLInputElement;
              if (numGamesField.classList.contains('invalid')) {
                numGamesField.focus();
                return;
              }
              const numGames = Number(numGamesField.value);
              env.dev.run({ type: 'matchup', players: [env.bot.white!.uid, env.bot.black!.uid] }, numGames);
            }
          }
          env.redraw();
        }),
      ),
    },
  );
}

function fen(): VNode {
  const boardFen = env.game.rewind?.fen ?? env.game.live.fen;
  return hl('input.fen', {
    key: boardFen,
    attrs: {
      type: 'text',
      value: boardFen === co.fen.INITIAL_FEN ? '' : boardFen,
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
      if (fen) reset({ setupFen: fen });
    }),
  });
}

function roundRobin() {
  domDialog({
    class: 'round-robin-dialog',
    htmlText: `<h2>round robin participants</h2>
    <ul>${[...env.bot.sorted(), ...env.bot.rateBots.filter(b => b.ratings[env.game.speed] % 100 === 0)]
      .map(p => {
        const checked = isNaN(parseInt(p.uid.slice(1)))
          ? storedBooleanProp(`botdev.tournament-${p.uid.slice(1)}`, true)()
          : false;
        return `<li><input type="checkbox" id="${p.uid.slice(1)}" ${checked ? 'checked=""' : ''} value="${
          p.uid
        }">
        <label for='${p.uid.slice(1)}'>${p.name} ${ratingText(p.uid, env.game.speed)}</label></li>`;
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
          const iterations = Number(iterationField.value);
          env.dev.run(
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
          storedBooleanProp(`botdev.tournament-${el.value.slice(1)}`, true)(el.checked);
        },
      },
    ],
    show: true,
    modal: true,
  });
}

async function report() {
  const text = await env.dev.getTrace();
  if (text.length) {
    site.asset.loadEsm('bits.diagnosticDialog', {
      init: { text, header: 'Game Info', submit: 'send to lichess' },
    });
  }
}

let botSelector: HandOfCards | undefined;

function showBotSelector(clickedEl: HTMLElement) {
  if (botSelector) return;
  const cardData = definedMap(env.bot.sorted('classical'), b => env.bot.card(b));
  cardData.forEach(c => c.classList.push('left'));
  const main = document.querySelector('main') as HTMLElement;
  const drops: Drop[] = [];
  main.classList.add('with-cards');

  document.querySelectorAll<HTMLElement>('main .player')?.forEach(el => {
    const selected = uidToDomId(env.bot[el.dataset.color as Color]?.uid);
    drops.push({ el: el as HTMLElement, selected });
  });
  botSelector = handOfCards({
    viewEl: main,
    getDrops: () => drops,
    getCardData: () => cardData,
    select: (el, domId) => {
      const color = (el ?? clickedEl).dataset.color as Color;
      reset({ ...env.bot.uids, [color]: domIdToUid(domId) });
    },
    onRemove: () => {
      main.classList.remove('with-cards');
      botSelector = undefined;
    },
    orientation: 'left',
    transient: true,
  });
}

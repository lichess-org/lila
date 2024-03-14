import * as game from 'game';
import { commands } from 'nvui/command';
import { Notify } from 'nvui/notify';
import { renderSetting } from 'nvui/setting';
import {
  Style,
  renderBoard,
  renderHand,
  renderMove,
  renderPieces,
  styleSetting,
  supportedVariant,
  validUsi,
} from 'nvui/shogi';
import { Shogiground } from 'shogiground';
import { VNode, h } from 'snabbdom';
import { renderClock } from '../clock/clockView';
import renderCorresClock from '../corresClock/corresClockView';
import RoundController from '../ctrl';
import { makeConfig as makeSgConfig } from '../ground';
import { Position, Redraw, Step } from '../interfaces';
import { plyStep } from '../round';
import { onInsert } from '../util';
import { renderResult } from '../view/replay';
import { renderTableEnd, renderTablePlay, renderTableWatch } from '../view/table';
import { opposite } from 'shogiground/util';
import { engineNameFromCode } from 'common/engineName';

window.lishogi.RoundNVUI = function (redraw: Redraw) {
  const notify = new Notify(redraw),
    moveStyle = styleSetting();

  window.lishogi.pubsub.on('socket.in.message', line => {
    if (line.u === 'lishogi') notify.set(line.t);
  });
  window.lishogi.pubsub.on('round.suggestion', notify.set);

  return {
    render(ctrl: RoundController): VNode {
      const d = ctrl.data,
        step = plyStep(d, ctrl.ply),
        style = moveStyle.get(),
        variantNope = !supportedVariant(d.game.variant.key) && 'Sorry, this variant is not supported in blind mode.',
        noarg = ctrl.trans.noarg;
      if (!ctrl.shogiground) {
        ctrl.shogiground = Shogiground({
          ...makeSgConfig(ctrl),
          animation: { enabled: false },
          drawable: { enabled: false },
          coordinates: { enabled: false },
        });
      }
      if (variantNope) setTimeout(() => notify.set(variantNope), 3000);
      return h(
        'div.nvui',
        {
          hook: onInsert(_ => setTimeout(() => notify.set(gameText(ctrl)), 2000)),
        },
        [
          h('h1', gameText(ctrl)),
          h('h2', noarg('gameInfo')),
          ...['sente', 'gote'].map((color: Color) =>
            h('p', [color + ' player: ', playerHtml(ctrl, ctrl.playerByColor(color))])
          ),
          h('p', `${noarg(d.game.rated ? 'rated' : 'casual')} ${d.game.perf}`),
          d.clock
            ? h('p', noarg('clock') + `: ${d.clock.initial / 60} + ${d.clock.increment} | ${d.clock.byoyomi})`)
            : null,
          h('h2', noarg('moves')),
          h(
            'p.moves',
            {
              attrs: {
                role: 'log',
                'aria-live': 'off',
              },
            },
            renderMoves(d.steps.slice(1), ctrl.data.game.variant.key, style)
          ),
          h('h2', noarg('pieces')),
          h('div.pieces', renderPieces(ctrl.shogiground.state.pieces, style)),
          h('h2', 'Game status'),
          h(
            'div.status',
            {
              attrs: {
                role: 'status',
                'aria-live': 'assertive',
                'aria-atomic': 'true',
              },
            },
            [ctrl.data.game.status.name === 'started' ? 'Playing' : renderResult(ctrl)]
          ),
          h('h2', 'Last move'),
          h(
            'p.lastMove',
            {
              attrs: {
                'aria-live': 'assertive',
                'aria-atomic': 'true',
              },
            },
            renderMove(step.usi, step.sfen, ctrl.data.game.variant.key, style)
          ),
          ...(ctrl.isPlaying()
            ? [
                h('h2', noarg('moveForm')),
                h(
                  'form',
                  {
                    hook: onInsert(el => {
                      const $form = $(el as HTMLFormElement),
                        $input = $form.find('.move').val('').focus();
                      $form.submit(onSubmit(ctrl, notify.set, moveStyle.get, $input));
                    }),
                  },
                  [
                    h('label', [
                      d.player.color === d.game.player ? 'Your move' : 'Waiting',
                      h('input.move.mousetrap', {
                        attrs: {
                          name: 'move',
                          type: 'text',
                          autocomplete: 'off',
                          autofocus: true,
                          disabled: !!variantNope,
                          title: variantNope,
                        },
                      }),
                    ]),
                  ]
                ),
              ]
            : []),
          h('h2', 'Your clock'),
          h('div.botc', anyClock(ctrl, 'bottom')),
          h('h2', 'Opponent clock'),
          h('div.topc', anyClock(ctrl, 'top')),
          notify.render(),
          h('h2', 'Actions'),
          ...(ctrl.data.player.spectator
            ? renderTableWatch(ctrl)
            : game.playableEvenPaused(ctrl.data)
              ? renderTablePlay(ctrl)
              : renderTableEnd(ctrl)),
          h('h2', [noarg('board'), ' & ', noarg('hands')]),
          h(
            'pre.hand',
            renderHand(
              'top',
              opposite(ctrl.data.player.color),
              ctrl.shogiground.state.hands.handMap.get(opposite(ctrl.data.player.color)),
              ctrl.data.game.variant.key,
              style
            )
          ),
          h(
            'pre.board',
            renderBoard(ctrl.shogiground.state.pieces, ctrl.data.player.color, ctrl.data.game.variant.key, style)
          ),
          h(
            'pre.hand',
            renderHand(
              'bottom',
              ctrl.data.player.color,
              ctrl.shogiground.state.hands.handMap.get(ctrl.data.player.color),
              ctrl.data.game.variant.key,
              style
            )
          ),
          h('h2', noarg('settings')),
          h('label', [noarg('notationSystem'), renderSetting(moveStyle, ctrl.redraw)]),
          h('h2', noarg('commands')),
          h('p', [
            'Type these commands in the move input.',
            h('br'),
            'c: Read clocks.',
            h('br'),
            'l: Read last move.',
            h('br'),
            commands.piece.help,
            h('br'),
            commands.scan.help,
            h('br'),
            'abort: Abort game.',
            h('br'),
            'resign: Resign game.',
            h('br'),
            'draw: Offer or accept draw.',
            h('br'),
            'takeback: Offer or accept take back.',
            h('br'),
          ]),
        ]
      );
    },
  };
};

function onSubmit(ctrl: RoundController, notify: (txt: string) => void, style: () => Style, $input: JQuery) {
  return function () {
    let input = $input.val().trim();
    if (isShortCommand(input)) input = '/' + input;
    if (input[0] === '/') onCommand(ctrl, notify, input.slice(1), style());
    else {
      const d = ctrl.data,
        usi = validUsi(input, plyStep(d, ctrl.ply).sfen, ctrl.data.game.variant.key);

      if (usi)
        ctrl.sendUsiData({
          u: usi,
        });
      else notify(d.player.color === d.game.player ? `Invalid move/drop: ${input}` : 'Not your turn');
    }
    $input.val('');
    return false;
  };
}

const shortCommands = ['c', 'clock', 'l', 'last', 'abort', 'resign', 'draw', 'takeback', 'p', 's', 'o', 'opponent'];

function isShortCommand(input: string): boolean {
  return shortCommands.includes(input.split(' ')[0].toLowerCase());
}

function onCommand(ctrl: RoundController, notify: (txt: string) => void, c: string, style: Style) {
  const lowered = c.toLowerCase();
  if (lowered == 'c' || lowered == 'clock') notify($('.nvui .botc').text() + ', ' + $('.nvui .topc').text());
  else if (lowered == 'l' || lowered == 'last') notify($('.lastMove').text());
  else if (lowered == 'abort') $('.nvui button.abort').click();
  else if (lowered == 'resign') $('.nvui button.resign-confirm').click();
  else if (lowered == 'draw') $('.nvui button.draw-yes').click();
  else if (lowered == 'takeback') $('.nvui button.takeback-yes').click();
  else if (lowered == 'o' || lowered == 'opponent') notify(playerText(ctrl, ctrl.data.opponent));
  else {
    const pieces = ctrl.shogiground.state.pieces,
      hands = ctrl.shogiground.state.hands.handMap;
    notify(
      commands.piece.apply(c, pieces, hands, style) || commands.scan.apply(c, pieces, style) || `Invalid command: ${c}`
    );
  }
}

function anyClock(ctrl: RoundController, position: Position) {
  const d = ctrl.data,
    player = ctrl.playerAt(position);
  return (
    (ctrl.clock && renderClock(ctrl, player, position)) ||
    (d.correspondence && renderCorresClock(ctrl.corresClock!, ctrl.trans, player.color, position, d.game.player)) ||
    undefined
  );
}

function renderMoves(steps: Step[], variant: VariantKey, style: Style) {
  const res: Array<string | VNode> = [];
  steps.forEach(s => {
    if (s.ply & 1) res.push(Math.ceil(s.ply / 2) + ' ');
    res.push(renderMove(s.usi, s.sfen, variant, style) + ', ');
    if (s.ply % 2 === 0) res.push(h('br'));
  });
  return res;
}

function playerHtml(ctrl: RoundController, player: game.Player) {
  if (player.ai) return engineNameFromCode(player.aiCode, player.ai, ctrl.trans);
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : perf && perf.rating,
    rd = player.ratingDiff,
    ratingDiff = rd ? (rd > 0 ? '+' + rd : rd < 0 ? '−' + -rd : '') : '';
  return user
    ? h('span', [
        h(
          'a',
          {
            attrs: { href: '/@/' + user.username },
          },
          user.title ? `${user.title} ${user.username}` : user.username
        ),
        rating ? ` ${rating}` : ``,
        ' ' + ratingDiff,
      ])
    : 'Anonymous';
}

function playerText(ctrl: RoundController, player: game.Player) {
  if (player.ai) return engineNameFromCode(player.aiCode, player.ai, ctrl.trans);
  const d = ctrl.data,
    user = player.user,
    perf = user ? user.perfs[d.game.perf] : null,
    rating = player.rating ? player.rating : perf && perf.rating;
  if (!user) return 'Anonymous';
  return `${user.title || ''} ${user.username} rated ${rating || 'unknown'}`;
}

function gameText(ctrl: RoundController) {
  const d = ctrl.data;
  return [
    d.game.status.name == 'started'
      ? ctrl.isPlaying()
        ? 'You play the ' + ctrl.data.player.color + ' pieces.'
        : 'Spectating.'
      : 'Game over.',
    d.game.rated ? 'Rated' : 'Casual',
    d.clock ? `${d.clock.initial / 60} + ${d.clock.increment}` : '',
    d.game.perf,
    'game versus',
    playerText(ctrl, ctrl.data.opponent),
  ].join(' ');
}

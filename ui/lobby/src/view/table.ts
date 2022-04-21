import { h, thunk } from 'snabbdom';
import { bind } from 'common/snabbdom';
import LobbyController from '../ctrl';
import { GameType } from '../interfaces';
import renderSetupModal from './setup/modal';

export default function table(ctrl: LobbyController) {
  const { data, trans, opts } = ctrl;
  const hasOngoingRealTimeGame = !!data.nowPlaying.find(nowPlaying => nowPlaying.speed !== 'correspondence');
  const hookDisabled = opts.playban || data.hasUnreadLichessMessage || data.me?.isBot || hasOngoingRealTimeGame;
  const { members, rounds } = data.counters;
  return h('div.lobby__table', [
    h('div.bg-switch', { attrs: { title: 'Dark mode' } }, [h('div.bg-switch__track'), h('div.bg-switch__thumb')]),
    h(
      'div.lobby__start',
      (opts.blindMode ? [h('h2', 'Play')] : []).concat(
        [
          ['hook', 'createAGame', hookDisabled],
          ['friend', 'playWithAFriend', hasOngoingRealTimeGame],
          ['ai', 'playWithTheMachine', hasOngoingRealTimeGame],
        ].map(([gameType, transKey, disabled]: [GameType, string, boolean]) =>
          h(
            `a.button.button-metal.config_${gameType}`,
            {
              class: { active: ctrl.setupCtrl.gameType === gameType, disabled },
              hook: bind(opts.blindMode ? 'click' : 'mousedown', () => ctrl.setupCtrl.openModal(gameType), ctrl.redraw),
            },
            trans(transKey)
          )
        )
      )
    ),
    renderSetupModal(ctrl),
    // Use a thunk here so that snabbdom does not rerender; we handle that manually in boot.ts
    thunk(
      'div.lobby__counters',
      () =>
        h('div.lobby__counters', [
          opts.blindMode ? h('h2', 'Counters') : null,
          h(
            'a#nb_connected_players',
            opts.blindMode ? {} : { attrs: { href: '/player' } },
            trans.vdomPlural('nbPlayers', members, h('strong', { attrs: { 'data-count': members } }, members))
          ),
          h(
            'a#nb_games_in_play',
            opts.blindMode ? {} : { attrs: { href: '/games' } },
            trans.vdomPlural('nbGamesInPlay', rounds, h('strong', { attrs: { 'data-count': rounds } }, rounds))
          ),
        ]),
      []
    ),
  ]);
}

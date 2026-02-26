import { storage } from 'lib/storage';
import { variants } from './options';
import type { Customisation, GameType } from './interfaces';
import * as licon from 'lib/licon';
import type LobbyController from './ctrl';
import { hl, type VNode, snabDialog } from 'lib/view';

const custoStoreKey = (username?: string) => `lobby.customisation.${username || 'anon'}`;
const lobbySetupStoreKey = (username: string | undefined, gameType: GameType) =>
  `lobby.setup.${username || 'anon'}.${gameType}`;

export const getAll = (username?: string): Record<string, Customisation> => {
  const raw = storage.make(custoStoreKey(username)).get();
  if (!raw) return {};
  try {
    return JSON.parse(raw);
  } catch {
    return {};
  }
};

export const get = (username: string | undefined, id: string): Customisation | undefined =>
  getAll(username)[id];

export const set = (username: string | undefined, id: string, pool: Customisation) => {
  const all = getAll(username);
  all[id] = pool;
  storage.make(custoStoreKey(username)).set(JSON.stringify(all));
};

export const remove = (username: string | undefined, id: string) => {
  const all = getAll(username);
  delete all[id];
  storage.make(custoStoreKey(username)).set(JSON.stringify(all));
};

export const overrideStoredLobbySetup = (
  poolId: string,
  username: string | undefined,
): Customisation | undefined => {
  const customisation = get(username, poolId);
  if (!customisation) return undefined;

  storage
    .make(lobbySetupStoreKey(username, customisation.gameType))
    .set(JSON.stringify(customisation.settings));

  return customisation;
};

export const renderCustomisedButton = (
  poolId: string,
  customisation: Customisation | undefined,
  selected: boolean,
  transp: boolean,
): VNode | undefined => {
  if (!customisation) return undefined;

  const variantDef = variants.find(v => v.key === customisation.settings.variant);
  const variantIcon =
    customisation.settings.variant !== 'standard' || customisation ? variantDef?.icon : undefined;
  const typeIconAttrs =
    customisation.gameType === 'hook'
      ? { 'data-icon': licon.Group }
      : customisation.gameType === 'friend'
        ? { 'data-icon': licon.User }
        : customisation.gameType === 'ai'
          ? { 'data-icon': licon.Cpu }
          : undefined;
  const timeLabel =
    customisation.settings.timeMode === 'realTime'
      ? `${customisation.settings.time}+${customisation.settings.increment}`
      : customisation.settings.timeMode === 'correspondence'
        ? `${customisation.settings.days}d`
        : '∞';
  const subLabel = customisation.gameType !== 'ai' ? customisation.settings.gameMode === 'rated' ? i18n.site.rated : i18n.site.casual : '';

  return hl(
    'div.lpool',
    {
      class: { selected, custom: true, transp },
      attrs: { role: 'button', 'data-id': poolId, tabindex: '0' },
    },
    [
      hl('div.clock', [
        hl('span', { attrs: typeIconAttrs }),
        variantIcon ? hl('span', { attrs: { 'data-icon': variantIcon } }) : null,
        timeLabel,
      ]),
      hl('div.perf', subLabel),
    ],
  );
};

export function renderCustomiserModalContent(ctrl: LobbyController): VNode[] | null {
  if (!ctrl.isEditingPoolButtons() || !ctrl.selectedPoolButton) return null;
  const customisation = get(ctrl.me?.username, ctrl.selectedPoolButton);
  return [
    hl('h2#lobby-setup-modal-title', 'Customise button ' + ctrl.selectedPoolButton),
    hl('div.setup-content', [
      hl('div.lobby__table', [
        hl('div.lobby__start', [
          makeRestoreButton(ctrl, customisation),
          ...lobbyButtons.map(b => makeCustomiserButton(ctrl, customisation, b)),
        ]),
      ]),
    ]),
  ];
}

export function renderCustomiserModal(ctrl: LobbyController): VNode[] | null {
  if (!ctrl.isEditingPoolButtons() || !ctrl.selectedPoolButton) return null;
  const customisation = get(ctrl.me?.username, ctrl.selectedPoolButton);

  return [
    snabDialog({
      attrs: { dialog: { 'aria-labelledBy': 'lobby-setup-modal-title', 'aria-modal': 'true' } },
      class: 'game-setup',
      css: [{ hashed: 'lobby.setup' }],
      onClose: () => {
        ctrl.selectedPoolButton = undefined;
        ctrl.redraw();
      },
      modal: true,
      vnodes: [
        hl('h2#lobby-setup-modal-title', 'Customise button ' + ctrl.selectedPoolButton),
        hl('div.setup-content', [
          hl('div.lobby__table', [
            hl('div.lobby__start', [
              makeRestoreButton(ctrl, customisation),
              ...lobbyButtons.map(b => makeCustomiserButton(ctrl, customisation, b)),
            ]),
          ]),
        ]),
      ],
      onInsert: dlg => {
        //ctrl.closeCustomiserModal = dlg.close;
        dlg.show();
      },
    }),
  ];
}

type ButtonInfo = { gameType: GameType; label: string; title?: string };
const lobbyButtons: ButtonInfo[] = [
  {
    gameType: 'hook',
    label: i18n.site.createLobbyGame,
  },
  {
    gameType: 'friend',
    label: i18n.site.challengeAFriend,
  },
  {
    gameType: 'ai',
    label: i18n.site.playAgainstComputer,
  },
];

function makeRestoreButton(ctrl: LobbyController, customisation: Customisation | undefined) {
  if (!customisation) return null;

  return hl(
    'button.button.button-metal.lobby__start__button.lobby__start__button--restore',
    {
      on: {
        click: () => {
          remove(ctrl.me?.username, ctrl.selectedPoolButton!);
          ctrl.redraw();
        },
      },
    },
    'Restore quick pairing',
  );
}

function makeCustomiserButton(
  ctrl: LobbyController,
  customisation: Customisation | undefined,
  buttonInfo: ButtonInfo,
) {
  return hl(
    `button.button.button-metal.lobby__start__button.lobby__start__button--${buttonInfo.gameType}`,
    {
      on: {
        click: () => {
          if(customisation) overrideStoredLobbySetup(customisation.gameType, ctrl.me?.username);
          ctrl.setupCtrl.gameType = buttonInfo.gameType;
          ctrl.setupCtrl.loadPropsFromStore();
          ctrl.redraw();
        },
      },
    },
    buttonInfo.label + (customisation && customisation.gameType === buttonInfo.gameType ? ' *' : ''),
  );
}

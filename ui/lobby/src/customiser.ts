import { storage } from 'lib/storage';
import { variants } from './options';
import type { Customisation, GameType } from './interfaces';
import { type VNode, h } from 'snabbdom';
import * as licon from 'lib/licon';

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

  const display = getDisplayData(customisation);
  const label = display.timeLabel;
  const icon = display.icon;
  const typeIconAttrs =
    customisation.gameType === 'hook'
      ? { 'data-icon': licon.Group }
      : customisation.gameType === 'friend'
        ? { 'data-icon': licon.User }
        : customisation.gameType === 'ai'
          ? { 'data-icon': licon.Cpu }
          : undefined;

  const subLabel = customisation.settings.gameMode === 'rated' ? i18n.site.rated : i18n.site.casual;

  return h(
    'div.lpool',
    {
      class: { selected, custom: true, transp },
      attrs: { role: 'button', 'data-id': poolId, tabindex: '0' },
    },
    [
      h('div.clock', [
        h('span', { attrs: typeIconAttrs }),
        icon ? h('span', { attrs: { 'data-icon': icon } }) : null,
        label,
      ]),
      h('div.perf', subLabel),
    ],
  );
};

export const getDisplayData = (p: Customisation) => {
  const timeLabel =
    p.settings.timeMode === 'realTime'
      ? `${p.settings.time}+${p.settings.increment}`
      : p.settings.timeMode === 'correspondence'
        ? `${p.settings.days}d`
        : '∞';

  const variantDef = variants.find(v => v.key === p.settings.variant);

  return {
    timeLabel,
    icon: p.settings.variant !== 'standard' ? variantDef?.icon : undefined,
  };
};

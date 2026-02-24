import { storage } from 'lib/storage';
import { variants } from './options';
import type { Customisation } from './interfaces';
import { type VNode, h } from 'snabbdom';

const makeKey = (username?: string) => `lobby.customisation.${username || 'anon'}`;

export const getAll = (username?: string): Record<string, Customisation> => {
  const raw = storage.make(makeKey(username)).get();
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
  storage.make(makeKey(username)).set(JSON.stringify(all));
};

export const remove = (username: string | undefined, id: string) => {
  const all = getAll(username);
  delete all[id];
  storage.make(makeKey(username)).set(JSON.stringify(all));
};

export const renderCustomisedButton = (
  poolId: string,
  customisation: Customisation | undefined,
  selected: boolean,
): VNode | undefined => {
  if (!customisation) return undefined;

  const display = getDisplayData(customisation);
  const label = display.timeLabel;
  const icon = display.icon;

  const subLabel = customisation.settings.gameMode === 'rated' ? i18n.site.rated : i18n.site.casual;

  return h(
    'div.lpool',
    {
      class: { selected, custom: true },
      attrs: { role: 'button', 'data-id': poolId, tabindex: '0' },
    },
    [
      h('div.clock', [icon ? h('span', { attrs: { 'data-icon': icon } }) : null, label]),
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

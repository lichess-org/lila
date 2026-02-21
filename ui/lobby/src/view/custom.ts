import { h } from 'snabbdom';
import type LobbyController from '../ctrl';
import type { CustomSetup, GameType, SetupStore } from '../interfaces';
import { storage } from 'lib/storage';

export const handler = (ctrl: LobbyController, e: Event) => {
  if (ctrl.redirecting) return;

  if (e instanceof KeyboardEvent) {
    if (e.key !== 'Enter' && e.key !== ' ') return;
    e.preventDefault(); // Prevent page scroll on space
  }

  const id =
    (e.target as HTMLElement).dataset['id'] ||
    ((e.target as HTMLElement).parentNode as HTMLElement).dataset['id'];
  if (id === 'custom') ctrl.setPoolMode('quick_pairing');
  else if (id) openModalWithSettings(Number(id), ctrl);
};

export function render(ctrl: LobbyController) {
  const ret = JSON.parse(storage.get(ctrl.setupCtrl.storeKey('custom'))!);
  const customSetups: CustomSetup[] = ret || [];
  const poolNumber = ctrl.pools.length;
  const padding =
    customSetups.length < poolNumber ? new Array(poolNumber - customSetups.length).fill(null) : [];
  let i = 0;
  return customSetups
    .map(setting => {
      return h(
        'div.lpool',
        {
          attrs: { role: 'button', 'data-id': i++, tabindex: '0' },
        },
        [h('div.perf', setting.gameType)],
      );
    })
    .concat(padding.map(() => h('div.lpool')))
    .concat(
      h(
        'div.lpool',
        {
          attrs: { role: 'button', 'data-id': 'custom', tabindex: '0' },
        },
        i18n.site.quickPairing,
      ),
    );
}

export function saveSettings(
  gameType: GameType,
  storeKey: string,
  settings: SetupStore,
  poolNumber: number = 11,
) {
  const ret = JSON.parse(storage.get(storeKey)!);
  const customSetups: CustomSetup[] = ret || [];
  customSetups.unshift({ gameType, settings });
  if (customSetups.length > poolNumber) customSetups.pop();
  storage.set(storeKey, JSON.stringify(customSetups));
}

function openModalWithSettings(settingId: number, ctrl: LobbyController) {
  const ret = JSON.parse(storage.get(ctrl.setupCtrl.storeKey('custom'))!);
  const customSetups: CustomSetup[] = ret || [];
  const setting = customSetups[settingId];
  if (setting) {
    const storeKey = ctrl.setupCtrl.storeKey(setting.gameType);
    storage.set(storeKey, JSON.stringify(setting.settings));
    ctrl.setupCtrl.openModal(setting.gameType);
    ctrl.redraw();
  }
}

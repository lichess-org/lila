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
  else if (id) ctrl.clickPool(id);
};

export function render(ctrl: LobbyController) {
  return ctrl.pools
    .map(pool => {
      return h(
        'div.lpool',
        {
          attrs: { role: 'button', 'data-id': pool.id, tabindex: '0' },
        },
        [h('div.perf', 'custom')],
      );
    })
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

export function saveSettings(gameType: GameType, storeKey: string, settings: SetupStore, storeLimit: number = 11) {
  const ret = JSON.parse(storage.get(storeKey)!);
  const customSetups: CustomSetup[] = ret || [];
  customSetups.unshift({ gameType, settings });
  if (customSetups.length > storeLimit) customSetups.pop();
  storage.set(storeKey, JSON.stringify(customSetups));
}
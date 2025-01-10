import type LobbyController from './ctrl';
import type { Hook, Tab } from './interfaces';

export const tabs: Tab[] = ['real_time', 'presets'];

const ratingOrder =
  (reverse: boolean) =>
  (a: Hook, b: Hook): number =>
    ((a.rating || 0) > (b.rating || 0) ? -1 : 1) * (reverse ? -1 : 1);

const timeOrder =
  (reverse: boolean) =>
  (a: Hook, b: Hook): number =>
    (a.t > b.t ? -1 : 1) * (reverse ? -1 : 1);

export function sort(ctrl: LobbyController, hooks: Hook[]): void {
  const s = ctrl.sort;
  hooks.sort(s.startsWith('time') ? timeOrder(s !== 'time') : ratingOrder(s !== 'rating'));
}

export function add(ctrl: LobbyController, hook: Hook): void {
  ctrl.data.hooks.push(hook);
}
export function setAll(ctrl: LobbyController, hooks: Hook[]): void {
  ctrl.data.hooks = hooks;
}
export function remove(ctrl: LobbyController, id: string): void {
  ctrl.data.hooks = ctrl.data.hooks.filter(h => h.id !== id);
  ctrl.stepHooks.forEach(h => {
    if (h.id === id) h.disabled = true;
  });
  if (ctrl.currentPresetId && !ctrl.data.hooks.some(h => h.sri === window.lishogi.sri))
    ctrl.currentPresetId = undefined;
}
export function syncIds(ctrl: LobbyController, ids: string[]): void {
  ctrl.data.hooks = ctrl.data.hooks.filter(h => ids.includes(h.id));
  if (ctrl.currentPresetId && !ctrl.data.hooks.some(h => h.sri === window.lishogi.sri))
    ctrl.currentPresetId = undefined;
}
export function find(ctrl: LobbyController, id: string): Hook | undefined {
  return ctrl.data.hooks.find(h => h.id === id);
}

import LobbyController from './ctrl';
import { Hook, Tab } from './interfaces';

export const tabs: Tab[] = ['real_time', 'presets'];

function ratingOrder(a: Hook, b: Hook) {
  return (a.rating || 0) > (b.rating || 0) ? -1 : 1;
}

function timeOrder(a: Hook, b: Hook) {
  return a.t < b.t ? -1 : 1;
}

export function sort(ctrl: LobbyController, hooks: Hook[]) {
  hooks.sort(ctrl.sort === 'time' ? timeOrder : ratingOrder);
}

export function action(hook: Hook): 'cancel' | 'join' {
  return hook.sri === window.lishogi.sri ? 'cancel' : 'join';
}

export function add(ctrl: LobbyController, hook: Hook) {
  ctrl.data.hooks.push(hook);
}
export function setAll(ctrl: LobbyController, hooks: Hook[]) {
  ctrl.data.hooks = hooks;
}
export function remove(ctrl: LobbyController, id: string) {
  ctrl.data.hooks = ctrl.data.hooks.filter(h => h.id !== id);
  ctrl.stepHooks.forEach(h => {
    if (h.id === id) h.disabled = true;
  });
}
export function syncIds(ctrl: LobbyController, ids: string[]) {
  ctrl.data.hooks = ctrl.data.hooks.filter(h => ids.includes(h.id));
  if (ctrl.currentPresetId && !ids.includes(window.lishogi.sri)) ctrl.currentPresetId = '';
}
export function find(ctrl: LobbyController, id: string) {
  return ctrl.data.hooks.find(h => h.id === id);
}

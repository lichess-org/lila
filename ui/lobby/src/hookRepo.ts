import LobbyController from './ctrl';
import { Hook } from './interfaces';

function ratingOrder(a, b) {
  return (a.rating || 0) > (b.rating || 0) ? -1 : 1;
}

function timeOrder(a, b) {
  return a.t < b.t ? -1 : 1;
}

export function sort(ctrl: LobbyController, hooks: Hook[]) {
  hooks.sort(ctrl.sort === 'time' ? timeOrder : ratingOrder);
}

export function init(hook: Hook) {
  hook.action = hook.sri === window.lichess.sri ? 'cancel' : 'join';
  hook.variant = hook.variant || 'standard';
}

export function initAll(ctrl: LobbyController) {
  ctrl.data.hooks.forEach(init);
}

export function add(ctrl: LobbyController, hook) {
  init(hook);
  ctrl.data.hooks.push(hook);
}
export function setAll(ctrl: LobbyController, hooks: Hook[]) {
  ctrl.data.hooks = hooks;
  initAll(ctrl);
}
export function remove(ctrl: LobbyController, id) {
  ctrl.data.hooks = ctrl.data.hooks.filter(h => h.id !== id);
  ctrl.stepHooks.forEach(h => {
    if (h.id === id) h.disabled = true;
  });
}
export function syncIds(ctrl: LobbyController, ids) {
  ctrl.data.hooks = ctrl.data.hooks.filter(h => ids.includes(h.id));
}
export function find(ctrl: LobbyController, id) {
  return ctrl.data.hooks.find(h => h.id === id);
}

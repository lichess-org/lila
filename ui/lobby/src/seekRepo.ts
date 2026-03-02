import type LobbyController from './ctrl';

export function sort(ctrl: LobbyController) {
  ctrl.data.seeks.sort((a, b) => (a.rating > b.rating ? -1 : 1));
}

export function initAll(ctrl: LobbyController) {
  ctrl.data.seeks.forEach(seek => {
    seek.action = ctrl.me && seek.username === ctrl.me.username ? 'cancelSeek' : 'joinSeek';
  });
  sort(ctrl);
}

export function find(ctrl: LobbyController, id: string) {
  return ctrl.data.seeks.find(s => s.id === id);
}

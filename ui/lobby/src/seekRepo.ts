import LobbyController from './ctrl';
import { Seek } from './interfaces';

function order(a: Seek, b: Seek) {
  return a.rating > b.rating ? -1 : 1;
}

export function sort(ctrl: LobbyController) {
  ctrl.data.seeks.sort(order);
}

export function action(seek: Seek, ctrl: LobbyController): 'cancelSeek' | 'joinSeek' {
  return ctrl.data.me && seek.username === ctrl.data.me.username ? 'cancelSeek' : 'joinSeek';
}

export function find(ctrl: LobbyController, id: string) {
  return ctrl.data.seeks.find(s => s.id === id);
}

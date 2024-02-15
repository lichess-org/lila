import LobbyController from './ctrl';
import { Seek } from './interfaces';

const ratingOrder =
  (reverse: boolean) =>
  (a: Seek, b: Seek): number =>
    (a.rating > b.rating ? -1 : 1) * (reverse ? -1 : 1);

const timeOrder =
  (reverse: boolean) =>
  (a: Seek, b: Seek): number =>
    ((a.days || 365) > (b.days || 365) ? -1 : 1) * (reverse ? -1 : 1);

export function sort(ctrl: LobbyController, seeks: Seek[]) {
  const s = ctrl.sort;
  seeks.sort(s.startsWith('time') ? timeOrder(s !== 'time') : ratingOrder(s !== 'rating'));
}

export function find(ctrl: LobbyController, id: string) {
  return ctrl.data.seeks.find(s => s.id === id);
}

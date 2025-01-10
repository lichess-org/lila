import type TournamentController from './ctrl';

export function isIn(ctrl: TournamentController): boolean {
  return ctrl.data.me && !ctrl.data.me.withdraw;
}

export function willBePaired(ctrl: TournamentController): boolean {
  return isIn(ctrl) && !ctrl.data.pairingsClosed;
}

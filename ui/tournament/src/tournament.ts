import TournamentController from './ctrl';

export function isIn(ctrl: TournamentController) {
  return !!ctrl.data.me && !ctrl.data.me.withdraw;
}

export function willBePaired(ctrl: TournamentController) {
  return isIn(ctrl) && !ctrl.data.pairingsClosed;
}

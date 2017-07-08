import TournamentController from './ctrl';

export function isIn(ctrl: TournamentController) {
  return ctrl.data.me && !ctrl.data.me.withdraw;
}

export function myCurrentGameId(ctrl: TournamentController): string | undefined {
  if (!ctrl.opts.userId) return;
  const pairing = ctrl.data.pairings.find(function(p) {
    return p.s === 0 && (
      p.u[0].toLowerCase() === ctrl.opts.userId || p.u[1].toLowerCase() === ctrl.opts.userId
    );
  });
  return pairing ? pairing.id : undefined;
}

export function willBePaired(ctrl: TournamentController) {
  return isIn(ctrl) && !ctrl.data.pairingsClosed;
}

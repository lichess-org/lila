import { RoundData, Step } from './interfaces';

export function firstPly(d: RoundData): number {
  return d.steps[0].ply;
}

export function lastPly(d: RoundData): number {
  return d.steps[d.steps.length - 1].ply;
}

export function plyStep(d: RoundData, ply: number): Step {
  return d.steps[ply - firstPly(d)];
}

export function massage(d: RoundData): void {

  if (d.clock) {
    d.clock.showTenths = d.pref.clockTenths;
    d.clock.showBar = d.pref.clockBar;
  }

  if (d.correspondence) d.correspondence.showBar = d.pref.clockBar;

  if (['horde', 'crazyhouse'].indexOf(d.game.variant.key) !== -1) d.pref.showCaptured = false;

  if (d.expiration) d.expiration.movedAt = Date.now() - d.expiration.idleMillis;
};

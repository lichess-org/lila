import { RoundData, Step } from './interfaces';
import { countGhosts } from 'draughtsground/fen'
import { san2alg } from 'draughts'

export function mergeSteps(steps: Step[], coordSystem: number): Step[] {
  const mergedSteps: Step[] = new Array<Step>();
  if (steps.length == 0) return mergedSteps;
  else mergedSteps.push(addNotation(steps[0], coordSystem));

  if (steps.length == 1) return mergedSteps;

  for (let i = 1; i < steps.length; i++) {
    const step = steps[i - 1];
    if (step.captLen === undefined || step.captLen < 2 || step.ply < steps[i].ply) {
      // Captures split over multiple steps have the same ply. If a multicapture is reported in one step, the ply does increase
      mergedSteps.push(addNotation(steps[i], coordSystem));
    } else {
      const originalStep = steps[i];
      for (let m = 0; m < step.captLen - 1 && i + 1 < steps.length; m++) {
        if (m === 0)
          originalStep.uci = originalStep.uci.substr(0, 4);
        i++;
        mergeStep(originalStep, steps[i]);
      }
      if (countGhosts(originalStep.fen) > 0)
        originalStep.ply++;
      mergedSteps.push(addNotation(originalStep, coordSystem));
    }
  }
  return mergedSteps;
}

function addNotation(step: Step, coordSystem: number): Step {
  if (coordSystem) {
    step.alg = san2alg(step.san);
  }
  return step;
}

function mergeStep(originalStep: Step, mergeStep: Step) {
  originalStep.ply = mergeStep.ply
  originalStep.fen = mergeStep.fen;
  originalStep.san = originalStep.san.slice(0, originalStep.san.indexOf('x') + 1) + mergeStep.san.substr(mergeStep.san.indexOf('x') + 1);
  originalStep.uci = originalStep.uci + mergeStep.uci.substr(2, 2);
}

export function addStep(steps: Step[], newStep: Step, coordSystem: number): Step {
  if (steps.length == 0 || countGhosts(steps[steps.length - 1].fen) === 0)
    steps.push(newStep);
  else
    mergeStep(steps[steps.length - 1], newStep);
  if (countGhosts(steps[steps.length - 1].fen) > 0)
    steps[steps.length - 1].ply++;
  return addNotation(steps[steps.length - 1], coordSystem);
}

export function firstPly(d: RoundData): number {
  return d.steps[0].ply;
}

export function lastPly(d: RoundData): number {
  return d.steps[d.steps.length - 1].ply;
}

export function plyStep(d: RoundData, ply: number): Step {
  let index = ply - firstPly(d);
  //while (index + 1 < d.steps.length && d.steps[index + 1].ply == d.steps[index].ply)
  //    index++;
  return d.steps[index];
}

export function massage(d: RoundData): void {

  if (d.clock) {
    d.clock.showTenths = d.pref.clockTenths;
    d.clock.showBar = d.pref.clockBar;
  }

  if (d.correspondence) d.correspondence.showBar = d.pref.clockBar;

  if (d.expiration) d.expiration.movedAt = Date.now() - d.expiration.idleMillis;
};

import { ChartGame, AcplChart } from './interface';
import movetime from './movetime';
import acpl from './acpl';

export { type ChartGame, type AcplChart };

export function initModule(): ChartGame {
  return {
    acpl,
    movetime,
  };
}

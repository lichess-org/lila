import { ChartGame, AcplChart } from './interface';
import movetime from './movetime';
import acpl from './acpl';
import { gridColor, tooltipBgColor, fontFamily, maybeChart } from './common';

export { type ChartGame, type AcplChart };

export { gridColor, tooltipBgColor, fontFamily, maybeChart };

export function initModule(): ChartGame {
  return {
    acpl,
    movetime,
  };
}

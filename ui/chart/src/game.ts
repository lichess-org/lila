import { ChartGame, AcplChart } from './interface';
import movetime from './movetime';
import acpl from './acpl';
import { gridColor, tooltipBgColor, fontFamily } from './common';

export { type ChartGame, type AcplChart };

export { gridColor, tooltipBgColor, fontFamily };

export function initModule(): ChartGame {
  return {
    acpl,
    movetime,
  };
}

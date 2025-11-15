import type { ChartGame, AcplChart } from './interface';
import movetime from './movetime';
import acpl from './acpl';
import { gridColor, tooltipBgColor, fontFamily, maybeChart, colorSeries } from './index';

export { type ChartGame, type AcplChart };

export { gridColor, colorSeries, tooltipBgColor, fontFamily, maybeChart };

export function initModule(): ChartGame {
  return {
    acpl,
    movetime,
  };
}

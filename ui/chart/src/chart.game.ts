import { ChartGame, AcplChart } from './interface';
import movetime from './movetime';
import acpl from './acpl';
import { gridColor, tooltipBgColor, fontFamily, maybeChart, resizePolyfill, colorSeries } from './common';

export { type ChartGame, type AcplChart };

export { gridColor, colorSeries, tooltipBgColor, fontFamily, maybeChart, resizePolyfill };

export function initModule(): ChartGame {
  return {
    acpl,
    movetime,
  };
}

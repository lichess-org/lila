import { fidePlayerFollow } from './fidePlayerFollow';
import { fideRatingChart } from './fideRatingChart';

export function initModule(): void {
  fidePlayerFollow();
  fideRatingChart();
}

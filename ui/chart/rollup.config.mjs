import { multi } from '@build/rollupProject';

export default multi([
  {
    name: 'LichessChartRatingDistribution',
    input: 'src/ratingDistribution.ts',
    output: 'chart.ratingDistribution',
  },
  {
    name: 'LichessChartRatingHistory',
    input: 'src/ratingHistory.ts',
    output: 'chart.ratingHistory',
  },
  {
    name: 'LichessChartGame',
    input: 'src/game.ts',
    output: 'chart.game',
  },
]);

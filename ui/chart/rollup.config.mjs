import rollupProject from '@build/rollupProject';

export default rollupProject({
  ratingDistribution: {
    name: 'LichessChartRatingDistribution',
    input: 'src/ratingDistribution.ts',
    output: 'chart.ratingDistribution',
  },
  ratingHistory: {
    name: 'LichessChartRatingHistory',
    input: 'src/ratingHistory.ts',
    output: 'chart.ratingHistory',
  },
  game: {
    name: 'LichessChartGame',
    input: 'src/game.ts',
    output: 'chart.game',
  },
});

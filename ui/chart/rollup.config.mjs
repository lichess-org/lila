import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessChartCommon',
    input: 'src/common.ts',
    output: 'chart.common',
  },
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
  movetime: {
    name: 'LichessChartMovetime',
    input: 'src/movetime.ts',
    output: 'chart.movetime',
  },
});

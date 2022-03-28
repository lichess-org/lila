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
  movetime: {
    name: 'LichessChartMovetime',
    input: 'src/movetime.ts',
    output: 'chart.movetime',
  },
});

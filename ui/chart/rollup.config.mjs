import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessChartCommon',
    input: 'src/common.ts',
    output: 'chart.common',
  },
});

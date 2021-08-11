import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessInsight',
    input: 'src/main.ts',
    output: 'insight',
    external: ['highcharts'],
    globals: {
      highcharts: 'Highcharts',
    },
  },
});

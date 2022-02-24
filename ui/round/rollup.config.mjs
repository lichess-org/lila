import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessRound',
    input: 'src/main.ts',
    output: 'round',
  },
  nvui: {
    name: 'NVUI',
    input: 'src/plugins/nvui.ts',
    output: 'round.nvui',
  },
});

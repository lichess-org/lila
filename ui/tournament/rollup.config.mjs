import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessTournament',
    input: 'src/main.ts',
    output: 'tournament',
  },
});

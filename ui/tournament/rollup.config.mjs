import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessTournament',
    input: 'src/main.ts',
    output: 'tournament',
  },
});

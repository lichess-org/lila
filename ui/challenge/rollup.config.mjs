import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessChallenge',
    input: 'src/main.ts',
    output: 'challenge',
  },
});

import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessChallenge',
    input: 'src/main.ts',
    output: 'challenge',
  },
});

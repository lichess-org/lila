import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessSpeech',
    input: 'src/main.ts',
    output: 'speech',
  },
});

import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessChat',
    input: 'src/main.ts',
    output: 'chat',
  },
});

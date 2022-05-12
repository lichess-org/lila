import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessKeyboardMove',
    input: 'src/plugins/keyboardMove.ts',
    output: 'keyboardMove',
  },
});

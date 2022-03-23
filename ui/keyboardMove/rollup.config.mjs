import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessKeyboardMove',
    input: 'src/main.ts',
    output: 'keyboardMove',
  },
  keyboardMove: {
    name: 'KeyboardMove',
    input: 'src/plugins/keyboardMove.ts',
    output: 'keyboardMove.keyboardMove',
  },
});

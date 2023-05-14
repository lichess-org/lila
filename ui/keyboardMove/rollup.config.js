import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  keyboardMove: {
    name: 'KeyboardMove',
    input: 'src/plugins/keyboardMove.ts',
    output: 'lishogi.keyboardMove',
  },
});

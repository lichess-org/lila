import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessSpeech',
    input: 'src/main.ts',
    output: 'speech',
  },
});

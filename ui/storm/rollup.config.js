import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LishogiStorm',
    input: 'src/main.ts',
    output: 'lishogi.storm',
  },
});

import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  tv: {
    input: 'src/tv-embed.ts',
    output: 'lichess.tv.embed',
  },
});

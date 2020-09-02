import { execSync } from 'child_process';
import { rollupProject } from '@build/rollupProject';
import replace from '@rollup/plugin-replace';

export default rollupProject({
  main: {
    input: 'src/site.ts',
    output: 'lichess.site',
    plugins: [
      replace({
        __info__: JSON.stringify({
          date: new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00',
          commit: execSync('git rev-parse -q --short HEAD', {encoding: 'utf-8'}).trim(),
          message: execSync('git log -1 --pretty=%s', {encoding: 'utf-8'}).trim(),
        }),
      }),
    ],
  },
  tv: {
    input: 'src/tv-embed.ts',
    output: 'lichess.tv.embed',
  },
});

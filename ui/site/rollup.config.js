import { execSync } from 'child_process';
import { rollupProject } from '@build/rollupProject';
import copy from 'rollup-plugin-copy';
import replace from '@rollup/plugin-replace';

export default rollupProject({
  main: {
    input: 'src/site.ts',
    output: 'lichess.site',
    plugins: [
      copy({
        targets: [{
          src: [
            require.resolve('stockfish.js/stockfish.js'),
            require.resolve('stockfish.js/stockfish.wasm'),
            require.resolve('stockfish.js/stockfish.wasm.js'),
          ],
          dest: '../../public/vendor/stockfish.js',
        }, {
          src: [
            require.resolve('stockfish.wasm/stockfish.js'),
            require.resolve('stockfish.wasm/stockfish.wasm'),
            require.resolve('stockfish.wasm/stockfish.worker.js'),
          ],
          dest: '../../public/vendor/stockfish.wasm',
        }, {
          src: [
            require.resolve('stockfish-mv.wasm/stockfish.js'),
            require.resolve('stockfish-mv.wasm/stockfish.wasm'),
            require.resolve('stockfish-mv.wasm/stockfish.worker.js'),
          ],
          dest: '../../public/vendor/stockfish-mv.wasm',
        }],
      }),
      replace({
        __info__: JSON.stringify({
          date: new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00',
          commit: execSync('git rev-parse -q --short HEAD', {encoding: 'utf-8'}).trim(),
          message: execSync('git log -1 --pretty=%s', {encoding: 'utf-8'}).trim(),
        }),
      }),
    ],
  },
  tvEmbed: {
    input: 'src/tv-embed.ts',
    output: 'lichess.tv.embed',
  },
  puzzleEmbed: {
    input: 'src/puzzle-embed.ts',
    output: 'lichess.puzzle.embed',
  },
  user: {
    input: 'src/user.ts',
    output: 'lichess.user',
  },
  modUser: {
    input: 'src/mod-user.ts',
    output: 'lichess.mod.user',
  },
  clas: {
    input: 'src/clas.ts',
    output: 'lichess.clas',
  },
  coordinate: {
    input: 'src/coordinate.ts',
    output: 'lichess.coordinate',
  },
  captcha: {
    input: 'src/captcha.ts',
    output: 'lichess.captcha',
  },
  expandText: {
    input: 'src/expand-text.ts',
    output: 'lichess.expand-text',
  },
});

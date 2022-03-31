import { execSync } from 'child_process';
import { dirname } from 'path';
import { multi } from '@build/rollupProject';
import copy from 'rollup-plugin-copy';
import replace from '@rollup/plugin-replace';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);

export default multi([
  {
    input: 'src/site.ts',
    output: 'site',
    plugins: [
      copy({
        targets: [
          // hopscotch
          {
            src: require.resolve('hopscotch/dist/js/hopscotch.min.js'),
            dest: '../../public/vendor/hopscotch/dist/js',
          },
          {
            src: require.resolve('hopscotch/dist/css/hopscotch.min.css'),
            dest: '../../public/vendor/hopscotch/dist/css',
          },
          {
            src: dirname(require.resolve('hopscotch/package.json')) + '/dist/img/*',
            dest: '../../public/vendor/hopscotch/dist/img',
          },
          // highcharts
          {
            src: ['highcharts/highcharts.js', 'highcharts/highcharts-more.js', 'highcharts/highstock.js'].map(
              require.resolve
            ),
            dest: '../../public/vendor/highcharts-4.2.5',
          },
          // tagify
          {
            src: require.resolve('@yaireo/tagify/dist/tagify.min.js'),
            dest: '../../public/vendor/tagify',
          },
          // stockfish.js
          {
            src: ['stockfish.js/stockfish.js', 'stockfish.js/stockfish.wasm', 'stockfish.js/stockfish.wasm.js'].map(
              require.resolve
            ),
            dest: '../../public/vendor/stockfish.js',
          },
          // stockfish.wasm
          {
            src: [
              'stockfish.wasm/stockfish.js',
              'stockfish.wasm/stockfish.wasm',
              'stockfish.wasm/stockfish.worker.js',
            ].map(require.resolve),
            dest: '../../public/vendor/stockfish.wasm',
          },
          // stockfish-mv.wasm
          {
            src: [
              'stockfish-mv.wasm/stockfish.js',
              'stockfish-mv.wasm/stockfish.wasm',
              'stockfish-mv.wasm/stockfish.worker.js',
            ].map(require.resolve),
            dest: '../../public/vendor/stockfish-mv.wasm',
          },
          // stockfish-nnue.wasm
          {
            src: [
              'stockfish-nnue.wasm/stockfish.js',
              'stockfish-nnue.wasm/stockfish.wasm',
              'stockfish-nnue.wasm/stockfish.worker.js',
            ].map(require.resolve),
            dest: '../../public/vendor/stockfish-nnue.wasm',
          },
        ],
      }),
      replace({
        values: {
          __info__: JSON.stringify({
            date: new Date(new Date().toUTCString()).toISOString().split('.')[0] + '+00:00',
            commit: execSync('git rev-parse -q HEAD', { encoding: 'utf-8' }).trim(),
            message: execSync('git log -1 --pretty=%s', { encoding: 'utf-8' }).trim(),
          }),
        },
        preventAssignment: true,
      }),
    ],
  },
]);

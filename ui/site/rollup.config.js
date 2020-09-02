import { execSync } from 'child_process';
import { dirname } from 'path';
import { rollupProject } from '@build/rollupProject';
import copy from 'rollup-plugin-copy';
import replace from '@rollup/plugin-replace';

export default rollupProject({
  main: {
    input: 'src/site.ts',
    output: 'lichess.site',
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
            src: [
              'highcharts/highcharts.js',
              'highcharts/highcharts-more.js',
              'highcharts/highstock.js',
            ].map(require.resolve),
            dest: '../../public/vendor/highcharts-4.2.5',
          },
          // jquery-bar-rating
          {
            src: require.resolve('jquery-bar-rating/dist/jquery.barrating.min.js'),
            dest: '../../public/vendor/bar-rating/dist',
          },
          // stockfish.js
          {
            src: [
              'stockfish.js/stockfish.js',
              'stockfish.js/stockfish.wasm',
              'stockfish.js/stockfish.wasm.js',
            ].map(require.resolve),
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
        ],
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
});

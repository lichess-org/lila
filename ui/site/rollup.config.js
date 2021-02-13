import { execSync } from 'child_process';
import { dirname } from 'path';
import { rollupProject } from '@build/rollupProject';
import copy from 'rollup-plugin-copy';
import replace from '@rollup/plugin-replace';

export default rollupProject({
  main: {
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
    output: 'tv.embed',
  },
  puzzleEmbed: {
    input: 'src/puzzle-embed.ts',
    output: 'puzzle.embed',
  },
  analyseEmbed: {
    input: 'src/analyse-embed.ts',
    output: 'analysis-board.embed',
    name: 'analyseEmbed'
  },
  user: {
    input: 'src/user.ts',
    output: 'user',
  },
  modUser: {
    input: 'src/mod-user.ts',
    output: 'mod.user',
  },
  clas: {
    input: 'src/clas.ts',
    output: 'clas',
  },
  coordinate: {
    input: 'src/coordinate.ts',
    output: 'coordinate',
  },
  captcha: {
    input: 'src/captcha.ts',
    output: 'captcha',
  },
  expandText: {
    input: 'src/expand-text.ts',
    output: 'expand-text',
  },
  team: {
    input: 'src/team.ts',
    output: 'team',
    name: 'teamStart'
  },
  forum: {
    input: 'src/forum.ts',
    output: 'forum',
  },
  account: {
    input: 'src/account.ts',
    output: 'account',
  },
  coachForm: {
    input: 'src/coach-form.ts',
    output: 'coach-form',
  },
  challengePage: {
    input: 'src/challenge-page.ts',
    output: 'challenge-page',
    name: 'challengePageStart'
  },
  checkout: {
    input: 'src/checkout.ts',
    output: 'checkout',
    name: 'checkoutStart'
  },
  login: {
    input: 'src/login.ts',
    output: 'login',
    name: 'loginSignup'
  },
  teamBattleForm: {
    input: 'src/team-battle-form.ts',
    output: 'team-battle-form'
  },
  dgt: {
    input: 'src/dgt/dgt.ts',
    output: 'dgt',
    name: 'lichessDgt'
  },
  gameSearch: {
    input: 'src/gameSearch.ts',
    output: 'game-search'
  }
});

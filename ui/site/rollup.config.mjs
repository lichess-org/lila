import { execSync } from 'child_process';
import { dirname } from 'path';
import rollupProject from '@build/rollupProject';
import copy from 'rollup-plugin-copy';
import replace from '@rollup/plugin-replace';
import { createRequire } from 'module';

const require = createRequire(import.meta.url);

const suppressThisIsUndefined = (warning, warn) => warning.code !== 'THIS_IS_UNDEFINED' && warn(warning);

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
  tvEmbed: {
    input: 'src/tvEmbed.ts',
    output: 'tvEmbed',
  },
  puzzleEmbed: {
    input: 'src/puzzleEmbed.ts',
    output: 'puzzle.embed',
  },
  analyseEmbed: {
    input: 'src/analyseEmbed.ts',
    output: 'analysisBoard.embed',
    name: 'analyseEmbed',
  },
  user: {
    input: 'src/user.ts',
    output: 'user',
  },
  clas: {
    input: 'src/clas.ts',
    output: 'clas',
  },
  captcha: {
    input: 'src/captcha.ts',
    output: 'captcha',
  },
  expandText: {
    input: 'src/expandText.ts',
    output: 'expandText',
  },
  lpv: {
    input: 'src/lpv.ts',
    output: 'lpv',
    name: 'LilaLpv',
  },
  lpvEmbed: {
    input: 'src/lpv.embed.ts',
    output: 'lpv.embed',
    name: 'LpvEmbed',
  },
  team: {
    input: 'src/team.ts',
    output: 'team',
    name: 'teamStart',
  },
  forum: {
    input: 'src/forum.ts',
    output: 'forum',
  },
  account: {
    input: 'src/account.ts',
    output: 'account',
  },
  passwordComplexity: {
    input: 'src/passwordComplexity.ts',
    output: 'passwordComplexity',
    name: 'passwordComplexity',
  },
  coachForm: {
    input: 'src/coachForm.ts',
    output: 'coach.form',
  },
  coachShow: {
    input: 'src/coachShow.ts',
    output: 'coach.show',
  },
  challengePage: {
    input: 'src/challengePage.ts',
    output: 'challengePage',
    name: 'challengePageStart',
  },
  checkout: {
    input: 'src/checkout.ts',
    output: 'checkout',
    name: 'checkoutStart',
  },
  plan: {
    input: 'src/plan.ts',
    output: 'plan',
    name: 'plan',
  },
  login: {
    input: 'src/login.ts',
    output: 'login',
    name: 'loginSignup',
  },
  teamBattleForm: {
    input: 'src/teamBattleForm.ts',
    output: 'teamBattleForm',
  },
  tourForm: {
    input: 'src/tourForm.ts',
    output: 'tourForm',
    onwarn: suppressThisIsUndefined,
  },
  gameSearch: {
    input: 'src/gameSearch.ts',
    output: 'gameSearch',
  },
  userComplete: {
    input: 'src/userComplete.ts',
    output: 'userComplete',
    name: 'UserComplete',
    onwarn: suppressThisIsUndefined,
  },
  infiniteScroll: {
    input: 'src/infiniteScroll.ts',
    output: 'infiniteScroll',
    name: 'InfiniteScroll',
    onwarn: suppressThisIsUndefined,
  },
  flatpickr: {
    input: 'src/flatpickr.ts',
    output: 'flatpickr',
    name: 'LichessFlatpickr',
    onwarn: suppressThisIsUndefined,
  },
  appeal: {
    input: 'src/appeal.ts',
    output: 'appeal',
  },
  publicChats: {
    input: 'src/publicChats.ts',
    output: 'publicChats',
  },
  contact: {
    input: 'src/contact.ts',
    output: 'contact',
  },
  userGamesDownload: {
    input: 'src/userGamesDownload.ts',
    output: 'userGamesDownload',
  },
  tvGames: {
    input: 'src/tvGames.ts',
    output: 'tvGames',
  },
  ublog: {
    input: 'src/ublog.ts',
    output: 'ublog',
  },
  ublogForm: {
    input: 'src/ublogForm.ts',
    output: 'ublogForm',
  },
  speech: {
    name: 'LichessSpeech',
    input: 'src/speech.ts',
    output: 'speech',
  },
});

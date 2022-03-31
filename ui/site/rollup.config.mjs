import { multi } from '@build/rollupProject';

export default multi([
  {
    input: 'src/tvEmbed.ts',
    output: 'tvEmbed',
  },
  {
    input: 'src/puzzleEmbed.ts',
    output: 'puzzle.embed',
  },
  {
    input: 'src/analyseEmbed.ts',
    output: 'analysisBoard.embed',
    name: 'analyseEmbed',
  },
  {
    input: 'src/user.ts',
    output: 'user',
  },
  {
    input: 'src/modUser.ts',
    output: 'mod.user',
  },
  {
    input: 'src/inquiry.ts',
    output: 'inquiry',
  },
  {
    input: 'src/modGames.ts',
    output: 'mod.games',
  },
  {
    input: 'src/modSearch.ts',
    output: 'mod.search',
  },
  {
    input: 'src/clas.ts',
    output: 'clas',
  },
  {
    input: 'src/captcha.ts',
    output: 'captcha',
  },
  {
    input: 'src/expandText.ts',
    output: 'expandText',
  },
  {
    input: 'src/team.ts',
    output: 'team',
    name: 'teamStart',
  },
  {
    input: 'src/forum.ts',
    output: 'forum',
  },
  {
    input: 'src/account.ts',
    output: 'account',
  },
  {
    input: 'src/passwordComplexity.ts',
    output: 'passwordComplexity',
    name: 'passwordComplexity',
  },
  {
    input: 'src/coachForm.ts',
    output: 'coach.form',
  },
  {
    input: 'src/coachShow.ts',
    output: 'coach.show',
  },
  {
    input: 'src/challengePage.ts',
    output: 'challengePage',
    name: 'challengePageStart',
  },
  {
    input: 'src/checkout.ts',
    output: 'checkout',
    name: 'checkoutStart',
  },
  {
    input: 'src/plan.ts',
    output: 'plan',
    name: 'planStart',
  },
  {
    input: 'src/login.ts',
    output: 'login',
    name: 'loginSignup',
  },
  {
    input: 'src/teamBattleForm.ts',
    output: 'teamBattleForm',
  },
  {
    input: 'src/tourForm.ts',
    output: 'tourForm',
  },
  {
    input: 'src/gameSearch.ts',
    output: 'gameSearch',
  },
  {
    input: 'src/userComplete.ts',
    output: 'userComplete',
    name: 'UserComplete',
  },
  {
    input: 'src/infiniteScroll.ts',
    output: 'infiniteScroll',
    name: 'InfiniteScroll',
  },
  {
    input: 'src/flatpickr.ts',
    output: 'flatpickr',
    name: 'LichessFlatpickr',
  },
  {
    input: 'src/teamAdmin.ts',
    output: 'team.admin',
  },
  {
    input: 'src/appeal.ts',
    output: 'appeal',
  },
  {
    input: 'src/publicChats.ts',
    output: 'publicChats',
  },
  {
    input: 'src/contact.ts',
    output: 'contact',
  },
  {
    input: 'src/userGamesDownload.ts',
    output: 'userGamesDownload',
  },
  {
    input: 'src/modActivity.ts',
    output: 'modActivity',
    name: 'modActivity',
  },
  {
    input: 'src/tvGames.ts',
    output: 'tvGames',
  },
  {
    input: 'src/ublog.ts',
    output: 'ublog',
  },
  // {
  //   input: 'src/ublogForm.ts',
  //   output: 'ublogForm',
  // },
]);

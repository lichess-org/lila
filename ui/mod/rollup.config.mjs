import rollupProject from '@build/rollupProject';

export default rollupProject({
  activity: {
    input: 'src/activity.ts',
    output: 'mod.activity',
    name: 'LichessModActivity',
  },
  user: {
    input: 'src/user.ts',
    output: 'mod.user',
  },
  inquiry: {
    input: 'src/inquiry.ts',
    output: 'mod.inquiry',
  },
  games: {
    input: 'src/games.ts',
    output: 'mod.games',
  },
  search: {
    input: 'src/search.ts',
    output: 'mod.search',
  },
  teamAdmin: {
    input: 'src/teamAdmin.ts',
    output: 'team.admin',
  },
});

import rollupProject from '@build/rollupProject';

export default rollupProject({
  activity: {
    input: 'src/activity.ts',
    output: 'modActivity',
    name: 'modActivity',
  },
  user: {
    input: 'src/user.ts',
    output: 'mod.user',
  },
  inquiry: {
    input: 'src/inquiry.ts',
    output: 'inquiry',
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

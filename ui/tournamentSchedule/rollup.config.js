import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessTournamentSchedule',
    input: 'src/main.ts',
    output: 'tournament.schedule',
  },
});

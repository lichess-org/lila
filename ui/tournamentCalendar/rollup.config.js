import { rollupProject } from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'LichessTournamentCalendar',
    input: 'src/main.ts',
    output: 'tournament.calendar',
  },
});

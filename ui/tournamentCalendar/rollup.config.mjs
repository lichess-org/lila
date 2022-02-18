import rollupProject from '@build/rollupProject';

export default rollupProject({
  main: {
    name: 'NewChessTournamentCalendar',
    input: 'src/main.ts',
    output: 'tournament.calendar',
  },
});

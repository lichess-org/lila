import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiTournamentCalendar",
    input: "src/main.ts",
    output: "lishogi.tournamentCalendar",
  },
});

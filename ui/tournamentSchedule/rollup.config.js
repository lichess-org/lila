import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiTournamentSchedule",
    input: "src/main.ts",
    output: "lishogi.tournamentSchedule",
  },
});

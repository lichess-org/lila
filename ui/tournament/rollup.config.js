import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiTournament",
    input: "src/main.ts",
    output: "lishogi.tournament",
  },
});

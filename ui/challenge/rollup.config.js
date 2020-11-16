import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiChallenge",
    input: "src/main.ts",
    output: "lishogi.challenge",
  },
});

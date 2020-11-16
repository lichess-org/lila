import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiDasher",
    input: "src/main.ts",
    output: "lishogi.dasher",
  },
});

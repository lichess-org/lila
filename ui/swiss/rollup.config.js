import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiSwiss",
    input: "src/main.ts",
    output: "lishogi.swiss",
  },
});

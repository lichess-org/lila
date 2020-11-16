import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiCli",
    input: "src/main.ts",
    output: "lishogi.cli",
  },
});

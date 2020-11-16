import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiEditor",
    input: "src/main.ts",
    output: "lishogi.editor",
  },
});

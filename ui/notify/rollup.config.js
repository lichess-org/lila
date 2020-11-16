import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiNotify",
    input: "src/main.ts",
    output: "lishogi.notify",
  },
});

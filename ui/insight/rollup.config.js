import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiInsight",
    input: "src/main.js",
    output: "lishogi.insight",
    js: true,
  },
});

import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiSimul",
    input: "src/main.js",
    output: "lishogi.simul",
    js: true,
  },
});

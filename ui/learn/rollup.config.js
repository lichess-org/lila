import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiLearn",
    input: "src/main.js",
    output: "lishogi.learn",
    js: true,
  },
});

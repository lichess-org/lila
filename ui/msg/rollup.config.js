import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiMsg",
    input: "src/main.ts",
    output: "lishogi.msg",
  },
});

import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiChat",
    input: "src/main.ts",
    output: "lishogi.chat",
  },
});

import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiAnalyse",
    input: "src/main.ts",
    output: "lishogi.analyse",
  },
  nvui: {
    input: "src/plugins/nvui.ts",
    output: "lishogi.analyse.nvui",
  },
});

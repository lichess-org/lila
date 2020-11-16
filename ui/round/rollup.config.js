import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiRound",
    input: "src/main.ts",
    output: "lishogi.round",
  },
  keyboardMove: {
    name: "KeyboardMove",
    input: "src/plugins/keyboardMove.ts",
    output: "lishogi.round.keyboardMove",
  },
  nvui: {
    name: "NVUI",
    input: "src/plugins/nvui.ts",
    output: "lishogi.round.nvui",
  },
});

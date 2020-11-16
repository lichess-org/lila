import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiSpeech",
    input: "src/main.ts",
    output: "lishogi.speech",
  },
});

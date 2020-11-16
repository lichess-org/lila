import { rollupProject } from "@build/rollupProject";

export default rollupProject({
  main: {
    name: "LishogiLobby",
    input: "src/main.ts",
    output: "lishogi.lobby",
  },
});

/// <reference types="emscripten" />
import process from "process";
import fs from "fs";
import path from "path";
import readline from "readline";
import YaneuraOu = require("./lib/yaneuraou.k-p");
import { YaneuraOuModule } from "./lib/yaneuraou.module";

const USI_BOOK_FILE = process.env.USI_BOOK_FILE;
const USI_EVAL_FILE = process.env.USI_EVAL_FILE;

async function runRepl(yaneuraou: YaneuraOuModule) {
  const iface = readline.createInterface({ input: process.stdin });
  for await (const command of iface) {
    if (command == "quit") {
      break;
    }
    yaneuraou.postMessage(command);
  }
  yaneuraou.terminate();
}

async function main(argv: string[]) {
  const wasmBinary = await fs.promises.readFile(path.join(__dirname, "./lib/yaneuraou.k-p.wasm"));
  const yaneuraou: YaneuraOuModule = await YaneuraOu({ wasmBinary });
  const FS = yaneuraou.FS;
  if (USI_BOOK_FILE) {
    const buffer = await fs.promises.readFile(USI_BOOK_FILE);
    FS.writeFile(`/${path.basename(USI_BOOK_FILE)}`, buffer);
    yaneuraou.postMessage("setoption name BookDir value .");
    yaneuraou.postMessage(`setoption name BookFile value ${path.basename(USI_BOOK_FILE)}`);
  }
  const USE_EVAL_FILE = true;
  if (USE_EVAL_FILE && USI_EVAL_FILE) {
    const buffer = await fs.promises.readFile(USI_EVAL_FILE);
    const filebase = path.basename(USI_EVAL_FILE);
    FS.writeFile(`/${filebase}`, buffer);
    yaneuraou.postMessage("setoption name EvalDir value .");
    yaneuraou.postMessage(`setoption name EvalFile value ${filebase}`);
  }
  if (argv.length > 0) {
    const commands = argv.join(" ").split(" , ");
    for (const command of commands) {
      yaneuraou.postMessage(command);
    }
    yaneuraou.terminate();
    return;
  }
  runRepl(yaneuraou);
}

if (require.main === module) {
  main(process.argv.slice(2));
}

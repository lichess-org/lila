import { parentPort, workerData } from 'worker_threads';
import ts from 'typescript';

export interface WorkerData {
  projects: string[];
  index: number;
  watch: boolean;
}

export interface Message {
  type: 'error' | 'ok' | 'busy';
  index: number;
  data?: any;
}

export interface ErrorMessage extends Message {
  type: 'error';
  data: {
    project: string;
    code: number;
    text: string;
    file: string;
    line: number;
    col: number;
  };
}

let builder: ts.SolutionBuilder<ts.EmitAndSemanticDiagnosticsBuilderProgram>|undefined;
const { projects, watch, index } = workerData as WorkerData;

if (watch) {
  const host = ts.createSolutionBuilderWithWatchHost(ts.sys, undefined, diagnostic, undefined, watchEvent);
  builder = ts.createSolutionBuilderWithWatch(host, projects, { preserveWatchOutput: true });

  builder.build();

} else {
  const host = ts.createSolutionBuilderHost(ts.sys, undefined, diagnostic);
  builder = ts.createSolutionBuilder(host, projects, {});

  const code = builder.build();

  if (code === 0) parentPort?.postMessage({ type: 'ok', index });
  process.exit(code);
}

function watchEvent(err: ts.Diagnostic) {

  if (err.code === 6194) // 0 errors found
    parentPort?.postMessage({ type: 'ok', index });

  else if (err.code === 6032) // File change detected
    parentPort?.postMessage({ type: 'busy', index });
}

function diagnostic(err: ts.Diagnostic) {
  let file, line, col;
  if (err.file) {
    file = err.file.fileName;
    if (err.start !== undefined) {
      const pos = err.file.getLineAndCharacterOfPosition(err.start);
      line = pos.line + 1;
      col = pos.character + 1;
    }
  }
  parentPort?.postMessage({
    type: 'error',
    index,
    data: { code: err.code, text: err.messageText, file, line, col },
  });
}

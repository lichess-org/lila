import { Chessground } from '@lichess-org/chessground';
import { Chess } from 'chessops/chess';
import { makeFen } from 'chessops/fen';
import { makeSquare } from 'chessops/util';

import { memoize } from 'lib';
import { type Feature, features } from 'lib/device';
import { domDialog } from 'lib/view/dialog';

import type { VisionWorkerResponse } from './editor.vision.worker';

export async function initModule({ file }: { file: File }): Promise<FEN> {
  const statusDialog = await domDialog({
    htmlText: $html`
      <p><strong class="status">Downloading ChessQueries Lite...</strong></p>
      <div class="mini-board cg-wrap is2d standard" style="width: 240px;height:240px"></div>`,
    class: 'vision-status',
    modal: false,
    noCloseButton: true,
  });
  const cg = Chessground(statusDialog.view.querySelector<HTMLElement>('.mini-board')!, {
    viewOnly: true,
    coordinates: false,
    drawable: { enabled: false, visible: false },
  });
  const randomBoard = () => {
    const chess = Chess.default();
    let move: { from: number; to: number } = { from: 0, to: 0 };
    for (let i = 0; i < 15 + Math.floor(Math.random() * 100); i++) {
      const moves = [...chess.allDests()].flatMap(([from, dests]) => [...dests].map(to => ({ from, to })));
      move = moves[Math.floor(Math.random() * moves.length)];
      chess.play(move);
      if (chess.isEnd()) break;
    }
    cg.set({ fen: makeFen(chess.toSetup()), lastMove: [makeSquare(move.from), makeSquare(move.to)] });
  };
  const interval = setInterval(randomBoard, 200);
  statusDialog.show();
  try {
    const [worker, image] = await Promise.all([
      getReader(),
      window.createImageBitmap(file, { imageOrientation: 'from-image' }),
    ]);
    return await new Promise<FEN>((resolve, reject) => {
      statusDialog.view.querySelector<HTMLElement>('.status')!.textContent = 'Processing...';
      worker.onmessage = event => {
        const response = event.data as VisionWorkerResponse;
        if (response.type === 'placement') resolve(response.boardFen);
        else if (response.type === 'error') reject(new Error(response.message));
      };
      worker.onerror = event => reject(new Error(event.message));
      worker.postMessage({ type: 'image', image }, [image]);
    });
  } finally {
    clearInterval(interval);
    statusDialog.close();
  }
}

type VisionFeature = 'webgpu' | Feature;

interface VisionReader {
  model: string;
  runtime: string;
  wasm: string;
  providers: ('webgpu' | 'wasm')[];
  requires: VisionFeature[];
}

const getReader = memoize<Promise<Worker>>(async () => {
  const boardReaders: VisionReader[] = [
    // {
    //   model: 'lifat/vision/chessq-lite-v4-fp16.onnx',
    //   runtime: 'npm/onnxruntime-web/ort.webgpu.min.mjs',
    //   wasm: 'npm/onnxruntime-web/ort-wasm-simd-threaded.asyncify',
    //   providers: ['webgpu', 'wasm'],
    //   requires: ['wasm', 'webgpu', 'dynamicImportFromWorker'],
    // },
    {
      model: 'lifat/vision/chessq-lite-v4-int8.onnx',
      runtime: 'npm/onnxruntime-web/ort.wasm.min.mjs',
      wasm: 'npm/onnxruntime-web/ort-wasm-simd-threaded',
      providers: ['wasm'],
      requires: ['wasm', 'dynamicImportFromWorker'],
    },
  ] as const;

  // can't detect webgpu synchronously in device.ts, so that detection lives here.
  const deviceFeatures: VisionFeature[] = [...features()];
  if ('gpu' in navigator && (await navigator.gpu.requestAdapter())) deviceFeatures.push('webgpu');
  const reader = boardReaders.find(reader =>
    reader.requires.every(requirement => deviceFeatures.includes(requirement)),
  );
  if (!reader) throw new Error('device not supported');

  return new Promise<Worker>((resolve, reject) => {
    const worker = new Worker(
      site.asset.url(site.asset.jsModule('editor.vision.worker'), { documentOrigin: true }),
      { type: 'module' },
    );
    worker.onmessage = event => {
      const response = event.data as VisionWorkerResponse;
      if (response.type === 'ready') resolve(worker);
      else if (response.type === 'error') reject(new Error(response.message));
    };
    worker.onerror = event => reject(new Error(event.message));
    worker.postMessage({
      type: 'load',
      runtimeUrl: site.asset.url(reader.runtime),
      modelUrl: site.asset.url(reader.model),
      wasmUrl: site.asset.url(`${reader.wasm}.wasm`),
      wasmModuleUrl: site.asset.url(`${reader.wasm}.mjs`),
      executionProviders: reader.providers,
      threads: Math.min(4, navigator.hardwareConcurrency || 1),
    });
  });
});

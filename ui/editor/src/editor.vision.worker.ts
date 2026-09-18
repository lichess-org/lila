import type * as ort from 'onnxruntime-web';

// an onnx runtime wrapper of the ChessQueriesLite vision model for chessboard recognition
// https://huggingface.co/joelseytre/chessqueries
// https://arxiv.org/abs/2608.30762

export type VisionWorkerResponse = VisionReadyResponse | VisionPlacementResponse | VisionErrorResponse;

interface VisionLoadRequest {
  type: 'load';
  runtimeUrl: string;
  modelUrl: string;
  wasmUrl: string;
  wasmModuleUrl: string;
  executionProviders: ('webgpu' | 'wasm')[];
  threads: number;
}

interface VisionImageRequest {
  type: 'image';
  image: ImageBitmap;
}

type VisionWorkerRequest = VisionLoadRequest | VisionImageRequest;

interface VisionReadyResponse {
  type: 'ready';
}

interface VisionPlacementResponse {
  type: 'placement';
  boardFen: string;
}

interface VisionErrorResponse {
  type: 'error';
  message: string;
}

let runtime: typeof ort | undefined;
let session: ort.InferenceSession | undefined;

const RESOLUTION = 644;
const CHANNEL_SIZE = RESOLUTION * RESOLUTION;
const CLASS_SYMBOLS = '.PNBRQKpnbrqk';
const MEAN = [0.485, 0.456, 0.406] as const; // these are standard ImageNet rgb normalization constants
const STD = [0.229, 0.224, 0.225] as const; // these are standard ImageNet rgb normalization constants

const worker = globalThis as unknown as {
  crossOriginIsolated: boolean;
  onmessage: ((event: MessageEvent<VisionWorkerRequest>) => void) | null;
  postMessage(message: VisionWorkerResponse): void;
};

worker.onmessage = event =>
  handleMessage(event.data).catch(error =>
    worker.postMessage({
      type: 'error',
      message: error instanceof Error ? error.message : String(error),
    }),
  );

async function handleMessage(request: VisionWorkerRequest): Promise<void> {
  if (request.type === 'load') {
    runtime = (await import(request.runtimeUrl)) as typeof ort;
    const threads =
      worker.crossOriginIsolated && typeof SharedArrayBuffer === 'function' ? request.threads : 1;
    runtime.env.wasm.numThreads = Math.max(1, threads);
    runtime.env.wasm.proxy = false;
    runtime.env.wasm.wasmPaths = { wasm: request.wasmUrl, mjs: request.wasmModuleUrl };
    session = await runtime.InferenceSession.create(request.modelUrl, {
      executionProviders: request.executionProviders,
      graphOptimizationLevel: 'all',
    });
    worker.postMessage({ type: 'ready' });
    return;
  }

  if (!runtime || !session) throw new Error('Vision model is not loaded');
  let tensor: ort.Tensor;
  try {
    tensor = makeTensor(runtime, request.image);
  } finally {
    request.image.close();
  }
  const output = await session.run({ image: tensor });
  const logits = output.logits;
  if (!logits || logits.dims.join(',') !== '1,64,13') {
    // 1 input image, 64 squares, 13 classes (including empty)
    throw new Error('unexpected output');
  }
  worker.postMessage({ type: 'placement', boardFen: boardFen(logits.data as Float32Array) });
}

function makeTensor(runtime: typeof ort, image: ImageBitmap): ort.Tensor {
  const canvas = new OffscreenCanvas(RESOLUTION, RESOLUTION);
  const context = canvas.getContext('2d', { alpha: false, willReadFrequently: true });
  if (!context) throw new Error('canvas unavailable');
  context.imageSmoothingEnabled = true;
  context.imageSmoothingQuality = 'high';
  context.drawImage(image, 0, 0, RESOLUTION, RESOLUTION);

  // normalize pixel values first
  const pixels = context.getImageData(0, 0, RESOLUTION, RESOLUTION).data;
  const input = new Float32Array(3 * CHANNEL_SIZE);
  for (let pixel = 0, rgba = 0; pixel < CHANNEL_SIZE; pixel++, rgba += 4) {
    input[pixel] = (pixels[rgba] / 255 - MEAN[0]) / STD[0];
    input[CHANNEL_SIZE + pixel] = (pixels[rgba + 1] / 255 - MEAN[1]) / STD[1];
    input[2 * CHANNEL_SIZE + pixel] = (pixels[rgba + 2] / 255 - MEAN[2]) / STD[2];
  }
  return new runtime.Tensor('float32', input, [1, 3, RESOLUTION, RESOLUTION]);
}

function boardFen(logits: Float32Array): string {
  const labels = new Uint8Array(64);
  for (let square = 0; square < 64; square++) {
    const offset = square * 13;
    let best = 0;
    for (let piece = 1; piece < 13; piece++) {
      if (logits[offset + piece] > logits[offset + best]) best = piece;
    }
    labels[square] = best;
  }

  const ranks: string[] = [];
  for (let rank = 0; rank < 8; rank++) {
    let encoded = '';
    let empty = 0;
    for (const label of labels.slice(rank * 8, rank * 8 + 8)) {
      if (label === 0) empty++;
      else {
        if (empty) encoded += empty;
        empty = 0;
        encoded += CLASS_SYMBOLS[label];
      }
    }
    ranks.push(encoded + (empty || ''));
  }
  return ranks.join('/');
}

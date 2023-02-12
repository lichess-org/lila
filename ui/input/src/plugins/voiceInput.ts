import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { RecognizerMessage } from 'vosk-browser/dist/interfaces';

export type VoiceListener = (command: string) => void;
export interface VoiceCtrl {
  start: () => void;
  stop: () => void;
  ready: () => boolean;
  status: () => string;
}
export interface VoiceOpts {
  impl: 'vanilla' | 'worklet';
  listener: VoiceListener;
}

const wasmSource = 'vendor/vosk/vosk.js';
const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';

const speechLookup = new Map<string, string>([
  ['a', 'a'],
  ['b', 'b'],
  ['c', 'c'],
  ['d', 'd'],
  ['e', 'e'],
  ['f', 'f'],
  ['g', 'g'],
  ['h', 'h'],

  ['one', '1'],
  ['two', '2'],
  ['three', '3'],
  ['four', '4'],
  ['five', '5'],
  ['six', '6'],
  ['seven', '7'],
  ['eight', '8'],

  ['rook', 'R'],
  ['knight', 'N'],
  ['bishop', 'B'],
  ['queen', 'Q'],
  ['king', 'K'],

  ['takes', 'x'],
]);

export const ctrl = (options: VoiceOpts): VoiceCtrl => {
  const opts = options;
  let sampleRate = 48000; // go ahead try to change it, apparently nobody cares
  let voiceModel: Model;
  let mediaStream: MediaStream;
  let recognizer: KaldiRecognizer;
  let audioContext: AudioContext;
  let ready = false;
  let status = 'not loaded';

  return {
    start: () => {
      if (!audioContext) load();
      else audioContext.resume();
    },
    stop: () => audioContext?.suspend(),
    ready: () => ready,
    status: () => status,
  };

  function processResult(message: RecognizerMessage) {
    if ('result' in message && 'text' in message.result) {
      console.log(`We heard: ${message.result.text}`);
      const split = message.result.text.split(' ');
      const command = split
        .map(word => speechLookup.get(word))
        .filter(word => word !== undefined)
        .join('');
      opts.listener(command);
    }
  }

  async function load() {
    try {
      status = 'fetching wasm';
      await lichess.loadScript(wasmSource);
      status = 'fetching model';
      await initShared(lichess.assetUrl(modelSource));
      await (opts.impl == 'vanilla' ? vanillaProcessor : workletProcessor)();
      console.log(`Voice input using ${opts.impl} engine with ${modelSource}`);
      ready = true;
    } catch (e) {
      console.log('Voice module init failed', e);
      status = `${JSON.stringify(e).slice(0, 40)}...`;
    }
  }

  async function initShared(modelUrl: string) {
    const devices = await navigator.mediaDevices.enumerateDevices();
    console.log(navigator.mediaDevices.getSupportedConstraints());
    devices.forEach(d => {
      if (d.kind == 'audioinput') console.log(d);
    });
    voiceModel = await createModel(modelUrl);

    mediaStream = await navigator.mediaDevices.getUserMedia({
      video: false,
      audio: {
        sampleRate: sampleRate,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
      },
    });
    sampleRate = mediaStream.getAudioTracks()[0].getSettings().sampleRate || sampleRate;
    audioContext = new AudioContext({
      sampleRate: sampleRate,
    });

    recognizer = new voiceModel.KaldiRecognizer(sampleRate, JSON.stringify([...speechLookup.keys()]));
    recognizer.on('result', processResult);
  }

  //========================== works ok on all but deprecated ==============================

  async function vanillaProcessor() {
    // createScriptProcessor was deprecated in 2014
    const recognizerNode = audioContext.createScriptProcessor(4096, 1, 1);
    recognizerNode.onaudioprocess = e => recognizer.acceptWaveform(e.inputBuffer);
    audioContext.createMediaStreamSource(mediaStream).connect(recognizerNode);
    recognizerNode.connect(audioContext.destination); // (shouldn't need this but it do)
  }

  //========================= preferred impl but safari bugged =============================

  async function workletProcessor() {
    const debugChannel = new MessageChannel(); // safari can't log in a worker
    debugChannel.port1.onmessage = (e: MessageEvent) => console.log(e.data);

    const channel = new MessageChannel();
    voiceModel.registerPort(channel.port1);

    await audioContext.audioWorklet.addModule(lichess.assetUrl('compiled/voskProcessor.js'));

    const voskNode = new AudioWorkletNode(audioContext, 'vosk-processor', {
      channelCount: 1,
      numberOfInputs: 1,
      numberOfOutputs: 1,
    });
    voskNode.port.postMessage({ action: 'register', recognizerId: recognizer.id }, [channel.port2, debugChannel.port2]);
    voskNode.connect(audioContext.destination);

    const sourceNode = audioContext.createMediaStreamSource(mediaStream);
    sourceNode.connect(voskNode);
  }
};

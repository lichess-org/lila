import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { ServerMessageResult, ServerMessagePartialResult } from 'vosk-browser/dist/interfaces';
import { RecognizerOpts } from '../interfaces';

const LOG_LEVEL = -1; // -1 errors only. 0 includes warnings, 3 is just insane
const BUF_SIZE = 8192;

// Based on the original implementation by Sam 'Spammy' Ezeh who found Vosk-browser and first
// got it working in lichess.
//
// do not touch this unless you are familiar with vosk-browser, emscripten, the vosk api,
// and kaldi.
class Recognizer {
  kaldi: KaldiRecognizer;
  node: ScriptProcessorNode;
  constructor(kaldi: KaldiRecognizer, node: ScriptProcessorNode) {
    this.kaldi = kaldi;
    this.node = node;
  }
  close() {
    this.kaldi.remove();
    if (this.node.onaudioprocess) this.node.onaudioprocess = null;
    this.node.disconnect();
  }
}

export default (window as any).LichessVoicePlugin.vosk = new (class {
  voiceModel?: Model;
  recs = new Map<Voice.ListenMode, Recognizer>();
  mode?: Voice.ListenMode = 'full';
  language?: string;

  get lang() {
    return this.voiceModel && this.language;
  }

  async initModel(url: string, lang: string): Promise<void> {
    this.voiceModel?.terminate();
    this.voiceModel = undefined;
    this.recs.forEach(r => r.close());
    this.recs.clear();
    this.language = lang;
    this.voiceModel = await createModel(url, LOG_LEVEL);
  }

  async setRecognizer(opts: RecognizerOpts): Promise<AudioNode | undefined> {
    if (!opts.vocab || !opts.vocab.length || !this.voiceModel) {
      this.close(opts.mode);
      return;
    }

    const kaldi = new this.voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify(opts.vocab));

    // fun fact - createScriptProcessor was deprecated in 2014
    const node = opts.audioCtx.createScriptProcessor(BUF_SIZE, 1, 1);
    if (opts.mode === 'partial')
      kaldi.on('partialresult', (msg: ServerMessagePartialResult) => {
        if (msg.result.partial.length < 2 || this.mode !== 'partial') return;
        opts.broadcast(msg.result.partial, opts.mode, 1000);
      });
    else
      kaldi.on('result', (msg: ServerMessageResult) => {
        if (msg.result.text.length < 2 || this.mode !== 'full') return;
        opts.broadcast(msg.result.text, opts.mode, 3000);
      });

    if (this.mode === opts.mode) node.onaudioprocess = e => kaldi.acceptWaveform(e.inputBuffer);
    this.close(opts.mode);

    if (LOG_LEVEL >= -1) console.info(`Creating ${opts.mode} recognizer with`, opts.vocab);
    this.recs.set(opts.mode, new Recognizer(kaldi, node));
    return node;
  }

  ready(): boolean {
    return this.voiceModel !== undefined;
  }

  setMode(newMode: Voice.ListenMode): void {
    if (this.mode === newMode && this.recs.get(newMode)?.node?.onaudioprocess) return;
    const oldRec = this.mode ? this.recs.get(this.mode) : undefined;
    if (oldRec?.node.onaudioprocess) {
      this.mode = undefined;
      oldRec.node.onaudioprocess = null;
      if (newMode === 'full') oldRec.kaldi.retrieveFinalResult();
    }
    const r = this.recs.get(newMode);
    if (r?.node) r.node.onaudioprocess = e => r.kaldi.acceptWaveform(e.inputBuffer);
    this.mode = newMode;
  }

  stop(): void {
    this.recs.forEach(rec => {
      if (rec.node?.onaudioprocess) rec.node.onaudioprocess = null;
    });
  }

  close(mode: Voice.ListenMode) {
    const rec = this.recs.get(mode);
    if (rec && LOG_LEVEL >= -1) console.info(`Closing ${mode} recognizer`);
    rec?.close();
    this.recs.delete(mode);
  }
})();

//========================== works on all but safari ==========================
/*
async function workletProcessor(audioCtx: AudioContext, kaldi: KaldiRecognizer): Promise<AudioNode> {
  const debugChannel = new MessageChannel(); // safari can't log with a worker
  debugChannel.port1.onmessage = (e: MessageEvent) => console.log(e.data);

  const channel = new MessageChannel();
  voiceModel.registerPort(channel.port1);

  await audioCtx.audioWorklet.addModule(lichess.assetUrl('compiled/voskWorklet.js'));

  const voskNode = new AudioWorkletNode(audioCtx, 'vosk-worklet', {
    channelCount: 1,
    numberOfInputs: 1,
    numberOfOutputs: 1,
  });
  voskNode.port.postMessage({ action: 'register', recognizerId: kaldi.id }, [channel.port2, debugChannel.port2]);
  return voskNode;
}
*/
//============================= voskWorklet.ts ================================
// this goes in src/plugins, and you need to copy dist/voskWorklet.js over to
// public/compiled as a copy build step in package.json's lichess object.
/*
export class VoskWorklet extends AudioWorkletProcessor {
  constructor() {
    super();
    this.port.onmessage = this.onmessage.bind(this);
  }
  id: string;
  outPort: MessagePort;
  debugPort: MessagePort;
  start = Date.now(); // debug shite
  sampleCount = 0;

  error(msg: any) {
    this.debugPort.postMessage(msg);
  }

  onmessage(event: MessageEvent) {
    if (event.data.action === 'register') {
      this.id = event.data.recognizerId;
      this.outPort = event.ports[0];
      this.debugPort = event.ports[1];
    }
  }
  process(inputs: Float32Array[][]) {
    this.sampleCount += inputs[0][0].length;
    if (this.sampleCount >= sampleRate) {
      const effectiveRate = Math.round(sampleRate / (this.start - Date.now()) / 1000);
      // sampleRate is an audioworklet global
      // this block should execute once every second.  On safari it does not.
      // my iphone seems to get data at 32kHz, and my macbook gets it at 22kHz
      // while all the queries against the MediaStream return 48k.
      // this is why i have sad.
      if (effectiveRate < 47000 || effectiveRate > 49000) this.error(`Effective sample rate: ${effectiveRate}`);
      this.sampleCount -= sampleRate;
    }
    const denormalized = inputs[0][0].map(x => x * 32768);
    try {
      this.outPort?.postMessage(
        {
          action: 'audioChunk',
          data: denormalized, // buf,
          recognizerId: this.id,
          sampleRate,
        },
        { transfer: [denormalized.buffer] }
      );
    } catch (e) {
      this.error(e);
    }
    return true;
  }
}

registerProcessor('vosk-worklet', VoskWorklet);
*/

import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { ServerMessageResult, ServerMessagePartialResult } from 'vosk-browser/dist/interfaces';
import { RecognizerOpts } from '../interfaces';

const LOG_LEVEL = -1; // -1 errors only. 0 includes warnings, 3 is just insane
const BUF_SIZE = 8192;

// Based on the original implementation by Sam 'Spammy' Ezeh who found Vosk-browser and first
// got it working in lichess.
//
// don't touch unless you are familiar with vosk-browser, the vosk api, and kaldi.
// web audio connections outside of script processor callbacks are not handled here.
// the audio context graph must be constructed and maintained at a higher level

export default (window as any).LichessVoicePlugin.vosk = new (class {
  voiceModel?: Model;
  kaldiNodes: { full?: KaldiNode; partial?: KaldiNode } = {};
  mode: Voice.ListenMode = 'full';
  lang?: string;

  async initModel(url: string, lang: string): Promise<void> {
    this.voiceModel?.terminate();
    this.voiceModel = undefined;
    this.closeKaldi();
    this.voiceModel = await createModel(url, LOG_LEVEL);
    this.lang = lang;
  }

  setRecognizer(opts: RecognizerOpts): AudioNode | undefined {
    if (!opts.vocab || !opts.vocab.length || !this.voiceModel) {
      this.closeKaldi(opts.mode);
      return;
    }

    const kaldi = new this.voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify(opts.vocab));

    // fun fact - createScriptProcessor was deprecated in 2014
    const node = opts.audioCtx.createScriptProcessor(BUF_SIZE, 1, 1);
    if (opts.mode === 'partial') {
      kaldi.on('partialresult', (msg: ServerMessagePartialResult) => {
        if (msg.result.partial.length < 1 || this.mode !== 'partial') return;
        opts.broadcast(msg.result.partial, opts.mode, 1000);
      });
    } else {
      kaldi.on('result', (msg: ServerMessageResult) => {
        if (msg.result.text.length < 1 || this.mode !== 'full') return;
        opts.broadcast(msg.result.text, opts.mode, 3000);
      });
    }
    this.closeKaldi(opts.mode);

    if (LOG_LEVEL >= -1) console.info(`Creating ${opts.mode} recognizer with`, opts.vocab);

    this.kaldiNodes[opts.mode] = new KaldiNode(kaldi, node, this.mode === opts.mode);
    return node;
  }

  isReady(lang?: string): boolean {
    return this.voiceModel !== undefined && (!lang || lang === this.lang);
  }

  setMode(newMode: Voice.ListenMode): void {
    if (this.kaldiNodes[newMode]?.enabled) return;
    if (newMode !== this.mode) {
      this.kaldiNodes[this.mode]?.setEnabled(false);
      if (newMode === 'full') this.kaldiNodes['partial']?.kaldi.retrieveFinalResult(); // flush lattice
    }
    this.kaldiNodes[newMode]?.setEnabled(true);
    this.mode = newMode;
  }

  stop(): void {
    this.kaldiNodes.full?.setEnabled(false);
    this.kaldiNodes.partial?.setEnabled(false);
  }

  closeKaldi(mode?: Voice.ListenMode) {
    if (mode) this.kaldiNodes[mode]?.kill();
    else {
      this.kaldiNodes.full?.kill();
      this.kaldiNodes.partial?.kill();
    }
  }
})();

class KaldiNode {
  kaldi: KaldiRecognizer;
  node: ScriptProcessorNode;

  constructor(kaldi: KaldiRecognizer, node: ScriptProcessorNode, enable = false) {
    this.kaldi = kaldi;
    this.node = node;
    this.setEnabled(enable);
  }

  kill() {
    this.setEnabled(false);
    this.kaldi.remove();
  }

  get enabled(): boolean {
    return this?.node.onaudioprocess != null; // != is intentional
  }

  setEnabled(isEnabled: boolean) {
    if (isEnabled) {
      if (this.node.onaudioprocess) return;
      this.node.onaudioprocess = e => this.kaldi.acceptWaveform(e.inputBuffer);
    } else if (this.node.onaudioprocess) this.node.onaudioprocess = null;
  }
}

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

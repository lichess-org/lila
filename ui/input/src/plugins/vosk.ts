import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { KaldiOpts, WordResult } from '../interfaces';

let kaldi: KaldiRecognizer;
let voiceModel: Model;

export default (window as any).LichessVoice = {
  initModel: async function (url: string): Promise<void> {
    voiceModel = await createModel(url);
  },

  initKaldi: async function (opts: KaldiOpts): Promise<AudioNode> {
    if (kaldi) {
      kaldi.remove();
    }
    kaldi = new voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify(opts.keys));
    kaldi.on('result', (message: any) => {
      if (!('result' in message && 'text' in message.result) || message.result.text == '') return;

      const text = message.result.text as string;

      opts.broadcast(text, 'command', message.result.result as WordResult, 3000);
    });

    return opts.impl == 'vanilla' ? vanillaProcessor(opts.audioCtx) : workletProcessor(opts.audioCtx);
  },
};

//========================== works ok on all but deprecated ==============================

function vanillaProcessor(audioCtx: AudioContext): AudioNode {
  // createScriptProcessor was deprecated in 2014
  const voskNode = audioCtx.createScriptProcessor(4096, 1, 1);

  voskNode.onaudioprocess = (e: any) => kaldi.acceptWaveform(e.inputBuffer);
  return voskNode;
}

//========================= preferred impl but safari bugged =============================

async function workletProcessor(audioCtx: AudioContext): Promise<AudioNode> {
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

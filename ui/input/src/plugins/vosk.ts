import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { VoskOpts } from '../interfaces';

let kaldi: KaldiRecognizer;
let voiceModel: Model;

export default (window as any).LichessVoice = {
  init: async (opts: VoskOpts): Promise<AudioNode> => {
    if (!kaldi) {
      voiceModel = await createModel(opts.url);

      kaldi = new voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify([...opts.speechMap.keys()]));
      kaldi.on('result', (message: any) => {
        if ('result' in message && 'text' in message.result) opts.broadcast(message.result.text as string, true, 3000);
      });
    }
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

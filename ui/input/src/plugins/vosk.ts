import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { VoskOpts } from '../interfaces';

let impl: 'vanilla' | 'worklet';
let kaldi: KaldiRecognizer;
let voiceModel: Model;

export default (window as any).LichessVoice = {
  resume: async (audioCtx: AudioContext): Promise<AudioNode> => {
    return impl == 'vanilla' ? vanillaProcessor(audioCtx) : workletProcessor(audioCtx);
  },

  init: async (opts: VoskOpts): Promise<AudioNode> => {
    try {
      voiceModel = await createModel(opts.url);

      kaldi = new voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify([...opts.speechMap.keys()]));
      kaldi.on('result', (message: any) => {
        if ('result' in message && 'text' in message.result) {
          opts.listen(message.result.text as string, true);
        }
      });
      impl = opts.impl || 'vanilla';
      return opts.impl == 'vanilla' ? vanillaProcessor(opts.audioCtx) : await workletProcessor(opts.audioCtx);
    } catch (e) {
      opts.listen(`${JSON.stringify(e).slice(0, 40)}...`, false);
      throw e;
    }
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

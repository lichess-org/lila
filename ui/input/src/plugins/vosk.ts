import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { ServerMessageResult, ServerMessagePartialResult } from 'vosk-browser/dist/interfaces';
import { KaldiOpts } from '../interfaces';

let kaldi: KaldiRecognizer;
let voiceModel: Model;

export default (window as any).LichessVoice = {
  initModel: async function (url: string): Promise<void> {
    voiceModel = await createModel(url);
  },

  initKaldi: async function (opts: KaldiOpts): Promise<AudioNode> {
    kaldi?.remove();
    kaldi = new voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify(opts.keys));
    kaldi.setWords(true);
    kaldi.on('result', (msg: ServerMessageResult) => {
      if (msg.result.text.length < 2) return; // can't do anything with this
      opts.broadcast(msg.result.text, 'phrase', msg.result.result, 3000);
    });
    kaldi.on('partialresult', (msg: ServerMessagePartialResult) => {
      if (msg.result.partial.length < 2) return; // can't do anything with this
      //opts.broadcast(msg.result.partial, 'partial', undefined, 1000);
    });
    const voskNode = opts.audioCtx.createScriptProcessor(4096, 1, 1);

    voskNode.onaudioprocess = (e: any) => kaldi.acceptWaveform(e.inputBuffer);
    return voskNode;
  },
};

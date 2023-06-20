import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { ServerMessageResult, ServerMessagePartialResult } from 'vosk-browser/dist/interfaces';
import { Selector, Selectable } from 'common/selector';
import { RecognizerOpts } from './interfaces';

const LOG_LEVEL = -1; // -1 errors only. 0 includes warnings, 3 is just insane

// Thanks to Sam 'Spammy' Ezeh who found Vosk-browser and first got it working in lichess.

export default (window as any).LichessVoicePlugin.vosk = new (class {
  voiceModel?: Model;
  recs = new Selector<KaldiRec>();
  lang?: string;

  async initModel(url: string, lang: string): Promise<void> {
    this.voiceModel?.terminate();
    this.voiceModel = undefined;
    this.recs.delete();
    this.voiceModel = await createModel(url, LOG_LEVEL);
    this.lang = lang;
  }

  initRecognizer(opts: RecognizerOpts): AudioNode | undefined {
    if (!opts.words?.length || !this.voiceModel) {
      this.recs.delete(opts.recId);
      return;
    }
    const kaldi = new this.voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify(opts.words));

    // buffer size under 100ms for timely partial results, 200ms for full results
    const bufSize = 2 ** Math.ceil(Math.log2(opts.audioCtx.sampleRate / (opts.partial ? 16 : 8)));

    // fun fact - createScriptProcessor was deprecated in 2014
    const node = opts.audioCtx.createScriptProcessor(bufSize, 1, 1);
    if (opts.partial)
      kaldi.on('partialresult', (msg: ServerMessagePartialResult) => {
        if (msg.result.partial.length < 1 || !this.recs.selected?.partial) return;
        opts.broadcast(msg.result.partial, 'partial', 0);
      });
    else
      kaldi.on('result', (msg: ServerMessageResult) => {
        if (msg.result.text.length < 1 || this.recs.selected?.partial) return;
        opts.broadcast(msg.result.text, 'full', 3000);
      });

    if (LOG_LEVEL >= -1)
      console.info(
        `Created ${opts.audioCtx.sampleRate.toFixed()}Hz recognizer '${
          opts.recId
        }' with buffer size ${bufSize}`,
        opts.words
      );

    this.recs.set(opts.recId, new KaldiRec(kaldi, node, opts.partial));
    return node;
  }

  isLoaded(lang?: string): boolean {
    return this.voiceModel !== undefined && (!lang || lang === this.lang);
  }

  select(recId: string | false): void {
    this.recs.select(recId);
  }
})();

class KaldiRec implements Selectable {
  kaldi: KaldiRecognizer;
  node: ScriptProcessorNode;
  partial: boolean;

  constructor(kaldi: KaldiRecognizer, node: ScriptProcessorNode, partial: boolean) {
    this.kaldi = kaldi;
    this.node = node;
    this.partial = partial;
  }
  select() {
    if (this.node.onaudioprocess) return;
    this.node.onaudioprocess = e => this.kaldi.acceptWaveform(e.inputBuffer);
  }
  deselect() {
    if (!this.node.onaudioprocess) return;
    this.kaldi.retrieveFinalResult(); // flush lattice
    this.node.onaudioprocess = null;
  }
  close() {
    this.deselect();
    this.kaldi.remove();
  }
}

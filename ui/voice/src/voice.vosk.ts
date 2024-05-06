import { KaldiRecognizer, createModel, Model } from 'vosk-browser';
import { ServerMessageResult, ServerMessagePartialResult } from 'vosk-browser/dist/interfaces';
import { RecognizerOpts, VoskModule } from './interfaces';

// IMPORTANT: We can't have code splitting here and I don't want a separate esbuild pass.
// Do not import code, just paste it in if needed.

const LOG_LEVEL = -1; // -1 errors only. 0 includes warnings, 3 is just insane

export function initModule(): VoskModule {
  const recs = new Selector<KaldiRec>();
  let voiceModel: Model;
  let lang: string;

  return {
    initModel,
    initRecognizer,
    isLoaded,
    select,
  };

  async function initModel(url: string, language: string): Promise<void> {
    voiceModel?.terminate();
    recs.delete();
    voiceModel = await createModel(url, LOG_LEVEL);
    lang = language;
  }

  function initRecognizer(opts: RecognizerOpts): AudioNode | undefined {
    if (!opts.words?.length || !voiceModel) {
      recs.delete(opts.recId);
      return;
    }
    const kaldi = new voiceModel.KaldiRecognizer(opts.audioCtx.sampleRate, JSON.stringify(opts.words));

    // buffer size under 100ms for timely partial results, 200ms for full results
    const bufSize = 2 ** Math.ceil(Math.log2(opts.audioCtx.sampleRate / (opts.partial ? 16 : 8)));

    // fun fact - createScriptProcessor was deprecated in 2014
    const node = opts.audioCtx.createScriptProcessor(bufSize, 1, 1);
    if (opts.partial)
      kaldi.on('partialresult', (msg: ServerMessagePartialResult) => {
        if (msg.result.partial.length > 0) opts.broadcast(msg.result.partial, 'partial', 0);
      });
    else
      kaldi.on('result', (msg: ServerMessageResult) => {
        if (msg.result.text.length > 0) opts.broadcast(msg.result.text, 'full', 3000);
      });

    if (LOG_LEVEL >= -1)
      console.info(
        `Created ${opts.audioCtx.sampleRate.toFixed()}Hz recognizer '${
          opts.recId
        }' with buffer size ${bufSize}`,
        opts.words,
      );

    recs.set(opts.recId, new KaldiRec(kaldi, node, opts.partial));
    return node;
  }

  function isLoaded(language?: string): boolean {
    return voiceModel !== undefined && (!language || language === lang);
  }

  function select(recId: string | false): void {
    recs.select(recId);
  }
}

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

export interface Selectable<C = any> {
  select?: (ctx?: C) => void;
  deselect?: (ctx?: C) => void;
  close?: (ctx?: C) => void;
}

export class Selector<T extends Selectable, C = any> {
  group = new Map<string, T>();
  context?: C;
  key: string | false = false;

  set ctx(ctx: any) {
    if (this.context === ctx) return;
    this.selected?.deselect?.(this.context);
    this.context = ctx;
    this.selected?.select?.(this.context);
  }

  get selected(): T | undefined {
    return this.key ? this.group.get(this.key) : undefined;
  }

  select(key: string | false) {
    if (this.key) {
      if (this.key === key) return;
      this.selected?.deselect?.(this.context);
    }
    this.key = key;
    this.selected?.select?.(this.context);
  }

  get(key: string): T | undefined {
    return this.group.get(key);
  }

  set(key: string, val: T) {
    const reselect = this.key === key;
    this.close(key);
    this.group.set(key, val);
    if (reselect) this.select(key);
  }

  close(key?: string) {
    if (key === undefined) {
      for (const k of this.group.keys()) this.close(k);
      return;
    }
    if (key === this.key) {
      this.group.get(key)?.deselect?.(this.context);
      this.key = false;
    }
    this.group.get(key)?.close?.(this.context);
  }

  delete(key?: string) {
    this.close(key);
    key ? this.group.delete(key) : this.group.clear();
  }
}

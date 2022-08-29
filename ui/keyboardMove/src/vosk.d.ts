export declare class KaldiRecognizer {
  constructor(model: Model, sampleRate: number);
  constructor(model: Model, sampleRate: number, grammar: string);
  public SetWords(words: boolean): void;
  public AcceptWaveform(address: number, length: number): boolean;
  public Result(): string;
  public PartialResult(): string;
  public FinalResult(): string;
  public delete(): void;
}

declare class ModelKaldiRecognizer extends KaldiRecognizer {
  constructor(sampleRate: number);
  constructor(sampleRate: number, grammar: string);

  public on(event: RecognizerEvent, listener: (message: RecognizerMessage) => void): void;
  public acceptWaveform(buffer: AudioBuffer): void;
}

export declare class Model {
  constructor(path: string);
  public delete(): void;
  KaldiRecognizer: typeof ModelKaldiRecognizer;
}

type VoskInstance = {
  createModel: (modelUrl: string, logLevel?: number) => Promise<Model>;
};

declare global {
  export const Vosk: VoskInstance;
}

export type RecognizerMessage = ServerMessagePartialResult | ServerMessageResult;

export interface ServerMessageError {
  event: 'error';
  recognizerId?: string;
  error: string;
}

export interface ServerMessageResult {
  event: 'result';
  recognizerId: string;
  result: {
    result: Array<{
      conf: number;
      start: number;
      end: number;
      word: string;
    }>;
    text: string;
  };
}
export interface ServerMessagePartialResult {
  event: 'partialresult';
  recognizerId: string;
  result: {
    partial: string;
  };
}

export type RecognizerEvent = RecognizerMessage['event'];

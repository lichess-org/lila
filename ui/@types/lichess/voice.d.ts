// implementation: file://./../../site/src/component/mic.ts

declare namespace Voice {
  export type MsgType = 'full' | 'partial' | 'status' | 'error' | 'stop' | 'start';
  export type ListenMode = 'full' | 'partial';
  export type Listener = (msgText: string, msgType: MsgType) => void;

  export interface Microphone {
    setLang(language: string): void;

    getMics(): Promise<MediaDeviceInfo[]>;
    setMic(micId: string): void;

    initRecognizer(
      words: string[],
      also?: {
        recId?: string; // = 'default' if not provided
        partial?: boolean; // = false
        listener?: Listener; // = undefined
        listenerId?: string; // = recId (specify for multiple listeners on same recId)
      },
    ): void;
    setRecognizer(recId: string): void;

    addListener(
      listener: Listener,
      also?: {
        recId?: string; // = 'default'
        listenerId?: string; // = recId
      },
    ): void;
    removeListener(listenerId: string): void;
    setController(listener: Listener): void; // for status display, indicators, etc
    stopPropagation(): void; // interrupt broadcast propagation on current rec (for modal interactions)

    start(listen?: boolean): Promise<void>; // listen = true if not provided, if false just initialize
    stop(): void; // stop listening/downloading/whatever
    pause(): void;
    resume(): void;

    readonly isListening: boolean;
    readonly isBusy: boolean; // are we downloading, extracting, or loading?
    readonly status: string; // status display for setController listener
    readonly recId: string; // get current recognizer
    readonly micId: string;
    readonly lang: string; // defaults to 'en'
  }
}

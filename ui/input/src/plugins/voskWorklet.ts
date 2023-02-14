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
    this.debugPort.postMessage(msg); // for console log
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
      // that is why i have a sadz.
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

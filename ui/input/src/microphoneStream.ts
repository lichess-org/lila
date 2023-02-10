import { Readable } from 'readable-stream';

export default class MicrophoneStream extends Readable {
  public context: AudioContext;
  public audioInput: MediaStreamAudioSourceNode;
  private stream: MediaStream;
  private recorder: ScriptProcessorNode;
  private recording = true;
  constructor() {
    super({ objectMode: true });

    this.context = new AudioContext();
    this.recorder = this.context.createScriptProcessor(1024, 1, 1); // outputChannels 1 for old chrome
    this.recorder.connect(this.context.destination);

    setTimeout(() => {
      this.emit('format', {
        channels: 1,
        bitDepth: 32,
        sampleRate: this.context.sampleRate,
        signed: true,
        float: true,
      });
    }, 0);
  }

  public setStream(stream: MediaStream): void {
    this.stream = stream;
    this.audioInput = this.context.createMediaStreamSource(stream);
    this.audioInput.connect(this.recorder);
    const recorderProcess = (e: AudioProcessingEvent) => {
      if (this.recording) {
        this.push(e.inputBuffer);
      }
    };
    this.recorder.onaudioprocess = recorderProcess;
  }

  public pauseRecording(): void {
    this.recording = false;
  }

  public playRecording(): void {
    this.recording = true;
  }

  public stop(): void {
    if (this.context.state === 'closed') {
      return;
    }
    try {
      this.stream.getTracks()[0].stop();
    } catch (ex) {
      console.log(ex);
    }
    this.recorder.disconnect();
    if (this.audioInput) {
      this.audioInput.disconnect();
    }
    try {
      this.context.close(); // returns a promise;
    } catch (ex) {
      console.log(ex);
    }
    this.recording = false;
    this.push(null);
    this.emit('close');
  }

  public _read(): void {}

  public static toRaw(chunk: Buffer): Float32Array {
    return new Float32Array(chunk.buffer);
  }
}

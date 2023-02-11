import { Duplex, DuplexOptions, Writable, Readable } from 'readable-stream';
//import MicrophoneStream from '../microphoneStream';
import { KaldiRecognizer, createModel } from 'vosk-browser';

const speechLookup = new Map<string, string>([
  ['a', 'a'],
  ['b', 'b'],
  ['c', 'c'],
  ['d', 'd'],
  ['e', 'e'],
  ['f', 'f'],
  ['g', 'g'],
  ['h', 'h'],

  // NATO phonetic alphabet
  ['alpha', 'a'],
  ['bravo', 'b'],
  ['charlie', 'c'],
  ['delta', 'd'],
  ['echo', 'e'],
  ['foxtrot', 'f'],
  ['golf', 'g'],
  ['hotel', 'h'],

  ['one', '1'],
  ['two', '2'],
  ['three', '3'],
  ['four', '4'],
  ['five', '5'],
  ['six', '6'],
  ['seven', '7'],
  ['eight', '8'],

  ['rook', 'R'],
  ['knight', 'N'],
  ['bishop', 'B'],
  ['queen', 'Q'],
  ['king', 'K'],

  ['takes', 'x'],
  ['check', ''],

  ['castle', 'o-o'],
  ['short castle', 'o-o'],
  ['long castle', 'o-o-o'],

  // Command words
  ['next', 'next'],
  ['continue', 'next'],
  ['next puzzle', 'next'],
  ['clock', 'clock'],
  ['draw', 'draw'],
  ['offer draw', 'draw'],
  ['accept draw', 'draw'],
  ['resign', 'resign'],
  ['upvote', 'upv'],
  ['downvote', 'downv'],
]);

const audioBucket = new Writable({
  write: function (_, __, callback) {
    callback();
  },
  objectMode: true,
  decodeStrings: false,
});

interface SubmitOpts {
  isTrusted: boolean;
  force?: boolean;
  yourMove?: boolean;
}
type Submit = (v: string, submitOpts: SubmitOpts) => void;
export class AudioStreamer extends Duplex {
  constructor(public recognizer: KaldiRecognizer, options?: DuplexOptions) {
    super(options);
  }

  public _write(chunk: AudioBuffer, _: any, callback: any) {
    const buffer = chunk.getChannelData(0);
    if (this.recognizer && buffer.byteLength > 0) {
      this.recognizer.acceptWaveform(chunk);
    }
    callback();
  }
}
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

export function loadVosk(submit: Submit, isEnabled: () => boolean) {
  async function initialise() {
    const model = await createModel(lichess.assetUrl('vendor/vosk/model.tar.gz'));
    const recognizer = new model.KaldiRecognizer(16000, JSON.stringify([...speechLookup.keys()]));
    recognizer.on('result', message => {
      if ('result' in message)
        if ('text' in message.result) {
          console.log(`We heard: ${message.result.text}`);
          const split = message.result.text.split(' ');
          const command = split
            .map(word => speechLookup.get(word))
            .filter(word => word !== undefined)
            .join('');
          if (isEnabled()) submit(command, { force: true, isTrusted: true });
        }
    });

    const mediaStream = await navigator.mediaDevices.getUserMedia({
      video: false,
      audio: {
        echoCancellation: true,
        noiseSuppression: true,
      },
    });

    const micStream = new MicrophoneStream();

    micStream.setStream(mediaStream);

    const audioStreamer = new AudioStreamer(recognizer, {
      objectMode: true,
    });
    micStream?.unpipe(audioBucket);
    micStream?.pipe(audioStreamer);
  }
  initialise();
}

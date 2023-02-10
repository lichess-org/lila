import { Duplex, DuplexOptions, Writable } from 'readable-stream';
import MicrophoneStream from '../microphoneStream';
import { KaldiRecognizer, createModel } from 'vosk-browser';
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

const speechLookup = new Map<string, string>([
  ['a', 'a'],
  ['b', 'b'],
  ['c', 'c'],
  ['d', 'd'],
  ['e', 'e'],
  ['f', 'f'],
  ['g', 'g'],
  ['h', 'h'],

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
    /*recognizer.on('partialresult', message => {
      if ('result' in message)
        if ('partial' in message.result) console.log(`Partial result: ${message.result.partial}`);
    });*/

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

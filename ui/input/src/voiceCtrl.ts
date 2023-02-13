import { VoiceCtrl, VoiceListener } from './interfaces';

const speechMap = new Map<string, string>([
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

export const modelSource = 'vendor/vosk/model-en-us-0.15.tar.gz';

export class VoiceCtrlImpl implements VoiceCtrl {
  audioContext: AudioContext;
  mediaStream: MediaStream;
  voskStatus = 'not loaded';
  voskReady = false;
  micRecording = false;
  listeners = new Set<VoiceListener>();

  addListener = (listener: VoiceListener) => this.listeners.add(listener);

  removeListener = (listener: VoiceListener) => this.listeners.delete(listener);

  ready = () => this.voskReady;
  status = () => this.voskStatus;
  recording = () => this.micRecording;
  stop = () => {
    this.audioContext?.suspend();
    this.micRecording = false;
  };
  start(): Promise<boolean> {
    if (!this.audioContext) return this.load();
    else this.audioContext.resume();
    return Promise.resolve(true);
  }
  async load(): Promise<boolean> {
    if (this.audioContext && this.voskReady) return Promise.resolve(true);
    const fakeModelUrl = `https://lichess.org/${modelSource}`;
    const sandboxPath = `/vosk/${fakeModelUrl.replace(/[\W]/g, '_')}`;
    const downloadPromise = this.downloadModelTo(sandboxPath);
    await lichess.loadModule('input.vosk');
    this.mediaStream = await navigator.mediaDevices.getUserMedia({
      video: false,
      audio: {
        sampleRate: sampleRate,
        channelCount: 1,
        echoCancellation: true,
        noiseSuppression: true,
      },
    });
    sampleRate = this.mediaStream.getAudioTracks()[0].getSettings().sampleRate || sampleRate;
    this.audioContext = new AudioContext({
      sampleRate: sampleRate,
    });
    if (await downloadPromise) await window.LichessVoice({ speechMap, impl: 'vanilla', ctrl: this, url: fakeModelUrl });
    return (this.micRecording = this.voskReady);
  }
  listen(text: string) {
    this.voskStatus = text;
    const command = text
      .split(' ')
      .flatMap(word => speechMap.get(word))
      .filter(word => word !== undefined)
      .join('');
    this.listeners.forEach(listener => listener(command));
  }
  downloadModelTo(sandboxPath: string): Promise<boolean> {
    // we need to trigger the model download here and put it in sandbox filesystem
    // at this path to trick the webassembly to use ours.
    const assetUrl = lichess.assetUrl(modelSource);
    assetUrl;
    sandboxPath;
    return Promise.resolve(true);
  }
}

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

export class VoiceCtrlImpl implements VoiceCtrl {
  audioContext: AudioContext;
  mediaStream: MediaStream;

  voskStatus = 'not loaded';
  voskReady = false;
  listeners = new Set<VoiceListener>();

  addListener = (listener: VoiceListener) => this.listeners.add(listener);
  removeListener = (listener: VoiceListener) => this.listeners.delete(listener);
  ready = () => this.voskReady;
  status = () => this.voskStatus;
  stop = () => this.audioContext?.suspend();
  start() {
    if (!this.audioContext)
      lichess.loadModule('input.vosk').then(async () => {
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
        window.LichessVoice({speechMap, impl: 'vanilla', ctrl: this});
      });
    else this.audioContext.resume();
  }

  listen(text: string) {
    this.voskStatus = text;
    const command = text
      .split(' ')
      .flatMap(word => speechMap.get(word))
      .filter(word => word !== undefined)
      .join('');
    this.listeners.forEach(l => l(command));
  }
}
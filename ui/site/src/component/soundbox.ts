import { storage } from './storage';

class SoundBox {

  sounds = new Map(); // The loaded sounds and their instances

  volume = storage.make('sound-volume');

  loadOggOrMp3 = (name: string, path: string) => 
    this.sounds.set(name, new window.Howl({
      src: ['ogg', 'mp3'].map(ext => `${path}.${ext}`)
    }));

  play(name: string, volume: number = 1) {
    const doPlay = () => {
      this.sounds.get(name).volume(volume * this.getVolume());
      this.sounds.get(name).play();
    };
    if (window.Howler.ctx.state == "suspended") window.Howler.ctx.resume().then(doPlay);
    else doPlay();
  };

  setVolume = this.volume.set;

  getVolume = () => {
    // garbage has been stored here by accident (e972d5612d)
    const v = parseFloat(this.volume.get() || '');
    return v >= 0 ? v : 0.7;
  }
}

const soundBox = new SoundBox;

export default soundBox;

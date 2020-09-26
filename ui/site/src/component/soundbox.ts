import { storage } from './storage';

const soundBox = new class {

  sounds = new Map(); // The loaded sounds and their instances

  volume = storage.make('sound-volume');

  loadOggOrMp3 = (name: string, path: string) => 
    this.sounds.set(name, new window.Howl({
      src: ['ogg', 'mp3'].map(ext => `${path}.${ext}`)
    }));

  play(name: string) {
    const doPlay = () => this.sounds.get(name).volume(this.getVolume()).play();
    if (window.Howler.ctx.state == "suspended") window.Howler.ctx.resume().then(doPlay);
    else doPlay();
  };

  setVolume = this.volume.set;

  getVolume = () => {
    // garbage has been stored here by accident (e972d5612d)
    const v = parseFloat(this.volume.get() || '');
    return v >= 0 ? v : 0.7;
  }

};

export default soundBox;

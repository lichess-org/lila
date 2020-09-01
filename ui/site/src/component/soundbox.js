class SoundBox {

  sounds = {}; // The loaded sounds and their instances

  volume = lichess.storage.make('sound-volume');

  loadOggOrMp3 = (name, path) => 
    this.sounds[name] = new Howl({
      src: ['ogg', 'mp3'].map(ext => `${path}.${ext}`)
    });

  play(name, volume = 1) {
    const doPlay = () => {
      this.sounds[name].volume(volume * this.getVolume());
      this.sounds[name].play();
    };
    if (Howler.ctx.state == "suspended") Howler.ctx.resume().then(doPlay);
    else doPlay();
  };

  setVolume = this.volume.set;

  getVolume = () => {
    // garbage has been stored here by accident (e972d5612d)
    const v = parseFloat(this.volume.get());
    return v >= 0 ? v : 0.7;
  }
}
lichess.soundBox = new SoundBox

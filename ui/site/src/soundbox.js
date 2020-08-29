class SoundBox {

  sounds = {}; // The loaded sounds and their instances

  loadOggOrMp3 = (name, path) => {
    this.sounds[name] = new Howl({
      src: ['ogg', 'mp3'].map(ext => `${path}.${ext}`)
    });
  }

  play(name, volume = 1) {
    if (!this.sounds[name]) return console.error(`Can't find sound ${name}`);
    this.sounds[name].volume(volume);
    this.sounds[name].play();
  };
}
lichess.soundBox = new SoundBox

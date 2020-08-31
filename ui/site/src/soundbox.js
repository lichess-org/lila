class SoundBox {

  sounds = {}; // The loaded sounds and their instances

  loadOggOrMp3 = (name, path) => 
    this.sounds[name] = new Howl({
      src: ['ogg', 'mp3'].map(ext => `${path}.${ext}`)
    });

  play(name, volume = 1) {
    const doPlay = () => {
      this.sounds[name].volume(volume);
      this.sounds[name].play();
    };
    if (Howler.ctx.state == "suspended") Howler.ctx.resume().then(doPlay);
    else doPlay();
  };
}
lichess.soundBox = new SoundBox

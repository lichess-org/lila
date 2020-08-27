// loosely based on https://raw.githubusercontent.com/sbrl/soundbox/master/soundbox.js
class SoundBox {

  sounds = {}; // The loaded sounds and their instances

  load(name, path) {
    try {
      return new Promise((resolve, reject) => {
        this.sounds[name] = new Audio(path);
        this.sounds[name].addEventListener("canplaythrough", resolve);
        this.sounds[name].addEventListener("error", reject);
      });
    } catch (e) {
      return new Promise((_, reject) => reject(e));
    }
  };

  loadOggOrMp3 = (name, path) =>
    this.load(name, `${path}.ogg`).catch(() => this.load(name, `${path}.mp3`));

  play(name, volume = 1) {
    if (!this.sounds[name]) console.error(`Can't find sound ${name}`);
    else try {
      const sound = this.sounds[name].cloneNode();
      sound.volume = volume;
      sound.play();
    } catch (e) {
      console.log(e);
    }
  };
}
lichess.soundBox = new SoundBox

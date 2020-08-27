// loosely based on https://raw.githubusercontent.com/sbrl/soundbox/master/soundbox.js
class SoundBox {

  sounds = {}; // The loaded sounds and their instances

  load(name, path) {
    this.sounds[name] = new Audio(path);
    return new Promise((resolve, reject) => {
      this.sounds[name].addEventListener("canplaythrough", resolve);
      this.sounds[name].addEventListener("error", reject);
    });
  };

  play(name, volume = 1) {
    if (!this.sounds[name]) {
      console.error(`Can't find sound ${name}`);
      return false;
    }

    const sound = this.sounds[name].cloneNode();
    sound.volume = volume;
    sound.play();

    return new Promise(resolve => sound.addEventListener("ended", resolve));
  };
}
lichess.soundBox = new SoundBox

// based on https://raw.githubusercontent.com/sbrl/soundbox/master/soundbox.js
class SoundBox {

  sounds = {}; // The loaded sounds and their instances
  instances = []; // Sounds that are currently playing

  load(soundName, path) {
    this.sounds[soundName] = new Audio(path);
    return new Promise((resolve, reject) => {
      this.sounds[soundName].addEventListener("canplaythrough", resolve);
      this.sounds[soundName].addEventListener("error", reject);
    });
  };

  play(soundName, volume = 1) {
    if (!this.sounds[soundName]) {
      console.error(`Can't find sound ${soundName}`);
      return false;
    }

    const soundInstance = this.sounds[soundName].cloneNode(true);
    this.instances.push(soundInstance);
    soundInstance.volume = volume;

    soundInstance.play().catch(e => {
      console.log(e);
      this.deleteInstance(soundInstance);
    });

    return new Promise(resolve => soundInstance.addEventListener("ended", () => {
      this.deleteInstance(soundInstance);
      resolve();
    }));
  };

  deleteInstance = soundInstance => {
    let index = this.instances.indexOf(soundInstance);
    if (index != -1) this.instances.splice(index, 1);
  };

  // stopAll() {
  //   // Pause all currently playing sounds

  //   // Shallow clone the array to avoid issues with instances auto-removing themselves
  //   this.instances.slice().forEach(instance => {
  //     instance.pause();
  //     instance.dispatchEvent(new Event("ended"));
  //   });
  // }
}
lichess.soundBox = new SoundBox

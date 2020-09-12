function make(file: string) {
  window.lichess.soundBox.loadOggOrMp3(file, `${window.lichess.soundUrl}/${file}`);
  return () => {
    if (window.lichess.sound.set() !== 'silent') window.lichess.soundBox.play(file);
  };
};

export default function() {
  return {
    success: make('other/energy3'),
    failure: make('other/failure2')
  };
}

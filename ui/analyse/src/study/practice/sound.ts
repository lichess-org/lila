function make(file: string) {
  const baseUrl = window.lichess.assetUrl('sound', {
    noVersion: true
  });
  window.lichess.soundBox.loadOggOrMp3(file, `${baseUrl}/${file}`);
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

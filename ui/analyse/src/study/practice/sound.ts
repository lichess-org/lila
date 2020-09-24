function make(file: string) {
  lichess.soundBox.loadOggOrMp3(file, `${lichess.soundUrl}/${file}`);
  return () => {
    if (lichess.sound.set() !== 'silent') lichess.soundBox.play(file);
  };
};

export default function() {
  return {
    success: make('other/energy3'),
    failure: make('other/failure2')
  };
}

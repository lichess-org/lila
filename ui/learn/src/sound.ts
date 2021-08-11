const make = (file: string, volume?: number) => {
  lichess.sound.loadOggOrMp3(file, `${lichess.sound.baseUrl}/${file}`);
  return () => lichess.sound.play(file, volume);
};

export const move = () => lichess.sound.play('move');
export const take = make('sfx/Tournament3rd', 0.4);
export const levelStart = make('other/ping');
export const levelEnd = make('other/energy3');
export const stageStart = make('other/guitar');
export const stageEnd = make('other/gewonnen');
export const failure = make('other/no-go');

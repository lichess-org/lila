const make = (name: string, volume?: number) => {
  site.sound.load(name, site.sound.url(`${name}.mp3`));
  return () => site.sound.play(name, volume);
};

export const move = () => site.sound.play('move');
export const take = make('sfx/Tournament3rd', 0.4);
export const levelStart = make('other/ping');
export const levelEnd = make('other/energy3');
export const stageStart = make('other/guitar');
export const stageEnd = make('other/gewonnen');
export const failure = make('other/no-go');

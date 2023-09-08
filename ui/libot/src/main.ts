export * from './interfaces';

export * from './ctrl';

export interface BotInfo {
  readonly name: string;
  readonly description: string;
  readonly image: string;
}

export const localBots: { [key: string]: BotInfo } = {
  coral: {
    name: 'Coral',
    description: 'Coral is a simple bot that plays random moves.',
    image: botImageUrl('coral.webp'),
  },
  babyHoward: {
    name: 'Baby Howard',
    description: 'Baby Howard is a bot that plays random moves.',
    image: botImageUrl('baby-howard.webp'),
  },
  elsieZero: {
    name: 'Elsie Zero',
    description: 'Elsie Zero is a bot that plays random moves.',
    image: botImageUrl('baby-robot.webp'),
  },
  beatrice: {
    name: 'Beatrice',
    description: 'Beatrice is a bot that plays random moves.',
    image: botImageUrl('beatrice.webp'),
  },
  benny: {
    name: 'Benny',
    description: '',
    image: botImageUrl('benny.webp'),
  },
  danny: {
    name: 'Danny',
    description: '',
    image: botImageUrl('danny.webp'),
  },
  dansby: {
    name: 'Dansby',
    description: '',
    image: botImageUrl('dansby.webp'),
  },
  gary: {
    name: 'Gary',
    description: '',
    image: botImageUrl('gary.webp'),
  },
  greta: {
    name: 'Greta',
    description: '',
    image: botImageUrl('greta.webp'),
  },
  grunt: {
    name: 'Grunt',
    description: '',
    image: botImageUrl('grunt.webp'),
  },
  helena: {
    name: 'Helena',
    description: '',
    image: botImageUrl('helena.webp'),
  },
  henry: {
    name: 'Henry',
    description: '',
    image: botImageUrl('henry.webp'),
  },
  larry: {
    name: 'Larry',
    description: '',
    image: botImageUrl('larry.webp'),
  },
  listress: {
    name: 'Listress',
    description: '',
    image: botImageUrl('listress.webp'),
  },
  louise: {
    name: 'Louise',
    description: '',
    image: botImageUrl('louise.webp'),
  },
  maia: {
    name: 'Maia',
    description: '',
    image: botImageUrl('maia.webp'),
  },
  marco: {
    name: 'Marco',
    description: '',
    image: botImageUrl('marco.webp'),
  },
  mitsoko: {
    name: 'Mitsoko',
    description: '',
    image: botImageUrl('mitsoko.webp'),
  },
  nacho: {
    name: 'Nacho',
    description: '',
    image: botImageUrl('nacho.webp'),
  },
  owen: {
    name: 'Owen',
    description: '',
    image: botImageUrl('owen.webp'),
  },
  shark: {
    name: 'Shark',
    description: '',
    image: botImageUrl('shark.webp'),
  },
  torso: {
    name: 'Torso',
    description: '',
    image: botImageUrl('soldier-torso.webp'),
  },
  ghost: {
    name: 'Ghost',
    description: '',
    image: botImageUrl('specops-lady.webp'),
  },
  terrence: {
    name: 'Terrence',
    description: '',
    image: botImageUrl('terrence.webp'),
  },
  agatha: {
    name: 'Agatha',
    description: '',
    image: botImageUrl('witch1.webp'),
  },
  sabine: {
    name: 'Sabine',
    description: '',
    image: botImageUrl('witch2.webp'),
  },
};

export function botNetUrl(net: string) {
  return lichess.assetUrl(`lifat/bots/weights/${net}.pb`, { noVersion: true });
}

export function botImageUrl(image: string) {
  return lichess.assetUrl(`lifat/bots/images/${image}`, { noVersion: true });
}

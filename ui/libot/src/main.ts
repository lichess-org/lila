export * from './interfaces';

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
  babyBot: {
    name: 'Baby Bot',
    description: 'Baby Bot is a bot that plays random moves.',
    image: botImageUrl('baby-robot.webp'),
  },
  beatrice: {
    name: 'Beatrice',
    description: 'Beatrice is a bot that plays random moves.',
    image: botImageUrl('beatrice.webp'),
  },
};

export function botNetUrl(weights: string) {
  return lichess.assetUrl(`lifat/bots/weights/${weights}`, { noVersion: true });
}

export function botImageUrl(image: string) {
  return lichess.assetUrl(`lifat/bots/images/${image}`, { noVersion: true });
}

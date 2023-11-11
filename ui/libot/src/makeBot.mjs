import * as fs from 'node:fs';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const srcDir = path.join(path.dirname(fileURLToPath(import.meta.url)), 'bots');

const localBots = {
  coral: {
    name: 'Coral',
    description: 'Coral is a simple bot that plays random moves.',
    image: 'coral.webp',
  },
  babyHoward: {
    name: 'Baby Howard',
    description: 'Baby Howard is a bot that plays random moves.',
    image: 'baby-howard.webp',
  },
  elsieZero: {
    name: 'Elsie Zero',
    description: 'Elsie Zero is a bot that plays random moves.',
    image: 'baby-robot.webp',
  },
  beatrice: {
    name: 'Beatrice',
    description: 'Beatrice is a bot that plays random moves.',
    image: 'beatrice.webp',
  },
  benny: {
    name: 'Benny',
    description: '',
    image: 'benny.webp',
  },
  danny: {
    name: 'Danny',
    description: '',
    image: 'danny.webp',
  },
  dansby: {
    name: 'Dansby',
    description: '',
    image: 'dansby.webp',
  },
  gary: {
    name: 'Gary',
    description: '',
    image: 'gary.webp',
  },
  greta: {
    name: 'Greta',
    description: '',
    image: 'greta.webp',
  },
  grunt: {
    name: 'Grunt',
    description: '',
    image: 'grunt.webp',
  },
  helena: {
    name: 'Helena',
    description: '',
    image: 'helena.webp',
  },
  henry: {
    name: 'Henry',
    description: '',
    image: 'henry.webp',
  },
  larry: {
    name: 'Larry',
    description: '',
    image: 'larry.webp',
  },
  listress: {
    name: 'Listress',
    description: '',
    image: 'listress.webp',
  },
  louise: {
    name: 'Louise',
    description: '',
    image: 'louise.webp',
  },
  maia: {
    name: 'Maia',
    description: '',
    image: 'maia.webp',
  },
  marco: {
    name: 'Marco',
    description: '',
    image: 'marco.webp',
  },
  mitsoko: {
    name: 'Mitsoko',
    description: '',
    image: 'mitsoko.webp',
  },
  nacho: {
    name: 'Nacho',
    description: '',
    image: 'nacho.webp',
  },
  owen: {
    name: 'Owen',
    description: '',
    image: 'owen.webp',
  },
  shark: {
    name: 'Shark',
    description: '',
    image: 'shark.webp',
  },
  torso: {
    name: 'Torso',
    description: '',
    image: 'soldier-torso.webp',
  },
  ghost: {
    name: 'Ghost',
    description: '',
    image: 'specops-lady.webp',
  },
  terrence: {
    name: 'Terrence',
    description: '',
    image: 'terrence.webp',
  },
  agatha: {
    name: 'Agatha',
    description: '',
    image: 'witch1.webp',
  },
  sabine: {
    name: 'Sabine',
    description: '',
    image: 'witch2.webp',
  },
};

for (const k in localBots) {
  const bot = localBots[k];
  const ext = makeBot(k, bot);
  fs.writeFileSync(path.join(srcDir, `${k}.ts`), ext);
}

function makeBot(k, bot) {
  const clz = bot.name.replace(/ /g, '');
  return `import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class ${clz} implements Libot {
  name = '${bot.name}';
  description = '${bot.name} is a bot that plays random moves.';
  imageUrl = lichess.assetUrl('lifat/bots/images/${bot.image}', { noVersion: true });
  netName = 'maia-1100';
  ratings = new Map();
  zf: Zerofish;

  constructor(zf: Zerofish, opts?: any) {
    opts;
    this.zf = zf;
  }

  async move(fen: string) {
    return await this.zf.goZero(fen);
  }
}

registry.${k} = ${clz};
`;
}

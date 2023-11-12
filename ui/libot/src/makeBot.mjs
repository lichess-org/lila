import * as fs from 'node:fs';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const srcDir = path.join(path.dirname(fileURLToPath(import.meta.url)), 'bots');

const localBots = {
  babyHoward: {
    name: 'Baby Howard',
    description: 'Baby Howard is a bot that plays random moves.',
    image: 'baby-howard.webp',
  },
  beatrice: {
    name: 'Beatrice',
    description: 'Beatrice is a bot that plays random moves.',
    image: 'beatrice.webp',
  },
  coral: {
    name: 'Coral',
    description: 'Coral is a simple bot that plays random moves.',
    image: 'coral.webp',
  },
  louise: {
    name: 'Louise',
    description: '',
    image: 'louise.webp',
  },
  gary: {
    name: 'Gary',
    description: '',
    image: 'gary.webp',
  },
  elsieZero: {
    name: 'Elsie Zero',
    description: 'Elsie Zero is a bot that plays random moves.',
    image: 'baby-robot.webp',
  },
  henry: {
    name: 'Henry',
    description: '',
    image: 'henry.webp',
  },
  owen: {
    name: 'Owen',
    description: '',
    image: 'owen.webp',
  },
  nacho: {
    name: 'Nacho',
    description: '',
    image: 'nacho.webp',
  },
  terrence: {
    name: 'Terrence',
    description: '',
    image: 'terrence.webp',
  },
  shark: {
    name: 'Shark',
    description: '',
    image: 'shark.webp',
  },
  dansby: {
    name: 'Dansby',
    description: '',
    image: 'dansby.webp',
  },
  larry: {
    name: 'Larry',
    description: '',
    image: 'larry.webp',
  },
  marco: {
    name: 'Marco',
    description: '',
    image: 'marco.webp',
  },
  agatha: {
    name: 'Agatha',
    description: '',
    image: 'witch1.webp',
  },
  greta: {
    name: 'Greta',
    description: '',
    image: 'greta.webp',
  },
  helena: {
    name: 'Helena',
    description: '',
    image: 'helena.webp',
  },
  grunt: {
    name: 'Grunt',
    description: '',
    image: 'grunt.webp',
  },
  maia: {
    name: 'Maia',
    description: '',
    image: 'maia.webp',
  },
  mitsoko: {
    name: 'Mitsoko',
    description: '',
    image: 'mitsoko.webp',
  },
  ghost: {
    name: 'Ghost',
    description: '',
    image: 'specops-lady.webp',
  },
  spectre: {
    name: 'Spectre',
    description: '',
    image: 'soldier-torso.webp',
  },
  listress: {
    name: 'Listress',
    description: '',
    image: 'listress.webp',
  },
};

let ordinal = 0;

for (const k in localBots) {
  const bot = localBots[k];
  const ext = makeBot(k, bot, ordinal++);
  fs.writeFileSync(path.join(srcDir, `${k}.ts`), ext);
}

function makeBot(k, bot, ordinal) {
  const clz = bot.name.replace(/ /g, '');
  return `import { type Zerofish } from 'zerofish';
import { Libot } from '../interfaces';
import { registry } from '../ctrl';

export class ${clz} implements Libot {
  name = '${bot.name}';
  uid = '#${k}';
  ordinal = ${ordinal};
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

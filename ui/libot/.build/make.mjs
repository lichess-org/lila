import * as fs from 'node:fs';
import * as path from 'node:path';
import { fileURLToPath } from 'node:url';

const buildDir = path.join(path.dirname(fileURLToPath(import.meta.url)));
const srcDir = path.join(buildDir, '../src/bots');

const localBots = JSON.parse(fs.readFileSync(path.join(buildDir, 'bots.json'), 'utf8'));
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
  imageUrl = site.assetUrl('lifat/bots/images/${bot.image}', { noVersion: true });
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

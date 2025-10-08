import * as cheerio from 'cheerio';
import { readFileSync, writeFileSync } from 'fs';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const lilaDir = path.resolve(path.dirname(__filename), '../..');
const baseDir = path.resolve(lilaDir, 'bin/flair');

const customFlairs = readFileSync(path.resolve(baseDir, 'custom.txt'), 'utf-8')
  .split('\n')
  .filter(line => line.trim() !== '');
const htmlEmoji = readFileSync(path.resolve(baseDir, 'emojipedia.html'), 'utf-8');
const $ = cheerio.load(htmlEmoji);

const ignoredEmoji = [
  'flags.',
  'food-drink.eggplant',
  'food-drink.peach',
  'objects.axe',
  'objects.bomb',
  'objects.cigarette',
  'objects.dagger',
  'objects.prayer-beads',
  'people.kiss-man',
  'people.kiss-person',
  'people.kiss-woman',
  'people.middle-finger',
  'people.pinching-hand',
  'people.thumbs-down',
  'smileys.face-vomiting',
  'smileys.pile-of-poo',
  'symbols.biohazard',
  'symbols.black-medium-square',
  'symbols.black-small-square',
  'symbols.check-box-with-check',
  'symbols.check-mark-button',
  'symbols.check-mark',
  'symbols.dotted-six-pointed-star',
  'symbols.khanda',
  'symbols.latin-cross',
  'symbols.menorah',
  'symbols.om',
  'symbols.orthodox-cross',
  'symbols.place-of-worship',
  'symbols.prohibited',
  'symbols.radioactive',
  'symbols.regional-indicator',
  'symbols.splatter',
  'symbols.star-and-crescent',
  'symbols.star-of-david',
  'symbols.sweat-droplets',
  'symbols.warning',
  'symbols.wheel-of-dharma',
  'symbols.white-medium-square',
  'symbols.white-small-square',
  'symbols.yin-yang',
];

let currentCategory = '';

const names: string[] = [];
const wgets: string[] = [];

$('div').each((i, el) => {
  const category = $(el)
    .text()
    .split('\n')
    .join(' ')
    .trim()
    .toLowerCase()
    .replace(' & ', '-')
    .replace('animals-', '');

  if (category) {
    currentCategory = category;
  }

  $(el)
    .find('a')
    .each((i, el) => {
      let name = $(el).attr('href');
      const url = $(el).attr('data-src');

      name = name?.substring(name.lastIndexOf('/') + 1);
      name = `${currentCategory}.${name?.replaceAll('_', '-')}`;

      if (!name || !url || ignoredEmoji.some(prefix => name.startsWith(prefix))) {
        return;
      }

      names.push(name);
      wgets.push(`curl -o public/flair/img/${name}.webp ${url}`);
    });
});

writeFileSync(path.resolve(baseDir, 'emoji.txt'), names.sort().join('\n'));
writeFileSync(path.resolve(baseDir, 'download-flair.sh'), `#!/bin/bash -e\n\n${wgets.join('\n')}`);

const allFlairs = Array.from(new Set([...customFlairs, ...names])).sort();
writeFileSync(path.resolve(lilaDir, 'public/flair/list.txt'), allFlairs.join('\n') + '\n');

console.log(
  `Wrote ${allFlairs.length} flairs to public/flair/list.txt (custom: ${customFlairs.length}, emoji: ${names.length})`,
);

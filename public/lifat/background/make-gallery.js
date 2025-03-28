#!node

const fs = require('fs');
const path = require('path');
const ps = require('process');
const cps = require('child_process');

const urlBase = 'lifat/background'; // for client asset downloads

// to change the order of images in the gallery, rename them before running this script.

ps.chdir(path.resolve(__dirname, 'gallery'));
const files = fs.readdirSync('.').sort();

let exec = 'montage';
try {
  cps.execFileSync('which', ['montage']).toString('utf-8');
} catch (e) {
  try {
    cps.execFileSync('which', ['magick']).toString('utf-8');
    exec = 'magick montage';
  } catch (e) {
    console.error('Install ImageMagick CLI tools. https://imagemagick.org/');
    ps.exit(1);
  }
}
console.log('Gallery thumbnails will appear in filename sort order. Building 2 & 4 column images...');
// for large numbers of images, they'll need to be downsized first before running montage
// but this should be ok for under 64 or so
const paramlist = [4, 2].map(n =>
  `-tile ${n}x -geometry +1+1 -resize 160x90^ -gravity center -extent 160x90 -background none ../montage${n}.webp`.split(
    ' '
  )
);
for (const params of paramlist) cps.execFileSync(exec, [...files, ...params]);
fs.writeFileSync(
  '../gallery.json',
  JSON.stringify(
    {
      images: files.map(f => path.join(urlBase, 'gallery', f)),
      montage2: path.join(urlBase, 'montage2.webp'),
      montage4: path.join(urlBase, 'montage4.webp'),
    },
    undefined,
    2
  )
);
console.log('Done.');

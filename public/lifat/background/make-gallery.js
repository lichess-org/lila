#!node

const fs = require('fs');
const path = require('path');
const ps = require('process');
const cps = require('child_process');

const urlBase = 'lifat/background'; // for client asset downloads
const rootDir = path.resolve(__dirname);
const themes = { light: 'galleryLight', dark: 'gallery' };

// to change the order of images in the gallery, rename them before running this script.

let exec = 'montage';
try {
  cps.execFileSync('which', ['montage']).toString('utf-8');
} catch (e) {
  try {
    cps.execFileSync('which', ['magick']).toString('utf-8');
    exec = 'magick';
  } catch (e) {
    console.error('Install ImageMagick CLI tools. https://imagemagick.org/');
    ps.exit(1);
  }
}

let font;
try {
  font = cps.execFileSync('fc-match', ['-f', '%{file}', 'sans'], { encoding: 'utf8' }).trim();
} catch (e) {
  console.error('Could not find a font for ImageMagick. Install fontconfig.');
  ps.exit(1);
}

console.log('Gallery thumbnails will appear in filename sort order. Building light and dark 2 & 4 column images...');
// for large numbers of images, they'll need to be downsized first before running montage
// but this should be ok for under 64 or so
const gallery = {};
for (const [theme, sourceName] of Object.entries(themes)) {
  const sourceDir = path.join(rootDir, sourceName);
  const files = fs
    .readdirSync(sourceDir)
    .filter(file => /^bg\d+\.webp$/.test(file))
    .sort();
  const lfsStubs = files.filter(file =>
    fs.readFileSync(path.join(sourceDir, file), 'utf8').startsWith('version https://git-lfs.github.com/spec/v1')
  );
  if (lfsStubs.length) {
    console.error(
      `Gallery files are Git LFS pointers, not image data: ${lfsStubs.join(', ')}. Run ` +
        `git lfs pull --include="public/lifat/background/${sourceName}/**" from the lila repository.`
    );
    ps.exit(1);
  }

  const outputDir = rootDir;
  const images = files.map(f => path.join(urlBase, sourceName, f));
  const montages = {};
  ps.chdir(sourceDir);
  for (const columns of [2, 4]) {
    const montage = `montage${theme === 'light' ? 'Light' : 'Dark'}${columns}.webp`;
    const params = [
      '-tile',
      `${columns}x`,
      '-geometry',
      '+1+1',
      '-resize',
      '160x90^',
      '-gravity',
      'center',
      '-extent',
      '160x90',
      '-background',
      'none',
      '-font',
      font,
      path.join(outputDir, montage),
    ];
    cps.execFileSync(exec, exec === 'magick' ? ['montage', ...files, ...params] : [...files, ...params]);
    montages[`montage${columns}`] = path.join(urlBase, montage);
  }
  gallery[theme] = { images, ...montages };
}

fs.writeFileSync(
  path.join(rootDir, 'gallery.json'),
  JSON.stringify(gallery, undefined, 2) + '\n'
);
console.log('Done.');

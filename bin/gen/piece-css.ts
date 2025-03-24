import { existsSync, mkdirSync, readdirSync, readFileSync, writeFileSync } from 'fs';
import path from 'path';

const lilaDir = path.resolve(__dirname, '../..');
const sourceDir = path.resolve(lilaDir, 'public/piece');
const destDir = path.resolve(lilaDir, 'public/piece-css');

const themes: string[] = [
  ...['cburnett'],
  // get list of other piece sets from modules/pref/src/main/PieceSet.scala
  ...[
    'merida',
    'alpha',
    'pirouetti',
    'chessnut',
    'chess7',
    'reillycraig',
    'companion',
    'riohacha',
    'kosal',
    'leipzig',
    'fantasy',
    'spatial',
    'celtic',
    'california',
    'caliente',
    'pixel',
    'rhosgfx',
    'maestro',
    'fresca',
    'cardinal',
    'gioco',
    'tatiana',
    'staunty',
    'cooke',
    'monarchy',
    'governor',
    'dubrovny',
    'icpieces',
    'mpchess',
    'kiwen-suwi',
    'horsey',
    'anarcandy',
    'xkcd',
    'shapes',
    'letter',
    'disguised',
  ],
];

const roles = ['pawn', 'knight', 'bishop', 'rook', 'queen', 'king'];
const colors: Color[] = ['white', 'black'];

const svgFilename = (color: string, role: string) => {
  const piece = color[0] + (role === 'knight' ? 'N' : role[0].toUpperCase());
  return `${piece}.svg`;
};

function generateBase64Css() {
  themes.forEach(name => {
    const classes =
      colors
        .map(color =>
          roles
            .map(role => {
              const filePath = path.join(sourceDir, name, svgFilename(color, role));
              const image = readFileSync(filePath);
              const base64 = image.toString('base64');
              return `.is2d .${role}.${color} {background-image:url('data:image/svg+xml;base64,${base64}')}`;
            })
            .join('\n'),
        )
        .join('\n') + '\n';

    const outputFile = path.join(destDir, `${name}.css`);
    writeFileSync(outputFile, classes, 'utf-8');
  });
}

if (!existsSync(destDir)) {
  mkdirSync(destDir, { recursive: true });
}

generateBase64Css();

console.log(`✅ Generated piece CSS files in ${destDir}`);

function validateThemeDirectories() {
  const validFilenames = colors.flatMap(color => roles.map(role => svgFilename(color, role)));
  readdirSync(sourceDir)
    .filter(dir => dir !== 'mono')
    .forEach(dir => {
      if (!themes.includes(dir)) {
        console.error(`${path.join(sourceDir, dir)} is not in the theme list`);
        process.exit(1);
      }

      readdirSync(path.join(sourceDir, dir)).forEach(file => {
        if (dir === 'disguised' && ['b.svg', 'w.svg'].includes(file)) return;
        if (!validFilenames.includes(file)) {
          console.error(`${path.join(sourceDir, dir, file)} is not a valid piece SVG filename`);
          process.exit(1);
        }
      });
    });
}

validateThemeDirectories();

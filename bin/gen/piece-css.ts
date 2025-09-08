import { existsSync, mkdirSync, readdirSync, writeFileSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const lilaDir = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '../..');
const sourceDir = path.resolve(lilaDir, 'public/piece');
const destDir = path.resolve(lilaDir, 'ui/lib/css/build/pieces-gen');

const roles = ['pawn', 'knight', 'bishop', 'rook', 'queen', 'king'] as const;
const colors = ['white', 'black'] as const;

const svgFilename = (color: string, role: string) => {
  const piece = color[0] + (role === 'knight' ? 'N' : role[0].toUpperCase());
  return `${piece}.svg`;
};

const themes = readdirSync(sourceDir, { withFileTypes: true })
  .filter(d => d.isDirectory() && d.name !== 'mono')
  .map(d => d.name);

function generateUrlCss() {
  themes.forEach(name => {
    const classes =
      colors
        .map(color =>
          roles
            .map(role => {
              const filename = svgFilename(color, role);
              const filePath = path.join(sourceDir, name, filename);
              if (name !== 'disguised' && !existsSync(filePath)) throw new Error(`${filePath} is missing`);
              return `.is2d .${role}.${color}{background-image:url(../piece/${name}/${filename})}`;
            })
            .join('\n'),
        )
        .join('\n') + '\n';

    const outputFile = path.join(destDir, `pieces.${name}.scss`);
    writeFileSync(outputFile, classes, 'utf-8');
  });
}

if (!existsSync(destDir)) mkdirSync(destDir, { recursive: true });

generateUrlCss();

console.log(`✅ Generated piece CSS files in ${destDir}`);

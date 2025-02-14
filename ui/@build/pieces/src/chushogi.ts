import * as fs from 'node:fs';
import * as path from 'node:path';
import dedent from 'dedent';
import { type Theme, colors, types } from './util.js';

const roles = [
  'lance',
  'leopard',
  'copper',
  'silver',
  'gold',
  'elephant',
  'chariot',
  'bishop',
  'tiger',
  'phoenix',
  'kirin',
  'sidemover',
  'verticalmover',
  'rook',
  'horse',
  'dragon',
  'queen',
  'lion',
  'pawn',
  'gobetween',
  'king',
  'tama',
  'promotedpawn',
  'ox',
  'stag',
  'boar',
  'falcon',
  'prince',
  'eagle',
  'whale',
  'whitehorse',
  'dragonpromoted',
  'horsepromoted',
  'lionpromoted',
  'queenpromoted',
  'bishoppromoted',
  'elephantpromoted',
  'sidemoverpromoted',
  'verticalmoverpromoted',
  'rookpromoted',
];

const regular: Theme[] = [
  ['Chu_Ryoko_1Kanji', 'svg'],
  ['Chu_Intl', 'svg'],
  ['Chu_Eigetsu_Gyoryu', 'png'],
  ['Chu_FCZ', 'svg'],
];

const bidirectional: Theme[] = [
  ['Chu_Mnemonic', 'svg'],
  ['Chu_Intl_BnW', 'svg'],
  ['Chu_Firi', 'svg'],
];

function sameUpDownM(role: string): boolean {
  return [
    'bishop',
    'bishoppromoted',
    'boar',
    'chariot',
    'dragon',
    'dragonpromoted',
    'horse',
    'horsepromoted',
    'king',
    'kirin',
    'leopard',
    'lion',
    'lionpromoted',
    'ox',
    'phoenix',
    'prince',
    'queen',
    'queenpromoted',
    'rook',
    'rookpromoted',
    'sidemover',
    'sidemoverpromoted',
    'stag',
    'tama',
    'verticalmover',
    'verticalmoverpromoted',
  ].includes(role);
}

function classesMnemonic(color: string, role: string): string {
  return `.v-chushogi piece.${role}.${color}`;
}

function classesWithOrientation(color: string, role: string, flipped: boolean): string {
  if (flipped) {
    if (color === 'sente') {
      return dedent`.v-chushogi .sg-wrap.orientation-gote piece.${role}.gote,
      .spare-bottom.v-chushogi piece.${role}.gote`;
    } else {
      return dedent`.v-chushogi .sg-wrap.orientation-gote piece.${role}.sente,
      .spare-top.v-chushogi piece.${role}.sente`;
    }
  } else {
    if (color === 'sente') {
      return dedent`.v-chushogi piece.${role}.sente,
      .v-chushogi .sg-wrap.orientation-sente piece.${role}.sente,
      .spare-bottom.v-chushogi piece.${role}.sente`;
    } else {
      return dedent`.v-chushogi piece.${role}.gote,
      .v-chushogi .sg-wrap.orientation-sente piece.${role}.gote,
      .spare-top.v-chushogi piece.${role}.gote`;
    }
  }
}

function classes(color: string, role: string): string {
  if (color === 'sente') {
    // facing up
    if (role === 'king') {
      return dedent`.v-chushogi .sg-wrap.orientation-gote piece.king.gote,
      .spare-bottom.v-chushogi piece.king`;
    } else if (role === 'tama') {
      return dedent`.v-chushogi piece.king.sente,
      .v-chushogi .sg-wrap.orientation-sente piece.king.sente,
      .spare-bottom.v-chushogi piece.king.sente`;
    } else {
      return dedent`.v-chushogi .sg-wrap.orientation-sente piece.${role}.sente,
      .v-chushogi .sg-wrap.orientation-gote piece.${role}.gote,
      .spare-bottom.v-chushogi piece.${role}`;
    }
  } else {
    // facing down
    if (role === 'king') {
      return dedent`.v-chushogi .sg-wrap.orientation-sente piece.king.gote,
      .spare-top.v-chushogi piece.king`;
    } else if (role === 'tama') {
      return dedent`.v-chushogi .sg-wrap.orientation-gote piece.king.sente,
      .spare-top.v-chushogi piece.king.sente`;
    } else {
      return dedent`.v-chushogi .sg-wrap.orientation-sente piece.${role}.gote,
      .v-chushogi .sg-wrap.orientation-gote piece.${role}.sente,
      .spare-top.v-chushogi piece.${role}`;
    }
  }
}

function extraCss(name: string, ext: string): string {
  const cssClasses: string[] = [];
  if (ext === 'png') {
    cssClasses.push(
      '.v-chushogi piece { will-change: transform !important; background-repeat: unset !important; }',
    );
  } else {
    cssClasses.push('.v-chushogi piece { will-change: auto; background-repeat: no-repeat; }');
  }

  if (name === 'Chu_Mnemonic') {
    cssClasses.push('.v-chushogi piece { background-size: contain !important; }');
  }
  return cssClasses.join('\n');
}

export function chushogi(sourceDir: string, destDir: string): void {
  for (const [name, ext] of regular) {
    const cssClasses = colors.flatMap(color =>
      roles.map(role => {
        const piece = `${color === 'sente' ? '0_' : '1_'}${role.toUpperCase()}`;
        const file = path.join(sourceDir, name, `${piece}.${ext}`);
        const image = fs.readFileSync(file);
        const base64 = image.toString('base64');
        return `${classes(color, role)} {background-image:url('data:image/${types[ext]}${base64}') !important;}`;
      }),
    );

    cssClasses.push(extraCss(name, ext));
    cssClasses.push(''); // trailing new line

    fs.writeFileSync(path.join(destDir, `${name}.css`), cssClasses.join('\n'));
  }

  for (const [name, ext] of bidirectional) {
    const rolesWithoutTama = roles.filter(role => role !== 'tama');
    const cssClasses = ['-1', '']
      .flatMap(up =>
        colors.flatMap(color =>
          rolesWithoutTama.map(role => {
            const piece = `${color === 'sente' ? '0_' : '1_'}${role.toUpperCase()}${up}`;
            const file = path.join(sourceDir, name, `${piece}.${ext}`);

            if (!(name === 'Chu_Mnemonic' && up === '-1' && sameUpDownM(role))) {
              try {
                const image = fs.readFileSync(file);
                const base64 = image.toString('base64');

                if (sameUpDownM(role) && name === 'Chu_Mnemonic') {
                  return `${classesMnemonic(color, role)} {background-image:url('data:image/${types[ext]}${base64}') !important;}`;
                } else {
                  return `${classesWithOrientation(color, role, up.length !== 0)} {background-image:url('data:image/${types[ext]}${base64}') !important;}`;
                }
              } catch (error) {
                console.error(`Error processing file ${file}:`, error);
                return '';
              }
            }
            return '';
          }),
        ),
      )
      .filter(css => css !== ''); // Remove empty strings

    cssClasses.push(extraCss(name, ext));
    cssClasses.push(''); // trailing new line

    fs.writeFileSync(path.join(destDir, `${name}.css`), cssClasses.join('\n'));
  }
}

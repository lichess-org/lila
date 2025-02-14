import * as fs from 'node:fs';
import * as path from 'node:path';
import dedent from 'dedent';
import { type RoleDict, type Theme, colors, types } from './util.js';

const regular: Theme[] = [
  ['Kyo_doubutsu', 'svg'],
  ['Kyo_joyful', 'png'],
  ['Kyo_orangain', 'svg'],
  ['Kyo_Kanji', 'svg'],
  ['Kyo_simple_kanji', 'svg'],
  ['Kyo_Intl', 'svg'],
  ['Kyo_international', 'svg'],
  ['Kyo_Logy_Games', 'svg'],
  ['Kyo_Ryoko_1Kanji', 'svg'],
];

const bidirectional: Theme[] = [];

const roleDict: RoleDict = {
  FU: 'pawn',
  GI: 'silver',
  GY: 'tama',
  HI: 'rook',
  KA: 'bishop',
  KE: 'knight',
  KI: 'gold',
  KY: 'lance',
  OU: 'king',
  TO: 'tokin',
};

function classesWithOrientation(color: string, role: string, flipped: boolean): string {
  if (flipped) {
    if (color === 'sente') {
      return dedent`.v-kyotoshogi .sg-wrap.orientation-gote piece.${role}.gote,
      .v-kyotoshogi .hand-bottom piece.${role}.gote,
      .spare-bottom.v-kyotoshogi piece.${role}.gote`;
    } else {
      return dedent`.v-kyotoshogi .sg-wrap.orientation-gote piece.${role}.sente,
      .v-kyotoshogi .hand-top piece.${role}.sente,
      .spare-top.v-kyotoshogi piece.${role}.sente`;
    }
  } else {
    if (color === 'sente') {
      return dedent`.v-kyotoshogi .sg-wrap.orientation-sente piece.${role}.sente,
      .v-kyotoshogi .hand-bottom piece.${role}.sente,
      .spare-bottom.v-kyotoshogi piece.${role}.sente`;
    } else {
      return dedent`.sg-wrap.orientation-sente piece.${role}.gote,
      .v-kyotoshogi .hand-top piece.${role}.gote,
      .spare-top.v-kyotoshogi piece.${role}.gote`;
    }
  }
}

function classes(color: string, role: string): string {
  if (color === 'sente') {
    // facing up
    if (role === 'king') {
      return dedent`.v-kyotoshogi .sg-wrap.orientation-gote piece.king.gote,
      .spare-bottom.v-kyotoshogi piece.king.gote`;
    } else if (role === 'tama') {
      return dedent`.v-kyotoshogi piece.king.sente,
      .v-kyotoshogi .sg-wrap.orientation-sente piece.king.sente,
      .spare-bottom.v-kyotoshogi piece.king.sente`;
    } else {
      return dedent`.v-kyotoshogi piece.${role}.sente,
      .v-kyotoshogi .sg-wrap.orientation-sente piece.${role}.sente,
      .v-kyotoshogi .sg-wrap.orientation-gote piece.${role}.gote,
      .v-kyotoshogi .hand-bottom piece.${role}.gote,
      .spare-bottom.v-kyotoshogi piece.${role}`;
    }
  } else {
    // facing down
    if (role === 'king') {
      return dedent`.v-kyotoshogi piece.king.gote,
      .v-kyotoshogi .sg-wrap.orientation-sente piece.king.gote,
      .spare-top.v-kyotoshogi piece.king.gote`;
    } else if (role === 'tama') {
      return dedent`.v-kyotoshogi .sg-wrap.orientation-gote piece.king.sente,
      .spare-top.v-kyotoshogi piece.king.sente`;
    } else {
      return dedent`.v-kyotoshogi piece.${role}.gote,
      .v-kyotoshogi .sg-wrap.orientation-sente piece.${role}.gote,
      .v-kyotoshogi .sg-wrap.orientation-gote piece.${role}.sente,
      .v-kyotoshogi .hand-top piece.${role},
      .spare-top.v-kyotoshogi piece.${role}`;
    }
  }
}

function extraCss(_name: string, ext: string): string {
  const cssClasses: string[] = [];

  if (ext === 'png') {
    cssClasses.push(
      '.v-kyotoshogi piece { will-change: transform !important; background-repeat: unset !important; }',
    );
  }
  return cssClasses.join('\n');
}

export function kyotoshogi(sourceDir: string, destDir: string): void {
  const roles = Object.keys(roleDict);

  for (const [name, ext] of regular) {
    const cssClasses = colors.flatMap(color =>
      roles.map(role => {
        const piece = `${color === 'sente' ? '0' : '1'}${role}`;
        const file = path.join(sourceDir, name, `${piece}.${ext}`);
        const image = fs.readFileSync(file);
        const base64 = image.toString('base64');
        const cls = classes(color, roleDict[role]);
        return `${cls} {background-image:url('data:image/${types[ext]}${base64}')!important;}`;
      }),
    );

    cssClasses.push(extraCss(name, ext));
    cssClasses.push(''); // trailing new line

    fs.writeFileSync(path.join(destDir, `${name}.css`), cssClasses.join('\n'));
  }

  for (const [name, ext] of bidirectional) {
    const cssClasses = ['-1', '']
      .flatMap(up =>
        colors.flatMap(color =>
          roles.map(role => {
            const piece = `${color === 'sente' ? '0' : '1'}${role}${up}`;
            const file = path.join(sourceDir, name, `${piece}.${ext}`);
            const image = fs.readFileSync(file);
            const base64 = image.toString('base64');
            const cls = classesWithOrientation(color, roleDict[role], up.length !== 0);
            return `${cls} {background-image:url('data:image/${types[ext]}${base64}')!important;}`;
          }),
        ),
      )
      .filter(css => css !== '');

    cssClasses.push(extraCss(name, ext));
    cssClasses.push(''); // trailing new line

    fs.writeFileSync(path.join(destDir, `${name}.css`), cssClasses.join('\n'));
  }
}

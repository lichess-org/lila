import * as fs from 'node:fs';
import * as path from 'node:path';
import dedent from 'dedent';
import { type RoleDict, type Theme, colors, types } from './util.js';

const regular: Theme[] = [
  ['1Kanji_3D', 'svg'],
  ['2Kanji_3D', 'svg'],
  ['doubutsu', 'svg'],
  ['international', 'svg'],
  ['Intl_Colored_2D', 'svg'],
  ['Intl_Colored_3D', 'svg'],
  ['Intl_Shadowed', 'svg'],
  ['Intl_Monochrome_2D', 'svg'],
  ['Intl_Wooden_3D', 'svg'],
  ['Intl_Portella', 'png'],
  ['kanji_brown', 'svg'],
  ['kanji_light', 'svg'],
  ['Kanji_Guide_Shadowed', 'svg'],
  ['kanji_red_wood', 'svg'],
  ['orangain', 'svg'],
  ['simple_kanji', 'svg'],
  ['Vald_opt', 'svg'],
  ['Valdivia', 'svg'],
  ['Logy_Games', 'svg'],
  ['Shogi_cz', 'svg'],
  ['Ryoko_1Kanji', 'svg'],
  ['Shogi_FCZ', 'svg'],
  ['Portella', 'png'],
  ['Portella_2Kanji', 'png'],
  ['western', 'svg'],
  ['Engraved_cz', 'svg'],
  ['pixel', 'png'],
];

const bidirectional: Theme[] = [
  ['shogi_BnW', 'svg'],
  ['Engraved_cz_BnW', 'svg'],
  ['joyful', 'png'],
  ['characters', 'png'],
  ['Firi', 'svg'],
];

const roleDict: RoleDict = {
  FU: 'pawn',
  GI: 'silver',
  GY: 'tama',
  HI: 'rook',
  KA: 'bishop',
  KE: 'knight',
  KI: 'gold',
  KY: 'lance',
  NG: 'promotedsilver',
  NK: 'promotedknight',
  NY: 'promotedlance',
  OU: 'king',
  RY: 'dragon',
  TO: 'tokin',
  UM: 'horse',
};

function classesWithOrientation(color: string, role: string, flipped: boolean): string {
  if (flipped) {
    if (color === 'sente') {
      return dedent`.sg-wrap.orientation-gote piece.${role}.gote,
      .hand-bottom piece.${role}.gote,
      .spare-bottom piece.${role}.gote`;
    } else {
      return dedent`.sg-wrap.orientation-gote piece.${role}.sente,
      .hand-top piece.${role}.sente,
      .spare-top piece.${role}.sente`;
    }
  } else {
    if (color === 'sente') {
      return dedent`piece.${role}.sente,
      .sg-wrap.orientation-sente piece.${role}.sente,
      .hand-bottom piece.${role}.sente,
      .spare-bottom piece.${role}.sente`;
    } else {
      return dedent`piece.${role}.gote,
      .sg-wrap.orientation-sente piece.${role}.gote,
      .hand-top piece.${role}.gote,
      .spare-top piece.${role}.gote`;
    }
  }
}

function classes(color: string, role: string): string {
  if (color === 'sente') {
    // facing up
    if (role === 'king') {
      return dedent`.sg-wrap.orientation-gote piece.king.gote,
      .spare-bottom piece.king.gote`;
    } else if (role === 'tama') {
      return dedent`piece.king.sente,
      .sg-wrap.orientation-sente piece.king.sente`;
    } else {
      return dedent`piece.${role}.sente,
      .sg-wrap.orientation-sente piece.${role}.sente,
      .sg-wrap.orientation-gote piece.${role}.gote,
      .hand-bottom piece.${role}.gote,
      .spare-bottom piece.${role}.gote`;
    }
  } else {
    // facing down
    if (role === 'king') {
      return dedent`piece.king.gote,
      .sg-wrap.orientation-sente piece.king.gote`;
    } else if (role === 'tama') {
      return dedent`.sg-wrap.orientation-gote piece.king.sente,
      .spare-top piece.king.sente`;
    } else {
      return dedent`piece.${role}.gote,
      .sg-wrap.orientation-sente piece.${role}.gote,
      .sg-wrap.orientation-gote piece.${role}.sente,
      .hand-top piece.${role},
      .spare-top piece.${role}.sente`;
    }
  }
}

function extraCss(name: string, ext: string): string {
  const cssClasses: string[] = [];
  if (name === 'pixel') {
    cssClasses.push('piece { image-rendering: pixelated; }');
    cssClasses.push('.v-chushogi piece, .v-kyotoshogi piece { image-rendering: unset; }');
  }

  if (name === 'characters') {
    cssClasses.push('piece { background-size: contain; }');
    cssClasses.push('.v-chushogi piece, .v-kyotoshogi piece { background-size: cover; }');
  }

  if (ext === 'png') {
    cssClasses.push('piece { will-change: transform; background-repeat: unset; }');
    cssClasses.push(
      '.v-chushogi piece, .v-kyotoshogi piece { will-change: auto; background-repeat: no-repeat; }',
    );
  }
  cssClasses.push('.v-chushogi piece, .v-kyotoshogi piece { background-image: none !important; }');
  return cssClasses.join('\n');
}

export function standard(sourceDir: string, destDir: string): void {
  const roles = Object.keys(roleDict);

  for (const [name, ext] of regular) {
    const cssClasses = colors.flatMap(color =>
      roles.map(role => {
        const piece = `${color === 'sente' ? '0' : '1'}${role}`;
        const file = path.join(sourceDir, name, `${piece}.${ext}`);
        const image = fs.readFileSync(file);
        const base64 = image.toString('base64');
        const cls = classes(color, roleDict[role]);
        return `${cls} {background-image:url('data:image/${types[ext]}${base64}')}`;
      }),
    );

    cssClasses.push(extraCss(name, ext));
    cssClasses.push(''); // trailing new line

    fs.writeFileSync(path.join(destDir, `${name}.css`), cssClasses.join('\n'));
  }

  for (const [name, ext] of bidirectional) {
    const cssClasses = ['-1', ''].flatMap(up =>
      colors.flatMap(color =>
        roles.map(role => {
          const piece = `${color === 'sente' ? '0' : '1'}${role}${up}`;
          const file = path.join(sourceDir, name, `${piece}.${ext}`);
          const image = fs.readFileSync(file);
          const base64 = image.toString('base64');
          const cls = classesWithOrientation(color, roleDict[role], up.length !== 0);
          return `${cls} {background-image:url('data:image/${types[ext]}${base64}')}`;
        }),
      ),
    );

    cssClasses.push(extraCss(name, ext));
    cssClasses.push(''); // trailing new line

    fs.writeFileSync(path.join(destDir, `${name}.css`), cssClasses.join('\n'));
  }
}

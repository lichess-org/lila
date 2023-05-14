import { Position } from 'shogiops/variant/position';
import { forsythToRole, parseSfen } from 'shogiops/sfen';
import { KeyboardMove } from '../main';
import { aimingAt, csaToRole, fromKanjiDigit, kanjiToRole } from 'shogiops/notation/util';
import { makeSquareName, parseSquareName } from 'shogiops/util';
import { Move, Role, Square, SquareSet, isDrop } from 'shogiops';
import { makeJapaneseMove } from 'shogiops/notation/japanese';
import { DrawShape } from 'shogiground/draw';
import { pieceCanPromote, unpromote } from 'shogiops/variant/util';

// NO chushogi support, because of - lion moves and three character long coords

// possible coordinates: 1一; 1a; 11 - all start with number
// full-width or half-width digit - '１' == '1'
// ignore case - 1A == 1a; P-7f == p-7F
//
// Promote unless speficied otherwise (= or 不成)
// Support last dest for 同 or for 銀x

// bb3 b3b
// b32 32b

interface Opts {
  input: HTMLInputElement;
  ctrl: KeyboardMove;
}

const fileR = new RegExp('(?:[１２３４５６７８９]|[一二三四五六七八九]|[1-9])'),
  rankR = new RegExp('(?:[１２３４５６７８９]|[一二三四五六七八九]|[1-9]|[a-i])'),
  keyR = new RegExp(`${fileR.source}${rankR.source}`, 'g'),
  japaneseAmbiguitiesR = new RegExp('左|右|上|行|引|寄|直'),
  allRolesR = new RegExp(
    'fu|ky|ke|gi|ki|ka|hi|to|ny|nk|ng|um|ry|ou|p|l|n|s|g|b|r|k|\\+p|\\+l|\\+n|\\+s|\\+b|\\+r|歩|香|桂|銀|金|角|飛|と|成香|成桂|成銀|馬|龍|王|玉|d|h|t'
  ),
  KKlastDestR = new RegExp(`^(?:${allRolesR.source})x$`);

interface SubmitOpts {
  submitCommand: boolean;
  isTrusted?: boolean;
  yourMove?: boolean;
}
type Submit = (v: string, submitOpts: SubmitOpts) => void;

window.lishogi.keyboardMove = function (opts: Opts) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let pos: Position | null = null,
    lastKey: Key | null | undefined;

  const submit: Submit = (v: string, submitOpts: SubmitOpts) => {
    if (!submitOpts.isTrusted) return;
    v = v.toLowerCase().replace(/\s/g, '');
    if (lastKey) {
      if (KKlastDestR.test(v)) v = v + lastKey;
      else if (v.includes('同')) v = v.replace('同', lastKey);
    }
    const shapes: DrawShape[] = [],
      move = pos && toMove(v, pos);

    if (!submitOpts.submitCommand) {
      if (move) {
        if (isDrop(move))
          shapes.push({ orig: { color: pos!.turn, role: move.role }, dest: makeSquareName(move.to), brush: 'confirm' });
        else
          shapes.push({
            orig: makeSquareName(move.from),
            dest: makeSquareName(move.to),
            brush: 'confirm',
            description: move.promotion ? '+' : undefined,
          });
      } else if (pos) {
        const sqs = regexMatchAllSquares(v),
          m = v.match(allRolesR)?.[0] || '',
          forceDrop = v.includes('*') || v.includes('打');

        const role = toRole(pos.rules, m);

        if (sqs.length === 1) {
          if (role && !forceDrop) {
            for (const sq of allCandidates(sqs[0], role, pos))
              shapes.push({ orig: makeSquareName(sq), dest: makeSquareName(sqs[0]), brush: 'suggest' });
          } else shapes.push({ orig: makeSquareName(sqs[0]), dest: makeSquareName(sqs[0]), brush: 'suggest' });
        } else if (role && Math.abs(m.length - v.length) <= 1) {
          if (!forceDrop) {
            const pieces = Array.from(pos.board.pieces(pos.turn, role));
            for (const piece of pieces)
              shapes.push({ orig: makeSquareName(piece), dest: makeSquareName(piece), brush: 'suggest' });
          }
          if (
            pos.hands.color(pos.turn).get(role) > 0 ||
            pos.hands.color(pos.turn).get(unpromote(pos.rules)(role) || role) > 0
          )
            shapes.push({ orig: { color: pos.turn, role }, dest: { color: pos.turn, role }, brush: 'suggest' });
        }
      }
      if (!v.length) opts.input.classList.toggle('wrong', false);
    } else {
      if (move) {
        if (isDrop(move)) opts.ctrl.drop(makeSquareName(move.to), move.role);
        else opts.ctrl.move(makeSquareName(move.from), makeSquareName(move.to), !!move.promotion);
        clear();
      } else if (v.length > 1 && 'resign'.startsWith(v)) {
        if (v === 'resign') {
          opts.ctrl.resign(true, true);
          clear();
        }
      } else if (v.length > 0 && 'clock'.startsWith(v)) {
        if ('clock' === v) {
          readClocks(opts.ctrl.clock());
          clear();
        }
      } else if (v.length > 0 && 'who'.startsWith(v)) {
        if ('who' === v) {
          readOpponentName();
          clear();
        }
      } else if (v.length > 0 && 'draw'.startsWith(v)) {
        if ('draw' === v) {
          opts.ctrl.draw();
          clear();
        }
      } else if (v.length > 0 && 'next'.startsWith(v)) {
        if ('next' === v) {
          opts.ctrl.next?.();
          clear();
        }
      } else if (v.length > 0 && 'upv'.startsWith(v)) {
        if ('upv' === v) {
          opts.ctrl.vote?.(true);
          clear();
        }
      } else if (v.length > 0 && 'downv'.startsWith(v)) {
        if ('downv' === v) {
          opts.ctrl.vote?.(false);
          clear();
        }
      } else if (submitOpts.yourMove && v.length > 1) {
        setTimeout(window.lishogi.sound.error, 500);
        opts.input.value = '';
      } else {
        const wrong = !!v.length;
        if (wrong && !opts.input.classList.contains('wrong')) window.lishogi.sound.error();
        opts.input.classList.toggle('wrong', wrong);
      }
    }
    opts.ctrl.sg.setAutoShapes(shapes);
  };
  const clear = () => {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  makeBindings(opts, submit, clear);
  return (variant: VariantKey, sfen: string, lastSquare: Square | undefined, yourMove: boolean) => {
    pos = parseSfen(variant, sfen).unwrap();
    lastKey = lastSquare !== undefined ? makeSquareName(lastSquare) : undefined;

    submit(opts.input.value, {
      submitCommand: false,
      isTrusted: true,
      yourMove: yourMove,
    });
  };
};

function makeBindings(opts: any, submit: Submit, clear: () => void) {
  window.Mousetrap.bind('enter', () => opts.input.focus());
  opts.input.addEventListener('keyup', (e: KeyboardEvent) => {
    if (!e.isTrusted) return;
    const v = (e.target as HTMLInputElement).value;

    if (v.includes('/')) {
      focusChat();
      clear();
    } else if (v === '' && e.which == 13) opts.ctrl.confirmMove();
    else
      submit(v, {
        submitCommand: e.which === 13,
        isTrusted: e.isTrusted,
      });
  });
  opts.input.addEventListener('focus', () => opts.ctrl.setFocus(true));
  opts.input.addEventListener('blur', () => opts.ctrl.setFocus(false));
  // prevent default on arrow keys: they only replay moves
  opts.input.addEventListener('keydown', (e: KeyboardEvent) => {
    if (e.which > 36 && e.which < 41) {
      if (e.which == 37) opts.ctrl.jump(-1);
      else if (e.which == 38) opts.ctrl.jump(-999);
      else if (e.which == 39) opts.ctrl.jump(1);
      else opts.ctrl.jump(999);
      e.preventDefault();
    }
  });
}

function fixDigits(str: string): string {
  return str
    .split('')
    .map(c => {
      const charCode = c.charCodeAt(0);
      if (charCode >= 0xff10 && charCode <= 0xff19) {
        return String.fromCharCode(charCode - 0xfee0);
      }
      return fromKanjiDigit(c) || c;
    })
    .join('');
}

function toSquare(str: string): Square | undefined {
  if (str.length !== 2) return;
  const mapped = fixDigits(str),
    secondDigit = parseInt(mapped[1]);

  const numberLetter = secondDigit ? mapped[0] + String.fromCharCode(96 + secondDigit) : mapped,
    parsed = parseSquareName(numberLetter);
  if (parsed !== undefined) return parsed;
  else return;
}

function regexMatchAllSquares(str: string): Square[] {
  const matches: Square[] = [];
  let match;
  while ((match = keyR.exec(str)) !== null) {
    const sq = toSquare(match[0]);
    if (sq) matches.push(sq);
  }
  return matches;
}

function toRole(variant: VariantKey, str: string): Role | undefined {
  if (str.length === 1) {
    str = str.replace('h', '+b').replace('d', '+r');
    if (variant !== 'kyotoshogi') str.replace('t', '+p');
  }
  return forsythToRole(variant)(str) || kanjiToRole(str)[0] || csaToRole(str.toUpperCase());
}

function allCandidates(dest: Square, role: Role, pos: Position): SquareSet {
  return aimingAt(pos, pos.board.pieces(pos.turn, role), dest);
}

function toMove(str: string, pos: Position): Move | undefined {
  const sqs = regexMatchAllSquares(str),
    unpromotion = str.includes('不成') || str.endsWith('='),
    forceDrop = str.includes('*') || str.includes('打');

  if (sqs.length) {
    if (sqs.length === 2) {
      const piece = pos.board.get(sqs[0]);
      if (piece) {
        const move = {
          from: sqs[0],
          to: sqs[1],
          promotion: pieceCanPromote(pos.rules)(piece, sqs[0], sqs[1], undefined) && !unpromotion,
        };
        return pos.isLegal(move) ? move : undefined;
      }
      const pieceReversed = pos.board.get(sqs[1]);
      if (pieceReversed) {
        const move = {
          from: sqs[1],
          to: sqs[0],
          promotion: pieceCanPromote(pos.rules)(pieceReversed, sqs[1], sqs[0], undefined) && !unpromotion,
        };
        return pos.isLegal(move) ? move : undefined;
      }
    } else if (sqs.length === 1) {
      const keyChar = str.match(keyR)![0],
        roleChar = str.replace(keyChar, '').match(allRolesR),
        role = roleChar && toRole(pos.rules, roleChar[0]);

      if (role) {
        const candidates = allCandidates(sqs[0], role, pos),
          piece = { color: pos.turn, role };
        if ((forceDrop || candidates.isEmpty()) && pos.dropDests(piece).has(sqs[0])) {
          const drop = { role, to: sqs[0] };
          return pos.isLegal(drop) ? drop : undefined;
        } else if (candidates.isSingleSquare())
          return {
            from: candidates.first()!,
            to: sqs[0],
            promotion: pieceCanPromote(pos.rules)(piece, candidates.first()!, sqs[0], undefined) && !unpromotion,
          };
        else if (japaneseAmbiguitiesR.test(str)) {
          const amb = str.match(japaneseAmbiguitiesR)!;

          for (const c of candidates) {
            const jpMove = makeJapaneseMove(pos, { from: c, to: sqs[0] })!;
            if (amb.every(a => jpMove.includes(a)))
              return {
                from: c,
                to: sqs[0],
                promotion: pieceCanPromote(pos.rules)(piece, c, sqs[0], undefined) && !unpromotion,
              };
          }
        }
      }
    }
  }
  return;
}

function focusChat() {
  const chatInput = document.querySelector('.mchat .mchat__say') as HTMLInputElement;
  if (chatInput) chatInput.focus();
}

function readClocks(clockCtrl: any | undefined) {
  if (!clockCtrl) return;
  const msgs = ['sente', 'gote'].map(color => {
    const time = clockCtrl.millisOf(color);
    const date = new Date(time);
    const msg =
      (time >= 3600000 ? simplePlural(Math.floor(time / 3600000), 'hour') : '') +
      ' ' +
      simplePlural(date.getUTCMinutes(), 'minute') +
      ' ' +
      simplePlural(date.getUTCSeconds(), 'second');
    return `${color}: ${msg}`;
  });
  window.lishogi.sound.say({ en: msgs.join('. ') });
}

function readOpponentName(): void {
  const opponentName = document.querySelector('.ruser-top name') as HTMLInputElement;
  window.lishogi.sound.say({ en: opponentName.innerText.split('\n')[0] });
}

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb != 1 ? 's' : ''}`;
}

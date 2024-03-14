import { Position } from 'shogiops/variant/position';
import { parseSfen } from 'shogiops/sfen';
import { KeyboardMove } from '../main';
import { Board } from 'shogiops/board';
import { makeSquareName, opposite } from 'shogiops/util';
import { RoleMap, Square, isDrop } from 'shogiops/types';
import { DrawShape } from 'shogiground/draw';
import { unpromote } from 'shogiops/variant/util';
import { KKlastDestR, allCandidates, allRolesR, regexMatchAllSquares, toMoveOrDrop, toRole } from './util';

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

interface SubmitOpts {
  submitCommand: boolean;
  isTrusted?: boolean;
}
type Submit = (v: string, submitOpts: SubmitOpts) => void;

window.lishogi.keyboardMove = function (opts: Opts) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');
  let pos: Position | null = null,
    lastKey: Key | null | undefined,
    canPlay: boolean | undefined;

  const submit: Submit = (v: string, submitOpts: SubmitOpts) => {
    opts.input.classList.remove('wrong');

    if (!submitOpts.isTrusted) return;
    v = v.toLowerCase().replace(/\s/g, '');

    if (v === '' && submitOpts.submitCommand) {
      if (canPlay) opts.ctrl.confirmMove();
      else opts.ctrl.sg.cancelMoveOrDrop();
      return;
    }

    if (lastKey) {
      if (KKlastDestR.test(v)) v = v + lastKey;
      else if (v.includes('同')) v = v.replace('同', lastKey);
    }
    const shapes: DrawShape[] = [],
      move = pos && toMoveOrDrop(v, pos);

    if (!submitOpts.submitCommand) {
      if (move) {
        if (isDrop(move))
          shapes.push({ orig: { color: pos!.turn, role: move.role }, dest: makeSquareName(move.to), brush: 'confirm' });
        else
          shapes.push({
            orig: makeSquareName(move.from),
            dest: makeSquareName(move.to),
            brush: `${!canPlay ? 'pre-' : ''}confirm`,
            description: move.promotion ? '+' : undefined,
          });
      } else if (pos) {
        const sqs = regexMatchAllSquares(v),
          m = v.match(allRolesR)?.[0] || '',
          forceDrop = v.includes('*') || v.includes('打'),
          role = toRole(pos.rules, m),
          brush = `${!canPlay ? 'pre-' : ''}suggest`;

        if (sqs.length === 1) {
          if (role && !forceDrop) {
            for (const sq of allCandidates(sqs[0], role, pos))
              shapes.push({ orig: makeSquareName(sq), dest: makeSquareName(sqs[0]), brush });
          } else shapes.push({ orig: makeSquareName(sqs[0]), dest: makeSquareName(sqs[0]), brush });
        } else if (role && Math.abs(m.length - v.length) <= 1) {
          if (!forceDrop) {
            const pieces = Array.from(pos.board.pieces(pos.turn, role));
            for (const piece of pieces)
              shapes.push({ orig: makeSquareName(piece), dest: makeSquareName(piece), brush });
          }
          if (
            pos.hands.color(pos.turn).get(role) > 0 ||
            pos.hands.color(pos.turn).get(unpromote(pos.rules)(role) || role) > 0
          )
            shapes.push({ orig: { color: pos.turn, role }, dest: { color: pos.turn, role }, brush });
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
          opts.ctrl.resign(true);
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
      } else {
        const wrong = !!v.length;
        if (wrong && !opts.input.classList.contains('wrong')) window.lishogi.sound.error();
        opts.input.classList.toggle('wrong', wrong);
      }
    }
    if (!opts.input.classList.contains('wrong')) opts.ctrl.sg.setAutoShapes(shapes);
  };
  const clear = () => {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  makeBindings(opts, submit, clear);
  return (variant: VariantKey, sfen: string, lastSquare: Square | undefined, yourMove: boolean) => {
    pos = parseSfen(variant, sfen).unwrap();
    lastKey = lastSquare !== undefined ? makeSquareName(lastSquare) : undefined;
    canPlay = yourMove;
    // premove/predrop
    if (!canPlay) {
      pos.turn = opposite(pos.turn);
      const myOccupied = pos.board.color(pos.turn),
        roleMap: RoleMap = new Map();

      for (const r of pos.board.presentRoles()) {
        roleMap.set(r, pos.board.role(r).intersect(myOccupied));
      }

      pos.board = Board.from(myOccupied, [[pos.turn, myOccupied]], roleMap);
    }

    submit(opts.input.value, {
      submitCommand: false,
      isTrusted: true,
    });
  };
};

let initiated = false; // so that enter keydown to focus, doesn't trigger keyup to submit
function makeBindings(opts: any, submit: Submit, clear: () => void) {
  window.Mousetrap.bind('enter', () => opts.input.focus());
  opts.input.addEventListener('keyup', (e: KeyboardEvent) => {
    if (!e.isTrusted) return;
    const v = (e.target as HTMLInputElement).value,
      submitCommand = e.which === 13 && initiated;

    if (v.includes('/')) {
      focusChat();
      clear();
    } else
      submit(v, {
        submitCommand: submitCommand,
        isTrusted: e.isTrusted,
      });
  });
  opts.input.addEventListener('focus', () => opts.ctrl.setFocus(true));
  opts.input.addEventListener('blur', () => {
    opts.ctrl.setFocus(false);
    initiated = false;
  });
  // prevent default on arrow keys: they only replay moves
  opts.input.addEventListener('keydown', (e: KeyboardEvent) => {
    initiated = true;
    if (e.which > 36 && e.which < 41) {
      if (e.which === 37) opts.ctrl.jump(-1);
      else if (e.which === 38) opts.ctrl.jump(-999);
      else if (e.which === 39) opts.ctrl.jump(1);
      else opts.ctrl.jump(999);
      e.preventDefault();
    }
  });
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

import { DropDests, MoveDests } from 'shogiground/types';
import { Board } from 'shogiops';
import { parseSfen, roleToForsyth } from 'shogiops/sfen';
import { PieceName, Role } from 'shogiops/types';
import { parsePieceName, parseSquare } from 'shogiops/util';
import { KeyboardMove } from '../keyboardMove';

// TODO
// we need to support diff board sizes and also different notations

// plan:
// move this to its own workplace, so we can also support kb moves in
// puzzles and analysis.
// Prob add parser for each notation to shogiops?,
// something like (pos: Position, notation: string) => MoveOrDrop
// til then...

const promotableRegex = /^[PLNSBR]?[1-9]?[1-9]?[-x]?[1-9][1-9]$/;
const pawnRegex = /^(?:\*|[-x][1-9]?[1-9]?)?[1-9][1-9][\+=]$/;

const numberR = new RegExp('(?:[１２３４５６７８９]|[一二三四五六七八九]|[1-9]|[a-i])');
const keyR = new RegExp(`${numberR.source}${numberR.source}`);
const promotionR = new RegExp('\\+|=');
const japanesePromotionR = new RegExp('成|不成');
const japaneseAmbiguityR = new RegExp('打|左|右|上|行|引|寄|直');
const allRolesR = new RegExp(
  'P|L|N|S|G|B|R|K|\\+P|\\+L|\\+N|\\+S|\\+B|\\+R|歩|香|桂|銀|金|角|飛|と|成香|成桂|成銀|馬|龍|王|玉'
);

const usiR = new RegExp(`^((?:${keyR.source})|(?:${allRolesR.source}))(${keyR.source})(${promotionR.source})?$`);
const westernR = new RegExp(
  `(${allRolesR.source})\\(?(${numberR.source})(${numberR.source})\\)?(?:-|x)?(${keyR.source})(${promotionR.source})?`
);
const japaneseR = new RegExp(
  `((?:${keyR.source})|同　)${allRolesR.source}(${japaneseAmbiguityR.source})*(${japanesePromotionR.source})?`
);

interface SubmitOpts {
  isTrusted?: boolean;
  force?: boolean;
  yourMove?: boolean;
}
type Submit = (v: string, submitOpts: SubmitOpts) => void;

window.lishogi.keyboardMove = function (opts: { input: HTMLInputElement; ctrl: KeyboardMove }) {
  if (opts.input.classList.contains('ready')) return;
  opts.input.classList.add('ready');

  //let eUsis: string[];

  const submit: Submit = function (v: string, submitOpts: SubmitOpts) {
    if (!submitOpts.isTrusted) return;
    console.log('submit', v, submitOpts);
    if (v.match(pawnRegex)) v = 'P' + v;
    // player pressed <Enter> instead of '+' to force promotion
    if (v.match(promotableRegex) && submitOpts.force) v += '+';

    // check length before pattern in case pattern is wrong or underperforms
    // pattern protects against accidental submission of S6-57 (during S6-5)
    const foundUsi = v.length >= 4 && v; // notationToUsi(v, eUsis);
    const selectedKey = opts.ctrl.hasSelected() || '';
    console.log('fu:', foundUsi);

    const usiMatch = v.match(usiR),
      wMatch = !usiMatch && v.match(westernR),
      jMatch = !usiMatch && !wMatch && v.match(japaneseR);
    if (usiMatch) opts.ctrl.move(usiMatch[1] as Key, usiMatch[2] as Key, usiMatch[3] === '+');
    else if (wMatch || jMatch) {
      console.log('macthes', wMatch, jMatch);
    } else if (keyR.test(v)) {
      opts.ctrl.select(alpha(v));
      if (selectedKey) {
      } else {
        clear();
      }
    } else if (v.length > 0 && 'clock'.startsWith(v.toLowerCase())) {
      if ('clock' === v.toLowerCase()) {
        readClocks(opts.ctrl.clock());
        clear();
      }
    } else if (numberR.test(v)) {
      // do nothing...
    } else if (submitOpts.yourMove && v.length > 1) {
      setTimeout(window.lishogi.sound.error, 500);
      opts.input.value = '';
    } else {
      const wrong = !!v.length;
      if (wrong && !opts.input.classList.contains('wrong')) window.lishogi.sound.error();
      opts.input.classList.toggle('wrong', wrong);
    }
  };
  const clear = function () {
    opts.input.value = '';
    opts.input.classList.remove('wrong');
  };
  makeBindings(opts, submit, clear);
  return function (sfen: string, dests: MoveDests | undefined, dropDests: DropDests | undefined, yourMove: boolean) {
    const board = parseSfen('standard', sfen).unwrap().board;
    const eUsis = createExtendedUsi(board, dests || new Map(), dropDests || new Map());
    console.log('kkkb:', eUsis);
    submit(opts.input.value, {
      isTrusted: true,
      yourMove: yourMove,
    });
  };
};

function makeBindings(opts: any, submit: Submit, clear: Function) {
  window.Mousetrap.bind('enter', function () {
    opts.input.focus();
  });
  opts.input.addEventListener('keyup', function (e: KeyboardEvent) {
    const v = (e.target as HTMLInputElement).value;
    if (v.includes('/')) {
      focusChat();
      clear();
    } else if (v === '' && e.which == 13) opts.ctrl.confirmMove();
    else
      submit(v, {
        force: e.which === 13,
        isTrusted: e.isTrusted,
      });
  });
  opts.input.addEventListener('focus', function () {
    opts.ctrl.setFocus(true);
  });
  opts.input.addEventListener('blur', function () {
    opts.ctrl.setFocus(false);
  });
  // prevent default on arrow keys: they only replay moves
  opts.input.addEventListener('keydown', function (e: KeyboardEvent) {
    if (e.which > 36 && e.which < 41) {
      if (e.which == 37) opts.ctrl.jump(-1);
      else if (e.which == 38) opts.ctrl.jump(-999);
      else if (e.which == 39) opts.ctrl.jump(1);
      else opts.ctrl.jump(999);
      e.preventDefault();
    }
  });
}

function alpha(coordinate: string): Key {
  // "97" -> "a3"
  return coordinate as Key;
}

//function findCandidates(piece: Piece, pieces: Pieces): Key[] {
//  return [...pieces].filter(([_key, boardPiece]) => samePiece(piece, boardPiece)).map(([key, _piece]) => key);
//}

function createExtendedUsi(board: Board, dests: MoveDests, dropDests: DropDests): string[] {
  const eUsis: string[] = [];
  for (const [orig, d] of dests) {
    const role = board.get(parseSquare(orig))?.role as Role,
      roleStr = role && roleToForsyth('standard')(role);
    if (roleStr) {
      d.forEach(dest => eUsis.push(roleStr + orig + dest));
    }
  }
  for (const [pn, d] of dropDests) {
    const role = parsePieceName(pn as PieceName)!.role;
    d.forEach(dest => eUsis.push(roleToForsyth('standard')(role) + '*' + dest));
  }
  return eUsis;
}

function focusChat() {
  const chatInput = document.querySelector('.mchat .mchat__say') as HTMLInputElement;
  if (chatInput) chatInput.focus();
}

function readClocks(clockCtrl: any | undefined) {
  console.log(clockCtrl);

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

function simplePlural(nb: number, word: string) {
  return `${nb} ${word}${nb != 1 ? 's' : ''}`;
}

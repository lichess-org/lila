// Minimal SAN player: just enough chess to animate opening lines on a board.
// Tracks piece identities so the view can slide pieces between squares.
(() => {
  const FILES = 'abcdefgh';
  const BACK = 'RNBQKBNR';
  const sqIndex = s => FILES.indexOf(s[0]) + (parseInt(s[1], 10) - 1) * 8;
  const fileOf = i => i % 8;
  const rankOf = i => Math.floor(i / 8);

  class Position {
    constructor() {
      this.board = new Array(64).fill(null);
      this.turn = 'w';
      this.ep = null;
      let id = 0;
      for (let f = 0; f < 8; f++) {
        this.board[f] = { c: 'w', t: BACK[f], id: id++ };
        this.board[8 + f] = { c: 'w', t: 'P', id: id++ };
        this.board[48 + f] = { c: 'b', t: 'P', id: id++ };
        this.board[56 + f] = { c: 'b', t: BACK[f], id: id++ };
      }
    }

    pieces() {
      return this.board.map((p, sq) => p && { ...p, sq }).filter(Boolean);
    }

    // can the piece on `from` attack `to` (ignores what is on `to`)
    attacks(from, to, board = this.board) {
      const p = board[from];
      if (!p || from === to) return false;
      const dx = fileOf(to) - fileOf(from),
        dy = rankOf(to) - rankOf(from);
      const ax = Math.abs(dx),
        ay = Math.abs(dy);
      const clear = () => {
        const sx = Math.sign(dx),
          sy = Math.sign(dy);
        for (let i = 1; i < Math.max(ax, ay); i++) if (board[from + sx * i + sy * i * 8]) return false;
        return true;
      };
      switch (p.t) {
        case 'P':
          return ax === 1 && dy === (p.c === 'w' ? 1 : -1);
        case 'N':
          return (ax === 1 && ay === 2) || (ax === 2 && ay === 1);
        case 'B':
          return ax === ay && clear();
        case 'R':
          return (ax === 0 || ay === 0) && clear();
        case 'Q':
          return (ax === ay || ax === 0 || ay === 0) && clear();
        case 'K':
          return Math.max(ax, ay) === 1;
      }
      return false;
    }

    inCheckAfter(from, to, color) {
      const b = this.board.slice();
      b[to] = b[from];
      b[from] = null;
      const king = b.findIndex(p => p && p.c === color && p.t === 'K');
      return b.some((p, i) => p && p.c !== color && this.attacks(i, king, b));
    }

    // returns { moves: [{id, from, to}], captured: id|null, promoted: {id, t}|null, from, to }
    play(rawSan) {
      const san = rawSan.replace(/[+#!?]/g, '');
      const c = this.turn;
      const dir = c === 'w' ? 1 : -1;
      const home = c === 'w' ? 0 : 56;
      const result = { moves: [], captured: null, promoted: null };
      const move = (from, to) => {
        result.moves.push({ id: this.board[from].id, from, to });
        this.board[to] = this.board[from];
        this.board[from] = null;
      };

      if (san.startsWith('O-O')) {
        const long = san.startsWith('O-O-O');
        move(home + 4, home + (long ? 2 : 6));
        move(home + (long ? 0 : 7), home + (long ? 3 : 5));
        result.from = home + 4;
        result.to = home + (long ? 2 : 6);
        this.ep = null;
        this.turn = c === 'w' ? 'b' : 'w';
        return result;
      }

      const m = san.match(/^([NBRQK])?([a-h])?([1-8])?(x)?([a-h][1-8])(=?([NBRQ]))?$/);
      if (!m) throw new Error('Cannot parse SAN ' + rawSan);
      const [, pieceLetter, dFile, dRank, , dest, , promo] = m;
      const type = pieceLetter || 'P';
      const to = sqIndex(dest);
      let from = -1;

      if (type === 'P' && !m[4]) {
        const one = this.board[to - 8 * dir];
        if (one?.t === 'P' && one.c === c) from = to - 8 * dir;
        else from = to - 16 * dir;
      } else {
        const candidates = [];
        this.board.forEach((p, i) => {
          if (!p || p.c !== c || p.t !== type) return;
          if (dFile && FILES[fileOf(i)] !== dFile) return;
          if (dRank && String(rankOf(i) + 1) !== dRank) return;
          if (!this.attacks(i, to)) return;
          candidates.push(i);
        });
        from = candidates.find(i => !this.inCheckAfter(i, to, c)) ?? candidates[0] ?? -1;
      }
      if (from < 0 || !this.board[from]) throw new Error('Illegal SAN ' + rawSan);

      if (this.board[to]) result.captured = this.board[to].id;
      else if (type === 'P' && to === this.ep) {
        const victim = to - 8 * dir;
        result.captured = this.board[victim].id;
        this.board[victim] = null;
      }
      this.ep = type === 'P' && Math.abs(to - from) === 16 ? from + 8 * dir : null;
      move(from, to);
      if (promo) {
        this.board[to] = { ...this.board[to], t: promo };
        result.promoted = { id: this.board[to].id, t: promo };
      }
      result.from = from;
      result.to = to;
      this.turn = c === 'w' ? 'b' : 'w';
      return result;
    }
  }

  const parsePgn = pgn =>
    pgn
      .replace(/\{[^}]*\}/g, '')
      .split(/\s+/)
      .filter(t => t && !/^\d+\.+$/.test(t) && !/^(1-0|0-1|1\/2-1\/2|\*)$/.test(t))
      .map(t => t.replace(/^\d+\.+/, ''));

  window.MiniChess = { Position, parsePgn, fileOf, rankOf };
})();

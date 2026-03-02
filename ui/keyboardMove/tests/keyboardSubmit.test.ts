import { beforeEach, describe, test } from 'node:test';
import assert from 'node:assert/strict';
import { type Prop, propWithEffect } from 'lib/index';
import { makeSubmit } from '../src/keyboardSubmit.js';
import { destsToUcis, sanWriter } from 'lib/game';

function spy() {
  const f: any = (...args: any[]) => {
    f.calls.push(args);
  };
  f.calls = [] as any[];
  f.calledTimes = (n: number) => assert.equal(f.calls.length, n);
  f.calledWith = (...args: any[]) => assert.deepEqual(f.calls[0], args);
  return f;
}

const startingFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';
const toDestsMap = (obj: object) => new Map(Object.entries(obj)) as Dests;
const fenDestsToSans = (fen: string, dests: Record<string, string[]>) =>
  sanWriter(fen, destsToUcis(toDestsMap(dests)));

const unexpectedErrorThrower = (name: string) => () => {
  throw new Error(`unexpected call to ${name}()`);
};
const defaultCtrl = {
  speakClock: unexpectedErrorThrower('clock'),
  goBerserk: unexpectedErrorThrower('berserk'),
  confirmMove: () => null,
  draw: unexpectedErrorThrower('draw'),
  next: unexpectedErrorThrower('next'),
  vote: unexpectedErrorThrower('vote'),
  drop: unexpectedErrorThrower('drop'),
  hasSelected: () => undefined as any,
  arrowNavigate: unexpectedErrorThrower('arrowNavigate'),
  justSelected: () => true,
  promote: unexpectedErrorThrower('promote'),
  registerHandler: () => null,
  resign: unexpectedErrorThrower('resign'),
  san: unexpectedErrorThrower('san'),
  select: unexpectedErrorThrower('select'),
  update: () => null,
  usedSan: true,
  legalSans: fenDestsToSans(startingFen, {}),
  helpModalOpen: propWithEffect(false, () => null),
  isFocused: propWithEffect(false, () => null),
};
const defaultClear = unexpectedErrorThrower('clear');

describe('keyboardSubmit', () => {
  let mockClear = spy();

  beforeEach(() => {
    mockClear = spy();
  });

  test('resigns game', () => {
    const mockResign = spy();
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, resign: mockResign } },
      mockClear,
    );
    submit('resign', { isTrusted: true });
    mockResign.calledTimes(1);
    mockClear.calledTimes(1);
  });

  test('draws game', () => {
    const mockDraw = spy();
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, draw: mockDraw } },
      mockClear,
    );
    submit('draw', { isTrusted: true });
    mockDraw.calledTimes(1);
    mockClear.calledTimes(1);
  });

  test('goes to next puzzle', () => {
    const mockNext = spy();
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, next: mockNext } },
      mockClear,
    );
    submit('next', { isTrusted: true });
    mockNext.calledTimes(1);
    mockClear.calledTimes(1);
  });

  test('up votes puzzle', () => {
    const mockVote = spy();
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, vote: mockVote } },
      mockClear,
    );
    submit('upv', { isTrusted: true });
    mockVote.calledTimes(1);
    mockVote.calledWith(true);
    mockClear.calledTimes(1);
  });

  test('down votes puzzle', () => {
    const mockVote = spy();
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, vote: mockVote } },
      mockClear,
    );
    submit('downv', { isTrusted: true });
    mockVote.calledTimes(1);
    mockVote.calledWith(false);
    mockClear.calledTimes(1);
  });

  test('reads out clock', () => {
    const mockSpeakClock = spy();
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, speakClock: mockSpeakClock } },
      mockClear,
    );
    submit('clock', { isTrusted: true });
    mockSpeakClock.calledTimes(1);
    mockClear.calledTimes(1);
  });

  test('berserks a game', () => {
    const mockGoBerserk = spy();
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, goBerserk: mockGoBerserk } },
      mockClear,
    );
    submit('zerk', { isTrusted: true });
    mockGoBerserk.calledTimes(1);
    mockClear.calledTimes(1);
  });

  test('speaks opponent name', () => {
    const say = spy();
    (globalThis as any).site = { sound: { say } };
    const submit = makeSubmit(
      { input: document.createElement('input'), ctrl: { ...defaultCtrl, opponent: 'opponent-name' } },
      mockClear,
    );
    submit('who', { isTrusted: true });
    say.calledTimes(1);
    say.calledWith('opponent-name', false, true);
  });

  test('opens help modal with ?', () => {
    const mockSetHelpModalOpen = spy();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: { ...defaultCtrl, helpModalOpen: mockSetHelpModalOpen as unknown as Prop<boolean> },
      },
      mockClear,
    );
    submit('?', { isTrusted: true });
    mockSetHelpModalOpen.calledTimes(1);
    mockSetHelpModalOpen.calledWith(true);
    mockClear.calledTimes(1);
  });

  describe('from starting position', () => {
    test('plays e4 via SAN', () => {
      const mockSan = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: { ...defaultCtrl, legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }), san: mockSan },
        },
        mockClear,
      );
      submit('e4', { isTrusted: true });
      mockSan.calledTimes(1);
      mockSan.calledWith('e2', 'e4');
      mockClear.calledTimes(1);
    });

    test('selects e2 via UCI', () => {
      const mockSelect = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
            select: mockSelect,
          },
        },
        mockClear,
      );
      submit('e2', { isTrusted: true });
      mockSelect.calledTimes(1);
      mockSelect.calledWith('e2');
      mockClear.calledTimes(1);
    });

    test('with e2 selected, plays e4 via UCI', () => {
      const mockSan = spy(),
        mockSelect = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
            hasSelected: () => 'e2',
            san: mockSan,
            select: mockSelect,
          },
        },
        mockClear,
      );
      submit('e4', { isTrusted: true });
      mockSelect.calledTimes(1);
      mockSelect.calledWith('e4');
      mockSan.calledTimes(1);
      mockSan.calledWith('e2', 'e4');
      mockClear.calledTimes(1);
    });

    test('selects e2 via ICCF', () => {
      const mockSelect = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
            select: mockSelect,
          },
        },
        mockClear,
      );
      submit('52', { isTrusted: true });
      mockSelect.calledTimes(1);
      mockSelect.calledWith('e2');
      mockClear.calledTimes(1);
    });

    test('with e2 selected, plays e4 via ICCF', () => {
      const mockSan = spy(),
        mockSelect = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
            hasSelected: () => 'e2',
            san: mockSan,
            select: mockSelect,
          },
        },
        mockClear,
      );
      submit('54', { isTrusted: true });
      mockSelect.calledTimes(1);
      mockSelect.calledWith('e4');
      mockSan.calledTimes(1);
      mockSan.calledWith('e2', 'e4');
      mockClear.calledTimes(1);
    });
  });

  describe('from a position where a b-file pawn or bishop can capture', () => {
    const ambiguousPawnBishopCapture = '4k3/8/8/8/8/2r5/1P1B4/4K3 w - - 0 1';

    test('does pawn capture', () => {
      const mockSan = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(ambiguousPawnBishopCapture, { b2: ['c3'], d2: ['c3'] }),
            hasSelected: () => 'e2',
            san: mockSan,
          },
        },
        mockClear,
      );
      submit('bc3', { isTrusted: true });
      mockSan.calledTimes(1);
      mockSan.calledWith('b2', 'c3');
      mockClear.calledTimes(1);
    });

    test('does bishop capture', () => {
      const mockSan = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(ambiguousPawnBishopCapture, { b2: ['c3'], d2: ['c3'] }),
            san: mockSan,
          },
        },
        mockClear,
      );
      submit('Bc3', { isTrusted: true });
      mockSan.calledTimes(1);
      mockSan.calledWith('d2', 'c3');
      mockClear.calledTimes(1);
    });
  });

  describe('from an ambiguous castling position', () => {
    const ambiguousCastlingFen = '4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1';

    test('does not castle short', () => {
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: { ...defaultCtrl, legalSans: fenDestsToSans(ambiguousCastlingFen, { e1: ['c1', 'g1'] }) },
        },
        mockClear,
      );
      submit('o-o', { isTrusted: true });
      assert.equal(mockClear.calls.length, 0);
    });

    test('does castle long', () => {
      const mockSan = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(ambiguousCastlingFen, { e1: ['c1', 'g1'] }),
            san: mockSan,
          },
        },
        mockClear,
      );
      submit('o-o-o', { isTrusted: true });
      mockSan.calledTimes(1);
      mockSan.calledWith('e1', 'c1');
      mockClear.calledTimes(1);
    });
  });

  describe('from a position where a pawn can promote multiple ways', () => {
    const promotablePawnFen = 'r3k3/1P6/8/8/8/8/8/4K3 w - - 0 1';

    test('with no piece specified does not promote by advancing', () => {
      const mockPromote = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
            promote: mockPromote,
          },
        },
        mockClear,
      );
      submit('b8', { isTrusted: true });
      mockPromote.calledTimes(0);
      assert.equal(mockClear.calls.length, 0);
    });

    test('with piece specified does promote by advancing', () => {
      const mockPromote = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
            promote: mockPromote,
          },
        },
        mockClear,
      );
      submit('b8=q', { isTrusted: true });
      mockPromote.calledTimes(1);
      mockClear.calledTimes(1);
    });

    test('with no piece specified does not promote by capturing', () => {
      const mockPromote = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
            promote: mockPromote,
          },
        },
        mockClear,
      );
      submit('ba8', { isTrusted: true });
      mockPromote.calledTimes(0);
      assert.equal(mockClear.calls.length, 0);
    });

    test('with piece specified does promote by capturing', () => {
      const mockPromote = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
            promote: mockPromote,
          },
        },
        mockClear,
      );
      submit('ba8=b', { isTrusted: true });
      mockPromote.calledTimes(1);
      mockClear.calledTimes(1);
    });

    describe('with pawn selected', () => {
      test('with no piece specified does not promote by advancing', () => {
        const mockPromote = spy();
        const submit = makeSubmit(
          {
            input: document.createElement('input'),
            ctrl: {
              ...defaultCtrl,
              legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
              hasSelected: () => 'b7',
              promote: mockPromote,
            },
          },
          mockClear,
        );
        submit('b8', { isTrusted: true });
        mockPromote.calledTimes(0);
        assert.equal(mockClear.calls.length, 0);
      });

      test('with piece specified does promote by advancing', () => {
        const mockPromote = spy();
        const submit = makeSubmit(
          {
            input: document.createElement('input'),
            ctrl: {
              ...defaultCtrl,
              legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
              hasSelected: () => 'b7',
              promote: mockPromote,
            },
          },
          mockClear,
        );
        submit('b8=r', { isTrusted: true });
        mockPromote.calledTimes(1);
        mockClear.calledTimes(1);
      });

      test('with no piece specified does not promote by capturing', () => {
        const mockPromote = spy();
        const submit = makeSubmit(
          {
            input: document.createElement('input'),
            ctrl: {
              ...defaultCtrl,
              legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
              hasSelected: () => 'b7',
              promote: mockPromote,
            },
          },
          mockClear,
        );
        submit('ba8', { isTrusted: true });
        mockPromote.calledTimes(0);
        assert.equal(mockClear.calls.length, 0);
      });

      test('with piece specified does promote by capturing', () => {
        const mockPromote = spy();
        const submit = makeSubmit(
          {
            input: document.createElement('input'),
            ctrl: {
              ...defaultCtrl,
              legalSans: fenDestsToSans(promotablePawnFen, { b7: ['a8', 'b8'] }),
              hasSelected: () => 'b7',
              promote: mockPromote,
            },
          },
          mockClear,
        );
        submit('a8=n', { isTrusted: true });
        mockPromote.calledTimes(1);
        mockClear.calledTimes(1);
      });
    });
  });

  describe('in crazyhouse variant', () => {
    test('with incomplete crazyhouse entry does nothing', () => {
      const mockDrop = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: { ...defaultCtrl, legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }), drop: mockDrop },
        },
        mockClear,
      );
      submit('Q@a', { isTrusted: true });
      mockDrop.calledTimes(0);
      assert.equal(mockClear.calls.length, 0);
    });

    test('with complete crazyhouse entry does a drop', () => {
      const mockDrop = spy();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: { ...defaultCtrl, legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }), drop: mockDrop },
        },
        mockClear,
      );
      submit('Q@a5', { isTrusted: true });
      mockDrop.calledTimes(1);
      mockClear.calledTimes(1);
    });
  });

  test('with incorrect entry marks it wrong', () => {
    const play = spy();
    (globalThis as any).site = { sound: { play } };
    const input = document.createElement('input');
    const submit = makeSubmit(
      { input, ctrl: { ...defaultCtrl, legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }) } },
      defaultClear,
    );
    submit('j4', { isTrusted: true });
    assert.equal(input.classList.contains('wrong'), true);
    play.calledTimes(1);
    play.calledWith('error');
  });
});

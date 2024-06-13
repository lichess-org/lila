import { beforeEach, describe, expect, test, vi } from 'vitest';
import { Prop, propWithEffect } from 'common';
import { makeSubmit } from '../src/keyboardSubmit';
import { Dests, destsToUcis, sanWriter } from 'chess';

// Tips for working with this file:
// - tests will often require a FEN position and a map of legal moves, e.g.:
//     legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
// - you need not actually supply all of the legal moves, just a relevant subset for the test
// - use https://lichess.org/editor to create positions and get their FENs
// - use https://lichess.org/editor/<FEN> to check what FENs look like

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
  hasSelected: () => undefined,
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
  let mockClear = vi.fn();

  beforeEach(() => {
    mockClear = vi.fn();
  });

  test('resigns game', () => {
    const mockResign = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          resign: mockResign,
        },
      },
      mockClear,
    );

    submit('resign', { isTrusted: true });

    expect(mockResign).toHaveBeenCalledTimes(1);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  test('draws game', () => {
    const mockDraw = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          draw: mockDraw,
        },
      },
      mockClear,
    );

    submit('draw', { isTrusted: true });

    expect(mockDraw).toHaveBeenCalledTimes(1);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  test('goes to next puzzle', () => {
    const mockNext = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          next: mockNext,
        },
      },
      mockClear,
    );

    submit('next', { isTrusted: true });

    expect(mockNext).toHaveBeenCalledTimes(1);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  test('up votes puzzle', () => {
    const mockVote = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          vote: mockVote,
        },
      },
      mockClear,
    );

    submit('upv', { isTrusted: true });

    expect(mockVote).toHaveBeenCalledTimes(1);
    expect(mockVote).toBeCalledWith(true);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  test('down votes puzzle', () => {
    const mockVote = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          vote: mockVote,
        },
      },
      mockClear,
    );

    submit('downv', { isTrusted: true });

    expect(mockVote).toHaveBeenCalledTimes(1);
    expect(mockVote).toBeCalledWith(false);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  test('reads out clock', () => {
    const mockSpeakClock = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          speakClock: mockSpeakClock,
        },
      },
      mockClear,
    );

    submit('clock', { isTrusted: true });

    expect(mockSpeakClock).toHaveBeenCalledTimes(1);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  test('berserks a game', () => {
    const mockGoBerserk = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          goBerserk: mockGoBerserk,
        },
      },
      mockClear,
    );

    submit('zerk', { isTrusted: true });

    expect(mockGoBerserk).toHaveBeenCalledTimes(1);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  test('speaks opponent name', () => {
    vi.stubGlobal('site', { sound: { say: vi.fn() } });

    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          opponent: 'opponent-name',
        },
      },
      mockClear,
    );

    submit('who', { isTrusted: true });
    expect(site.sound.say).toHaveBeenCalledTimes(1);
    expect(site.sound.say).toBeCalledWith('opponent-name', false, true);
  });

  test('opens help modal with ?', () => {
    const mockSetHelpModalOpen = vi.fn();
    const submit = makeSubmit(
      {
        input: document.createElement('input'),
        ctrl: {
          ...defaultCtrl,
          helpModalOpen: mockSetHelpModalOpen as Prop<boolean>,
        },
      },
      mockClear,
    );

    submit('?', { isTrusted: true });

    expect(mockSetHelpModalOpen).toHaveBeenCalledTimes(1);
    expect(mockSetHelpModalOpen).toBeCalledWith(true);
    expect(mockClear).toHaveBeenCalledTimes(1);
  });

  describe('from starting position', () => {
    test('plays e4 via SAN', () => {
      const mockSan = vi.fn();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
            san: mockSan,
          },
        },
        mockClear,
      );

      submit('e4', { isTrusted: true });

      expect(mockSan).toHaveBeenCalledTimes(1);
      expect(mockSan).toBeCalledWith('e2', 'e4');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });

    test('selects e2 via UCI', () => {
      const mockSelect = vi.fn();
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

      expect(mockSelect).toHaveBeenCalledTimes(1);
      expect(mockSelect).toBeCalledWith('e2');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });

    test('with e2 selected, plays e4 via UCI', () => {
      const mockSan = vi.fn();
      const mockSelect = vi.fn();
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

      // is it intended behavior to call both select and san?
      expect(mockSelect).toHaveBeenCalledTimes(1);
      expect(mockSelect).toBeCalledWith('e4');
      expect(mockSan).toHaveBeenCalledTimes(1);
      expect(mockSan).toBeCalledWith('e2', 'e4');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });

    test('selects e2 via ICCF', () => {
      const mockSelect = vi.fn();
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

      expect(mockSelect).toHaveBeenCalledTimes(1);
      expect(mockSelect).toBeCalledWith('e2');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });

    test('with e2 selected, plays e4 via ICCF', () => {
      const mockSan = vi.fn();
      const mockSelect = vi.fn();
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

      // is it intended behavior to call both select and san?
      expect(mockSelect).toHaveBeenCalledTimes(1);
      expect(mockSelect).toBeCalledWith('e4');
      expect(mockSan).toHaveBeenCalledTimes(1);
      expect(mockSan).toBeCalledWith('e2', 'e4');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });
  });

  describe('from a position where a b-file pawn or bishop can capture', () => {
    const ambiguousPawnBishopCapture = '4k3/8/8/8/8/2r5/1P1B4/4K3 w - - 0 1';

    test('does pawn capture', () => {
      const mockSan = vi.fn();
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

      expect(mockSan).toHaveBeenCalledTimes(1);
      expect(mockSan).toBeCalledWith('b2', 'c3');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });

    test('does bishop capture', () => {
      const mockSan = vi.fn();
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

      expect(mockSan).toHaveBeenCalledTimes(1);
      expect(mockSan).toBeCalledWith('d2', 'c3');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });
  });

  describe('from an ambiguous castling position', () => {
    const ambiguousCastlingFen = '4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1';

    test('does not castle short', () => {
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(ambiguousCastlingFen, { e1: ['c1', 'g1'] }),
          },
        },
        mockClear,
      );

      submit('o-o', { isTrusted: true });

      expect(mockClear).toHaveBeenCalledTimes(0);
    });

    test('does castle long', () => {
      const mockSan = vi.fn();
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

      expect(mockSan).toHaveBeenCalledTimes(1);
      expect(mockSan).toBeCalledWith('e1', 'c1');
      expect(mockClear).toHaveBeenCalledTimes(1);
    });
  });

  describe('from a position where a pawn can promote multiple ways', () => {
    const promotablePawnFen = 'r3k3/1P6/8/8/8/8/8/4K3 w - - 0 1';

    test('with no piece specified does not promote by advancing', () => {
      const mockPromote = vi.fn();
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

      expect(mockPromote).toHaveBeenCalledTimes(0);
      expect(mockClear).toHaveBeenCalledTimes(0);
    });

    test('with piece specified does promote by advancing', () => {
      const mockPromote = vi.fn();
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

      expect(mockPromote).toHaveBeenCalledTimes(1);
      expect(mockClear).toHaveBeenCalledTimes(1);
    });

    test('with no piece specified does not promote by capturing', () => {
      const mockPromote = vi.fn();
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

      expect(mockPromote).toHaveBeenCalledTimes(0);
      expect(mockClear).toHaveBeenCalledTimes(0);
    });

    test('with piece specified does promote by capturing', () => {
      const mockPromote = vi.fn();
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

      expect(mockPromote).toHaveBeenCalledTimes(1);
      expect(mockClear).toHaveBeenCalledTimes(1);
    });

    describe('with pawn selected', () => {
      test('with no piece specified does not promote by advancing', () => {
        const mockPromote = vi.fn();
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

        expect(mockPromote).toHaveBeenCalledTimes(0);
        expect(mockClear).toHaveBeenCalledTimes(0);
      });

      test('with piece specified does promote by advancing', () => {
        const mockPromote = vi.fn();
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

        expect(mockPromote).toHaveBeenCalledTimes(1);
        expect(mockClear).toHaveBeenCalledTimes(1);
      });

      test('with no piece specified does not promote by capturing', () => {
        const mockPromote = vi.fn();
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

        expect(mockPromote).toHaveBeenCalledTimes(0);
        expect(mockClear).toHaveBeenCalledTimes(0);
      });

      test('with piece specified does promote by capturing', () => {
        const mockPromote = vi.fn();
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

        expect(mockPromote).toHaveBeenCalledTimes(1);
        expect(mockClear).toHaveBeenCalledTimes(1);
      });
    });
  });

  describe('in crazyhouse variant', () => {
    test('with incomplete crazyhouse entry does nothing', () => {
      const mockDrop = vi.fn();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
            drop: mockDrop,
          },
        },
        mockClear,
      );

      submit('Q@a', { isTrusted: true });

      expect(mockDrop).toHaveBeenCalledTimes(0);
      expect(mockClear).toHaveBeenCalledTimes(0);
    });

    test('with complete crazyhouse entry does a drop', () => {
      const mockDrop = vi.fn();
      const submit = makeSubmit(
        {
          input: document.createElement('input'),
          ctrl: {
            ...defaultCtrl,
            legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
            drop: mockDrop,
          },
        },
        mockClear,
      );

      submit('Q@a5', { isTrusted: true });

      expect(mockDrop).toHaveBeenCalledTimes(1);
      expect(mockClear).toHaveBeenCalledTimes(1);
    });
  });

  test('with incorrect entry marks it wrong', () => {
    vi.stubGlobal('site', { sound: { play: vi.fn() } });

    const input = document.createElement('input');
    const submit = makeSubmit(
      {
        input,
        ctrl: {
          ...defaultCtrl,
          legalSans: fenDestsToSans(startingFen, { e2: ['e4'] }),
        },
      },
      defaultClear,
    );

    submit('j4', { isTrusted: true });

    expect(input.classList.contains('wrong')).toBe(true);
    expect(site.sound.play).toHaveBeenCalledTimes(1);
    expect(site.sound.play).toBeCalledWith('error');
  });
});

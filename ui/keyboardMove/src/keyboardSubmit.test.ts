import { jest, beforeEach, describe, expect, test } from '@jest/globals';
import { Prop, propWithEffect } from 'common';
import { makeSubmit } from './keyboardSubmit';
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

// we don't have access to DOM elements in jest (testEnvironment: 'node'), so we need to mock this
const input = {
  value: '',
  classList: { contains: () => false, toggle: () => {} },
} as unknown as HTMLInputElement;

describe('keyboardSubmit', () => {
  let mockClear = jest.fn();

  beforeEach(() => {
    mockClear = jest.fn();
  });

  test('resigns game', () => {
    const mockResign = jest.fn();
    const submit = makeSubmit(
      {
        input,
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
    const mockDraw = jest.fn();
    const submit = makeSubmit(
      {
        input,
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
    const mockNext = jest.fn();
    const submit = makeSubmit(
      {
        input,
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
    const mockVote = jest.fn();
    const submit = makeSubmit(
      {
        input,
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
    const mockVote = jest.fn();
    const submit = makeSubmit(
      {
        input,
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
    const mockSpeakClock = jest.fn();
    const submit = makeSubmit(
      {
        input,
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
    const mockGoBerserk = jest.fn();
    const submit = makeSubmit(
      {
        input,
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

  test('opens help modal with ?', () => {
    const mockSetHelpModalOpen = jest.fn();
    const submit = makeSubmit(
      {
        input,
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
      const mockSan = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockSelect = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockSan = jest.fn();
      const mockSelect = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockSelect = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockSan = jest.fn();
      const mockSelect = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockSan = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockSan = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
          input,
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
      const mockSan = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockPromote = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockPromote = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockPromote = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockPromote = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
        const mockPromote = jest.fn();
        const submit = makeSubmit(
          {
            input,
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
        const mockPromote = jest.fn();
        const submit = makeSubmit(
          {
            input,
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
        const mockPromote = jest.fn();
        const submit = makeSubmit(
          {
            input,
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
        const mockPromote = jest.fn();
        const submit = makeSubmit(
          {
            input,
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
      const mockDrop = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
      const mockDrop = jest.fn();
      const submit = makeSubmit(
        {
          input,
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
    input.classList.toggle = jest.fn() as any;
    site.sound = { play: jest.fn() } as any;
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

    expect(input.classList.toggle).toHaveBeenCalledTimes(1);
    expect(input.classList.toggle).toBeCalledWith('wrong', true);
    expect(site.sound.play).toHaveBeenCalledTimes(1);
  });
});

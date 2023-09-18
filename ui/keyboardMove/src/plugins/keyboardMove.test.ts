import { jest, beforeEach, describe, expect, test } from '@jest/globals';
import { Prop, propWithEffect } from 'common';
import { initModule as keyboardMove } from './keyboardMove';

// Tips for working with this file:
// - use https://lichess.org/editor to create positions and get their FENs
// - use https://lichess.org/editor/<FEN> to check what FENs look like

// Set up the globals that are normally attached to window
declare let global: any;
global.lichess = { sound: { say: () => null, play: () => null }, mousetrap: { bind: () => undefined } };

// Set up the common defaults
document.body.innerHTML = `<input id="keyboardInput" />`;
let input = document.getElementById('keyboardInput') as HTMLInputElement;
const unexpectedErrorThrower = (name: string) => () => {
  throw new Error(`unexpected call to ${name}()`);
};
const defaultCtrl = {
  clock: unexpectedErrorThrower('clock'),
  confirmMove: () => null,
  draw: unexpectedErrorThrower('draw'),
  next: unexpectedErrorThrower('next'),
  vote: unexpectedErrorThrower('vote'),
  drop: unexpectedErrorThrower('drop'),
  hasSelected: () => undefined,
  jump: () => null,
  justSelected: () => true,
  promote: unexpectedErrorThrower('promote'),
  registerHandler: () => null,
  resign: unexpectedErrorThrower('resign'),
  san: unexpectedErrorThrower('san'),
  select: unexpectedErrorThrower('select'),
  update: () => null,
  usedSan: true,
  helpModalOpen: propWithEffect(false, () => null),
  isFocused: propWithEffect(false, () => null),
};
const startingFen = 'rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1';

const toMap = (obj: object) => new Map(Object.entries(obj));

describe('keyboardMove', () => {
  beforeEach(() => {
    document.body.innerHTML = `<input id="keyboardInput" />`;
    input = document.getElementById('keyboardInput') as HTMLInputElement;
  });

  test('resigns game', () => {
    input.value = 'resign';
    const mockResign = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        resign: mockResign,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({}), true);

    expect(mockResign.mock.calls.length).toBe(1);
    expect(input.value).toBe('');
  });

  test('draws game', () => {
    input.value = 'draw';
    const mockDraw = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        draw: mockDraw,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({}), true);

    expect(mockDraw.mock.calls.length).toBe(1);
    expect(input.value).toBe('');
  });

  test('goes to next puzzle', () => {
    input.value = 'next';
    const mockNext = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        next: mockNext,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({}), true);

    expect(mockNext).toHaveBeenCalledTimes(1);
    expect(input.value).toBe('');
  });

  test('up votes puzzle', () => {
    input.value = 'upv';
    const mockVote = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        vote: mockVote,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({}), true);

    expect(mockVote).toHaveBeenCalledTimes(1);
    expect(mockVote).toBeCalledWith(true);
    expect(input.value).toBe('');
  });

  test('down votes puzzle', () => {
    input.value = 'downv';
    const mockVote = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        vote: mockVote,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({}), true);

    expect(mockVote).toHaveBeenCalledTimes(1);
    expect(mockVote).toBeCalledWith(false);
    expect(input.value).toBe('');
  });

  test('reads out clock', () => {
    input.value = 'clock';
    const mockMillisOf = jest.fn(_ => 1000);
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        clock: () => ({ millisOf: mockMillisOf }) as any,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({}), true);

    expect(mockMillisOf.mock.calls.length).toBe(2);
    expect(input.value).toBe('');
  });

  test('opens help modal with ?', () => {
    input.value = '?';
    const mockSetHelpModalOpen = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        helpModalOpen: mockSetHelpModalOpen as Prop<boolean>,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({}), true);

    expect(mockSetHelpModalOpen.mock.calls.length).toBe(1);
    expect(input.value).toBe('');
  });

  describe('from starting position', () => {
    test('plays e4 via SAN', () => {
      input.value = 'e4';
      const mockSan = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          san: mockSan,
        },
      }) as any;

      keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

      expect(mockSan.mock.calls.length).toBe(1);
      expect(mockSan.mock.calls[0][0]).toBe('e2');
      expect(mockSan.mock.calls[0][1]).toBe('e4');
      expect(input.value).toBe('');
    });

    test('selects e2 via UCI', () => {
      input.value = 'e2';
      const mockSelect = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          select: mockSelect,
        },
      }) as any;

      keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

      expect(mockSelect.mock.calls.length).toBe(1);
      expect(mockSelect.mock.calls[0][0]).toBe('e2');
      expect(input.value).toBe('');
    });

    test('with e2 selected, plays e4 via UCI', () => {
      input.value = 'e4';
      const mockSelect = jest.fn();
      const mockSan = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          hasSelected: () => 'e2',
          san: mockSan,
          select: mockSelect,
        },
      }) as any;

      keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

      expect(mockSelect.mock.calls.length).toBe(1);
      expect(mockSelect.mock.calls[0][0]).toBe('e4');
      // is it intended behavior to call both select and san?
      expect(mockSan.mock.calls.length).toBe(1);
      expect(mockSan.mock.calls[0][0]).toBe('e2');
      expect(mockSan.mock.calls[0][1]).toBe('e4');
      expect(input.value).toBe('');
    });

    test('selects e2 via ICCF', () => {
      input.value = '52';
      const mockSelect = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          select: mockSelect,
        },
      }) as any;

      keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

      expect(mockSelect.mock.calls.length).toBe(1);
      expect(mockSelect.mock.calls[0][0]).toBe('e2');
      expect(input.value).toBe('');
    });

    test('with e2 selected, plays e4 via ICCF', () => {
      input.value = '54';
      const mockSelect = jest.fn();
      const mockSan = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          hasSelected: () => 'e2',
          san: mockSan,
          select: mockSelect,
        },
      }) as any;

      keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

      expect(mockSelect.mock.calls.length).toBe(1);
      expect(mockSelect.mock.calls[0][0]).toBe('e4');
      // is it intended behavior to call both select and san?
      expect(mockSan.mock.calls.length).toBe(1);
      expect(mockSan.mock.calls[0][0]).toBe('e2');
      expect(mockSan.mock.calls[0][1]).toBe('e4');
      expect(input.value).toBe('');
    });
  });

  test('when it is newly your move clears invalid input', () => {
    input.value = 'nf6';
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: defaultCtrl,
    }) as any;

    keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

    expect(input.value).toBe('');
  });

  describe('from a position where a b-file pawn or bishop can capture', () => {
    const ambiguousPawnBishopCapture = '4k3/8/8/8/8/2r5/1P1B4/4K3 w - - 0 1';
    test('does pawn capture', () => {
      input.value = 'bc3';
      const mockSan = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          san: mockSan,
        },
      }) as any;

      keyboardMovePlugin(ambiguousPawnBishopCapture, toMap({ b2: ['c3'], d2: ['c3'] }), true);

      expect(mockSan.mock.calls.length).toBe(1);
      expect(mockSan.mock.calls[0][0]).toBe('b2');
      expect(mockSan.mock.calls[0][1]).toBe('c3');
      expect(input.value).toBe('');
    });
    test('does bishop capture', () => {
      input.value = 'Bc3';
      const mockSan = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          san: mockSan,
        },
      }) as any;

      keyboardMovePlugin(ambiguousPawnBishopCapture, toMap({ b2: ['c3'], d2: ['c3'] }), true);

      expect(mockSan.mock.calls.length).toBe(1);
      expect(mockSan.mock.calls[0][0]).toBe('d2');
      expect(mockSan.mock.calls[0][1]).toBe('c3');
      expect(input.value).toBe('');
    });
  });

  describe('from an ambiguous castling position', () => {
    const ambiguousCastlingFen = '4k3/8/8/8/8/8/8/R3K2R w KQ - 0 1';
    test('does not castle short', () => {
      input.value = 'o-o';
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: defaultCtrl,
      }) as any;

      keyboardMovePlugin(ambiguousCastlingFen, toMap({ e1: ['c1', 'g1'] }), true);

      expect(input.value).toBe('o-o');
    });

    test('does castle long', () => {
      input.value = 'o-o-o';
      const mockSan = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          san: mockSan,
        },
      }) as any;

      keyboardMovePlugin(ambiguousCastlingFen, toMap({ e1: ['c1', 'g1'] }), true);

      expect(mockSan.mock.calls.length).toBe(1);
      expect(mockSan.mock.calls[0][0]).toBe('e1');
      expect(mockSan.mock.calls[0][1]).toBe('c1');
      expect(input.value).toBe('');
    });
  });

  describe('from a position where a pawn can promote multiple ways', () => {
    const promotablePawnFen = 'r3k3/1P6/8/8/8/8/8/4K3 w - - 0 1';
    test('with no piece specified does not promote by advancing', () => {
      input.value = 'b8';
      const mockPromote = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          promote: mockPromote,
        },
      }) as any;

      keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

      expect(mockPromote.mock.calls.length).toBe(0);
      expect(input.value).toBe('b8');
    });

    test('with piece specified does promote by advancing', () => {
      input.value = 'b8=q';
      const mockPromote = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          promote: mockPromote,
        },
      }) as any;

      keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

      expect(mockPromote.mock.calls.length).toBe(1);
      expect(input.value).toBe('');
    });

    test('with no piece specified does not promote by capturing', () => {
      input.value = 'ba8';
      const mockPromote = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          promote: mockPromote,
        },
      }) as any;

      keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

      expect(mockPromote.mock.calls.length).toBe(0);
      expect(input.value).toBe('ba8');
    });

    test('with piece specified does promote by capturing', () => {
      input.value = 'ba8=b';
      const mockPromote = jest.fn();
      const keyboardMovePlugin = keyboardMove({
        input,
        ctrl: {
          ...defaultCtrl,
          promote: mockPromote,
        },
      }) as any;

      keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

      expect(mockPromote.mock.calls.length).toBe(1);
      expect(input.value).toBe('');
    });

    describe('with pawn selected', () => {
      test('with no piece specified does not promote by advancing', () => {
        input.value = 'b8';
        const mockPromote = jest.fn();
        const keyboardMovePlugin = keyboardMove({
          input,
          ctrl: {
            ...defaultCtrl,
            hasSelected: () => 'b7',
            promote: mockPromote,
          },
        }) as any;

        keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

        expect(mockPromote.mock.calls.length).toBe(0);
        expect(input.value).toBe('b8');
      });

      test('with piece specified does promote by advancing', () => {
        input.value = 'b8=r';
        const mockPromote = jest.fn();
        const keyboardMovePlugin = keyboardMove({
          input,
          ctrl: {
            ...defaultCtrl,
            hasSelected: () => 'b7',
            promote: mockPromote,
          },
        }) as any;

        keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

        expect(mockPromote.mock.calls.length).toBe(1);
        expect(input.value).toBe('');
      });

      test('with no piece specified does not promote by capturing', () => {
        input.value = 'ba8';
        const mockPromote = jest.fn();
        const keyboardMovePlugin = keyboardMove({
          input,
          ctrl: {
            ...defaultCtrl,
            hasSelected: () => 'b7',
            promote: mockPromote,
          },
        }) as any;

        keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

        expect(mockPromote.mock.calls.length).toBe(0);
        expect(input.value).toBe('ba8');
      });

      test('with piece specified does promote by capturing', () => {
        input.value = 'a8=n';
        const mockPromote = jest.fn();
        const keyboardMovePlugin = keyboardMove({
          input,
          ctrl: {
            ...defaultCtrl,
            hasSelected: () => 'b7',
            promote: mockPromote,
          },
        }) as any;

        keyboardMovePlugin(promotablePawnFen, toMap({ b7: ['a8', 'b8'] }), true);

        expect(mockPromote.mock.calls.length).toBe(1);
        expect(input.value).toBe('');
      });
    });
  });

  test('with incomplete crazyhouse entry does nothing', () => {
    input.value = 'Q@a';
    const mockDrop = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        drop: mockDrop,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

    expect(mockDrop.mock.calls.length).toBe(0);
    expect(input.value).toBe('Q@a');
  });

  test('with complete crazyhouse entry does a drop', () => {
    input.value = 'Q@a5';
    const mockDrop = jest.fn();
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: {
        ...defaultCtrl,
        drop: mockDrop,
      },
    }) as any;

    keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), true);

    expect(mockDrop.mock.calls.length).toBe(1);
    expect(input.value).toBe('');
  });

  test('with incorrect entry marks it wrong', () => {
    input.value = 'j4';
    const keyboardMovePlugin = keyboardMove({
      input,
      ctrl: defaultCtrl,
    }) as any;

    keyboardMovePlugin(startingFen, toMap({ e2: ['e4'] }), false);

    expect(input.value).toBe('j4');
    expect(input.classList.contains('wrong')).toBeTruthy();
  });
});

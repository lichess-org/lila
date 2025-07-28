// Simple test for Safari drag hover functionality
// This test verifies the core logic without requiring full build

import { setupSafariDragHover } from '../src/safariDragHover';

// Mock objects for testing
const mockSquare = {
  classList: {
    contains: jest.fn().mockReturnValue(true), // assume move-dest
    add: jest.fn(),
    remove: jest.fn(),
  },
  cgKey: 'e4',
};

const mockBoard = {
  querySelectorAll: jest.fn().mockReturnValue([mockSquare]),
  getBoundingClientRect: jest.fn().mockReturnValue({
    left: 0,
    top: 0,
    width: 400,
    height: 400,
  }),
  addEventListener: jest.fn(),
  removeEventListener: jest.fn(),
};

const mockChessground = {
  state: {
    dom: {
      elements: {
        board: mockBoard,
      },
    },
  },
  getKeyAtDomPos: jest.fn().mockReturnValue('e4'),
};

// Mock Safari user agent
Object.defineProperty(navigator, 'userAgent', {
  writable: true,
  value: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Safari/537.36',
});

describe('Safari Drag Hover', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  test('should setup event listeners on Safari', () => {
    setupSafariDragHover(mockChessground as any);
    
    expect(mockBoard.addEventListener).toHaveBeenCalledWith('mousemove', expect.any(Function));
  });

  test('should not setup on non-Safari browsers', () => {
    Object.defineProperty(navigator, 'userAgent', {
      writable: true, 
      value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124',
    });

    setupSafariDragHover(mockChessground as any);
    
    expect(mockBoard.addEventListener).not.toHaveBeenCalled();
  });
});
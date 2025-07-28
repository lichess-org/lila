// Simple test for Safari drag hover functionality
// This test verifies the core logic without requiring full build

import { describe, test, expect, beforeEach, vi } from 'vitest';

// Mock objects for testing
const mockSquare = {
  classList: {
    contains: vi.fn().mockReturnValue(true), // assume move-dest
    add: vi.fn(),
    remove: vi.fn(),
  },
  cgKey: 'e4',
};

const mockBoard = {
  querySelectorAll: vi.fn().mockReturnValue([mockSquare]),
  getBoundingClientRect: vi.fn().mockReturnValue({
    left: 0,
    top: 0,
    width: 400,
    height: 400,
  }),
  addEventListener: vi.fn(),
  removeEventListener: vi.fn(),
  nodeType: undefined, // This will make the Node check fail, avoiding MutationObserver issues
};

const mockChessground = {
  state: {
    dom: {
      elements: {
        board: mockBoard,
      },
    },
  },
  getKeyAtDomPos: vi.fn().mockReturnValue('e4'),
};

describe('Safari Drag Hover', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  test('should setup event listeners on Safari', async () => {
    // Mock Safari user agent
    Object.defineProperty(navigator, 'userAgent', {
      writable: true,
      value: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Safari/537.36',
    });

    const { setupSafariDragHover } = await import('../src/safariDragHover');
    
    setupSafariDragHover(mockChessground as any);
    
    expect(mockBoard.addEventListener).toHaveBeenCalledWith('mousemove', expect.any(Function));
  });

  test('should not setup on non-Safari browsers', async () => {
    Object.defineProperty(navigator, 'userAgent', {
      writable: true, 
      value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124',
    });

    const { setupSafariDragHover } = await import('../src/safariDragHover');

    setupSafariDragHover(mockChessground as any);
    
    expect(mockBoard.addEventListener).not.toHaveBeenCalled();
  });
});
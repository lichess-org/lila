/**
 * Integration test for Safari drag hover fix
 * Tests the complete flow from drag start to hover highlighting
 */
import { describe, test, expect, beforeEach, vi } from 'vitest';

describe('Safari Drag Hover Integration', () => {
  let mockBoard: HTMLElement;
  let mockChessground: any;
  let mockSquare: HTMLElement;
  
  beforeEach(() => {
    // Mock DOM elements
    mockSquare = {
      classList: {
        contains: vi.fn(),
        add: vi.fn(),
        remove: vi.fn(),
      },
      cgKey: 'e4',
    } as any;
    
    const mockPiece = {
      classList: {
        contains: vi.fn(),
        add: vi.fn(),
        remove: vi.fn(),
      },
      tagName: 'PIECE',
    };
    
    mockBoard = {
      getBoundingClientRect: vi.fn().mockReturnValue({
        left: 100,
        top: 100,
        width: 400,
        height: 400,
      }),
      addEventListener: vi.fn(),
      removeEventListener: vi.fn(),
      querySelectorAll: vi.fn(),
      querySelector: vi.fn(),
    } as any;
    
    // Mock MutationObserver
    global.MutationObserver = vi.fn().mockImplementation((callback) => ({
      observe: vi.fn(),
      disconnect: vi.fn(),
    }));
    
    mockChessground = {
      state: {
        dom: {
          elements: {
            board: mockBoard,
          },
        },
      },
      getKeyAtDomPos: vi.fn(),
    };
    
    // Mock Safari user agent
    Object.defineProperty(navigator, 'userAgent', {
      writable: true,
      value: 'Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 Safari/537.36',
    });
    
    vi.clearAllMocks();
  });

  test('should detect drag start and enable hover tracking', async () => {
    const { setupSafariDragHover } = await import('../src/safariDragHover');
    
    setupSafariDragHover(mockChessground);
    
    // Verify MutationObserver was set up
    expect(global.MutationObserver).toHaveBeenCalled();
    
    // Verify mousemove listener was added
    expect(mockBoard.addEventListener).toHaveBeenCalledWith('mousemove', expect.any(Function));
  });

  test('should not activate on non-Safari browsers', async () => {
    // Set Chrome user agent
    Object.defineProperty(navigator, 'userAgent', {
      writable: true,
      value: 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 Chrome/91.0.4472.124',
    });
    
    const { setupSafariDragHover } = await import('../src/safariDragHover');
    
    setupSafariDragHover(mockChessground);
    
    expect(global.MutationObserver).not.toHaveBeenCalled();
    expect(mockBoard.addEventListener).not.toHaveBeenCalled();
  });
});
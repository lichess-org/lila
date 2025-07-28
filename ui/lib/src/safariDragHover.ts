// Safari-specific fix for hover highlighting during drag operations
// Safari suppresses :hover styles during pointer capture, so we need to manually manage hover classes

const isSafari = () => /^((?!chrome|android).)*safari/i.test(navigator.userAgent);

interface HoverState {
  currentHoveredSquare?: string;
  isActive: boolean;
}

export function setupSafariDragHover(chessground: CgApi): void {
  if (!isSafari()) return;

  const hoverState: HoverState = { isActive: false };
  
  const addHoverClass = (square: HTMLElement) => {
    square.classList.add('safari-drag-hover');
  };
  
  const removeHoverClass = (square: HTMLElement) => {
    square.classList.remove('safari-drag-hover');
  };
  
  const clearAllHovers = () => {
    const hovered = chessground.state.dom.elements.board.querySelectorAll('.safari-drag-hover');
    hovered.forEach(square => removeHoverClass(square as HTMLElement));
    hoverState.currentHoveredSquare = undefined;
  };
  
  const updateHover = (e: MouseEvent) => {
    if (!hoverState.isActive) return;
    
    const rect = chessground.state.dom.elements.board.getBoundingClientRect();
    const pos = [e.clientX - rect.left, e.clientY - rect.top] as const;
    const key = chessground.getKeyAtDomPos(pos);
    
    if (key && key !== hoverState.currentHoveredSquare) {
      clearAllHovers();
      
      // Find the square element with matching cgKey
      const squares = chessground.state.dom.elements.board.querySelectorAll('square');
      for (const square of squares) {
        if ((square as any).cgKey === key && square.classList.contains('move-dest')) {
          addHoverClass(square as HTMLElement);
          hoverState.currentHoveredSquare = key;
          break;
        }
      }
    } else if (!key && hoverState.currentHoveredSquare) {
      clearAllHovers();
    }
  };
  
  const startDragHover = () => {
    hoverState.isActive = true;
  };
  
  const endDragHover = () => {
    hoverState.isActive = false;
    clearAllHovers();
  };
  
  // Listen for drag start/end through DOM mutations or piece state changes
  const observer = new MutationObserver((mutations) => {
    mutations.forEach((mutation) => {
      if (mutation.type === 'attributes' && mutation.attributeName === 'class') {
        const target = mutation.target as HTMLElement;
        if (target.tagName === 'PIECE') {
          if (target.classList.contains('dragging')) {
            startDragHover();
          } else if (hoverState.isActive && !target.classList.contains('dragging')) {
            // Check if any pieces are still dragging
            const stillDragging = chessground.state.dom.elements.board.querySelector('piece.dragging');
            if (!stillDragging) {
              endDragHover();
            }
          }
        }
      }
    });
  });
  
  observer.observe(chessground.state.dom.elements.board, {
    attributes: true,
    attributeFilter: ['class'],
    subtree: true
  });
  
  chessground.state.dom.elements.board.addEventListener('mousemove', updateHover);
  
  // Cleanup function (though chessground doesn't currently expose a way to call this)
  return () => {
    observer.disconnect();
    chessground.state.dom.elements.board.removeEventListener('mousemove', updateHover);
    clearAllHovers();
  };
}
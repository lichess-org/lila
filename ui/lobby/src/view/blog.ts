let blogRotateTimer: number | undefined = undefined;

export function rotateBlogs() {
  document.querySelectorAll<HTMLElement>('.lobby__blog').forEach(el => {
    const style = window.getComputedStyle(el);
    const gridGap = parseFloat(style.columnGap) * (style.columnGap.endsWith('%') ? el.clientWidth / 100 : 1);
    const kids = Array.from(el.children) as HTMLElement[]; // Explicitly handle each card as an HTMLElement
    const visible = Math.floor((el.clientWidth + gridGap) / (192 + gridGap));
    const colW = (el.clientWidth - gridGap * (visible - 1)) / visible;
    let startX = 0;
    let isDragging = false;
    let currentCard: HTMLElement | null = null;

    el.style.gridTemplateColumns = `repeat(7, ${colW}px)`;

    const rotateBlogInner = () => {
      kids.forEach(kid => {
        kid.style.transition = 'transform 0.6s ease';
        kid.style.transform = `translateX(-${Math.floor(colW + gridGap)}px)`;
      });

      setTimeout(() => {
        el.append(el.firstChild!);
        fix();
      }, 600); // Ensure this matches transition duration
    };

    const fix = () => {
      kids.forEach(kid => {
        kid.style.transition = '';
        kid.style.transform = 'translateX(0)';
      });
    };

    // Prevent native drag behavior
    kids.forEach(kid => {
      kid.setAttribute('draggable', 'false');
    });

    el.addEventListener('mousedown', event => {
      event.preventDefault(); // Prevent default behavior to avoid red slash cursor
      isDragging = true;
      startX = event.clientX;

      // Select the whole card by finding the closest .lobby__blog child
      currentCard = (event.target as HTMLElement).closest('.lobby__blog > *') as HTMLElement;

      if (currentCard) {
        currentCard.style.transition = 'transform 0.1s ease'; // Smooth follow effect
        clearInterval(blogRotateTimer); // Pause auto-rotation while dragging
      }
    });

    el.addEventListener('mousemove', event => {
      if (isDragging && currentCard) {
        const deltaX = event.clientX - startX;
        currentCard.style.transform = `translateX(${deltaX}px)`; // Move the whole card with cursor

        if (Math.abs(deltaX) > colW / 2) {
          // Threshold for rotation
          if (deltaX > 0) {
            // Rotate right
            el.prepend(el.lastChild!);
          } else {
            // Rotate left
            el.append(el.firstChild!);
          }

          fix(); // Reset positions after rotation
          isDragging = false; // End the drag after rotation
          currentCard = null; // Clear current card
        }
      }
    });

    el.addEventListener('mouseup', () => {
      if (currentCard) {
        currentCard.style.transform = 'translateX(0)'; // Snap back to original position
        currentCard = null;
      }
      isDragging = false;
      blogRotateTimer = setInterval(rotateBlogInner, 10000); // Resume auto-rotation
    });

    el.addEventListener('mouseleave', () => {
      if (currentCard) {
        currentCard.style.transform = 'translateX(0)'; // Snap back if mouse leaves
        currentCard = null;
      }
      isDragging = false;
    });

    fix();
    clearInterval(blogRotateTimer);
    blogRotateTimer = setInterval(rotateBlogInner, 10000);
  });
}

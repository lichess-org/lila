let blogRotateTimer: number | undefined = undefined;

export function rotateBlogs() {
  document.querySelectorAll<HTMLElement>('.lobby__blog').forEach(el => {
    const style = window.getComputedStyle(el);
    const gridGap = parseFloat(style.columnGap) * (style.columnGap.endsWith('%') ? el.clientWidth / 100 : 1);
    const kids = Array.from(el.children) as HTMLElement[];
    const visible = Math.floor((el.clientWidth + gridGap) / (192 + gridGap));
    const colW = (el.clientWidth - gridGap * (visible - 1)) / visible;
    let startX = 0;
    let isDragging = false;
    let isMouseDown = false;
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
      }, 500); // Ensure this matches transition duration
    };

    const fix = () => {
      kids.forEach(kid => {
        kid.style.transition = '';
        kid.style.transform = 'translateX(0)';
      });
    };

    fix();
    clearInterval(blogRotateTimer);
    blogRotateTimer = setInterval(rotateBlogInner, 10000);

    const shiftCards = (numTimes: number) => {
      if (numTimes <= 0) {
        return;
      }
      el.append(el.firstChild!);
      fix();
      shiftCards(numTimes - 1);
    };

    const stopDragging = (xPosition: number) => {
      if (currentCard) {
        // Smoothly snap back all cards
        kids.forEach(kid => {
          kid.style.transition = 'transform 0.3s ease-out'; // Smooth transition back
          kid.style.transform = 'translateX(0)';
        });
      }
      if (isDragging && currentCard) {
        const factor = (startX - xPosition) / (colW + gridGap);
        shiftCards(Math.max(0, Math.min(3, Math.ceil(factor - 0.7))));
      }
      isDragging = false;
      currentCard = null;
      isMouseDown = false;
      clearInterval(blogRotateTimer);
      blogRotateTimer = setInterval(rotateBlogInner, 10000); // Resume auto-rotation
    };

    el.addEventListener('mousedown', event => {
      event.preventDefault(); // Prevent default behavior to avoid red slash cursor
      startX = event.clientX;
      isMouseDown = true;
      currentCard = (event.target as HTMLElement).closest('.lobby__blog > *') as HTMLElement;
      if (currentCard) {
        clearInterval(blogRotateTimer); // Pause auto-rotation while dragging
      }
    });

    el.addEventListener('mousemove', event => {
      if (isMouseDown && currentCard) {
        isDragging = true;
        kids.forEach(kid => {
          kid.style.transition = 'transform 0.1s ease';
          kid.style.transform = `translateX(${event.clientX - startX}px)`;
        });
      }
    });

    el.addEventListener('click', event => {
      if (isDragging) {
        event.preventDefault();
      }
      stopDragging(event.clientX);
    });

    el.addEventListener('mouseleave', event => {
      stopDragging(event.clientX);
    });
  });
}

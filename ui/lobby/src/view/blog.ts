let blogRotateTimer: number | undefined = undefined;

export function rotateBlogs() {
  document.querySelectorAll<HTMLElement>('.lobby__blog').forEach(el => {
    const style = window.getComputedStyle(el);
    const gridGap = parseFloat(style.columnGap) * (style.columnGap.endsWith('%') ? el.clientWidth / 100 : 1);
    const kids = el.children as HTMLCollectionOf<HTMLElement>;
    const visible = Math.floor((el.clientWidth + gridGap) / (192 + gridGap));
    const colW = (el.clientWidth - gridGap * (visible - 1)) / visible;
    let startX = 0;
    let isDragging = false;
    let currentCard: HTMLElement | null = null;
    el.style.gridTemplateColumns = `repeat(7, ${colW}px)`;

    const translate_cards = (translate_amt: number, transition: string = '') => {
      for (let i = 0; i < kids.length; i++) {
        kids[i].style.transition = transition;
        kids[i].style.transform = `translateX(${translate_amt}px)`;
      }
    };

    const fix = () => {
      translate_cards(0);
    };

    const rotateBlogInner = () => {
      translate_cards(-Math.floor(colW + gridGap), 'transform 0.6s ease');
      setTimeout(() => {
        el.append(el.firstChild!);
        fix();
      }, 500);
    };

    fix();
    clearInterval(blogRotateTimer);
    blogRotateTimer = setInterval(rotateBlogInner, 10000);

    const stopDragging = (xPosition: number) => {
      if (isDragging && currentCard) {
        const factor = (startX - xPosition) / (colW + gridGap);
        const shiftCount = Math.max(0, Math.min(3, Math.ceil(factor - 0.6)));
        const transition_amount_ms = 400;
        translate_cards(-shiftCount * (colW + gridGap), `transform ${transition_amount_ms / 1000}s ease`);
        setTimeout(() => {
          for (let i = 0; i < shiftCount; i++) {
            el.append(el.firstChild!);
          }
          fix();
        }, transition_amount_ms);
      }
      isDragging = false;
      currentCard = null;
      clearInterval(blogRotateTimer);
      blogRotateTimer = setInterval(rotateBlogInner, 10000); // Resume auto-rotation
    };

    el.addEventListener('mousedown', event => {
      event.preventDefault();
      startX = event.clientX;
      currentCard = (event.target as HTMLElement).closest('.lobby__blog > *') as HTMLElement;
      if (currentCard) {
        clearInterval(blogRotateTimer); // Pause auto-rotation while dragging
      }
    });

    el.addEventListener('mousemove', event => {
      if (currentCard) {
        isDragging = true;
        translate_cards(Math.min(0, event.clientX - startX));
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

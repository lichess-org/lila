
let blogRotateTimer: number | undefined = undefined;

export function rotateBlogs() {
  document.querySelectorAll<HTMLElement>('.lobby__blog').forEach(el => {
    const style = window.getComputedStyle(el);
    const gridGap = parseFloat(style.columnGap) * (style.columnGap.endsWith('%') ? el.clientWidth / 100 : 1);
    const kids = el.children as HTMLCollectionOf<HTMLElement>;
    const visible = Math.floor((el.clientWidth + gridGap) / (192 + gridGap));
    const colW = (el.clientWidth - gridGap * (visible - 1)) / visible;
    el.style.gridTemplateColumns = `repeat(7, ${colW}px)`;
    const rotateBlogInner = () => {
      for (let i = 0; i < kids.length; i++) {
        kids[i].style.transition = 'transform 0.6s ease';
        kids[i].style.transform = `translateX(-${Math.floor(colW + gridGap)}px)`;
      }
      setTimeout(() => {
        el.append(el.firstChild!);
        fix();
      }, 500);
    };

    const fix = () => {
      for (let i = 0; i < kids.length; i++) {
        const kid = kids[i % kids.length];
        kid.style.transition = '';
        kid.style.transform = 'translateX(0)';
      }
    };
    fix();
    clearInterval(blogRotateTimer);
    blogRotateTimer = setInterval(rotateBlogInner, 10000);
  });
}

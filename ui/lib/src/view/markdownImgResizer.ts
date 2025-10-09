import { frag } from '@/common';
import type { Prop } from '@/common';
import { clamp } from '@/algo';

export type UpdateImageHook =
  | { markdown: Prop<string> }
  | { url: (img: HTMLElement, newUrl: string, widthRatio: number) => void };

export function wrapImg(arg: { img: HTMLImageElement } | { src: string; alt: string }): HTMLElement {
  const img = 'img' in arg ? arg.img : frag<HTMLImageElement>(`<img src="${arg.src}" alt="${arg.alt}">`);
  const span = frag<HTMLElement>($html`
    <span class="markdown-img-container">
      <span>
        <i class="resize-handle right"></i>
        <i class="resize-handle bottom"></i>
        <i class="resize-handle left"></i>
      </span>
    </span>`);
  span.querySelector('span')?.prepend(img);
  return span;
}

export async function wireMarkdownImgResizers(init: {
  root: HTMLElement;
  updateImage: UpdateImageHook;
  isToastUi?: boolean;
}): Promise<void> {
  const { root, updateImage } = init;

  let rootStyle: CSSStyleDeclaration;
  let rootPadding: number;

  for (const [index, img] of root.querySelectorAll<HTMLImageElement>('img').entries()) {
    if (img.closest('.markdown-img-resizer')) continue;

    await img.decode().catch(() => {});

    rootStyle ??= window.getComputedStyle(root);
    rootPadding ??= parseInt(rootStyle.paddingLeft) + parseInt(rootStyle.paddingRight);

    const pointerdown = async (down: PointerEvent) => {
      const handle = down.currentTarget as HTMLElement;
      const rootWidth = root.clientWidth - rootPadding;
      const imgClientRect = img.getBoundingClientRect();
      const aspectRatio = img.naturalHeight ? img.naturalWidth / img.naturalHeight : 1;
      const oldImgWidth = img.dataset.widthRatio
        ? rootWidth * Number(img.dataset.widthRatio)
        : imgClientRect.width;
      const isBottomDrag = handle.className.includes('bottom');
      const isCornerDrag = !isBottomDrag && imgClientRect.bottom - down.clientY < 18;
      const dir = handle.className.includes('left') ? -1 : 1;
      if (isCornerDrag) handle.style.cursor = dir === 1 ? 'nwse-resize' : 'nesw-resize';

      handle.setPointerCapture?.(down.pointerId);
      img.style.willChange = 'width,height';
      img.style.width = `${oldImgWidth}px`;
      img.closest<HTMLElement>('.markdown-img-resizer')!.style.width = '';

      const pointermove = (move: PointerEvent) => {
        const deltaX = isCornerDrag
          ? dir * (move.clientX - down.clientX) + (aspectRatio * (move.clientY - down.clientY)) / 2
          : isBottomDrag
            ? (move.clientY - down.clientY) * aspectRatio
            : dir * 2 * (move.clientX - down.clientX);
        const newImgWidth = clamp(oldImgWidth + deltaX, { min: 128, max: rootWidth });
        img.style.width = `${newImgWidth}px`;
        img.dataset.widthRatio = String(newImgWidth / rootWidth);
      };
      const pointerup = async () => {
        handle.removeEventListener('pointermove', pointermove);
        handle.removeEventListener('pointerup', pointerup);
        handle.removeEventListener('pointercancel', pointerup);
        if (handle.hasPointerCapture(down.pointerId)) handle.releasePointerCapture(down.pointerId);
        img.style.willChange = '';
        handle.style.cursor = '';
        if ('url' in updateImage) {
          // toast
          const imageId = img.src.match(imageIdRe)?.[1];
          const rsp = await fetch(`/image-url/${imageId}?widthRatio=${img.dataset.widthRatio}`);
          const { imageUrl } = await rsp.json();
          const preloadImg = new Image();
          preloadImg.src = imageUrl;
          await preloadImg.decode();
          img.style.width = '';
          updateImage.url(img, imageUrl, Number(img.dataset.widthRatio)!);
          return;
        }
        const text = updateImage.markdown(); // textarea
        const paths = [...text.matchAll(globalImageIdRe)];
        const match = paths[index];
        if (!img.dataset.widthRatio || !match[1]) return;
        const rsp = await fetch(`/image-url/${match[1]}?widthRatio=${img.dataset.widthRatio}`);
        const before = text.slice(0, match.index);
        const after = text.slice(match.index! + match[0].length);
        updateImage.markdown(before + (await rsp.json()).imageUrl + after);
      };
      handle.addEventListener('pointermove', pointermove, { passive: true });
      handle.addEventListener('pointerup', pointerup, { passive: true });
      handle.addEventListener('pointercancel', pointerup, { passive: true });
      down.preventDefault();
    };
    for (const h of dragHandles(img)) {
      h.addEventListener('pointerdown', pointerdown, { passive: false });
    }
  }
}

const imageIdRe = /&path=([a-z]\w+:[a-z0-9]{12}:[a-z0-9]{8}\.\w{3,4})&/i;
const globalImageIdRe = /https:[^)\s]+&path=([a-z]\w+:[a-z0-9]{12}:[a-z0-9]{8}\.\w{3,4})&[^)]+/gi;

function dragHandles(img: HTMLImageElement): HTMLElement[] {
  const span = img.closest('.markdown-img-container') ?? wrapImg({ img });
  span.firstElementChild!.classList.add('markdown-img-resizer');
  return [...span.querySelectorAll<HTMLElement>('.resize-handle')];
}

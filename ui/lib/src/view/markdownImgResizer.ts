import { frag, type Prop } from '@/common';
import { clamp } from '@/algo';
import { json as xhrJson } from '@/xhr';

// resize images in toast WYSIWYGs and raw textarea previews.
// toast: UpdateImageHook.url - img src is updated in-place via ProseMirror hooks (bits/src/toastEditor.ts)
// textarea: UpdateImageHook.markdown - the whole markdown text is updated (bits/src/bits.markdownTextarea.ts)

export type UpdateImageHook =
  | { markdown: Prop<string> }
  | { url: (img: HTMLElement, newUrl: string, width: number) => void };

type ResizerOptions = {
  root: HTMLElement;
  update: UpdateImageHook;
  resizePath: string;
  designWidth?: number;
};

export async function wireMarkdownImgResizers({
  root,
  update,
  resizePath,
  designWidth,
}: ResizerOptions): Promise<void> {
  let rootStyle: CSSStyleDeclaration;
  let rootPadding: number;

  for (const [index, img] of root.querySelectorAll<HTMLImageElement>('img').entries()) {
    if (img.closest('.markdown-img-resizer')) continue; // already wrapped
    await img.decode().catch(() => {});
    rootStyle ??= window.getComputedStyle(root);
    rootPadding ??= parseInt(rootStyle.paddingLeft) + parseInt(rootStyle.paddingRight);
    if (!isFinite(rootPadding)) rootPadding = 0;

    const pointerdown = async (down: PointerEvent) => {
      const handle = down.currentTarget as HTMLElement;
      const rootWidth = root.clientWidth - rootPadding;
      const imgClientRect = img.getBoundingClientRect();
      const aspectRatio = img.naturalHeight ? img.naturalWidth / img.naturalHeight : 1;
      // here, extrinsic is pixels in the current viewport (whereas intrinsic is natural/picfit size)
      const oldExtrinsicImgWidth = imgClientRect.width;
      const isBottomDrag = handle.className.includes('bottom');
      const isCornerDrag = !isBottomDrag && imgClientRect.bottom - down.clientY < 18;
      const dir = handle.className.includes('left') ? -1 : 1;
      if (isCornerDrag) handle.style.cursor = dir === 1 ? 'nwse-resize' : 'nesw-resize';

      handle.setPointerCapture?.(down.pointerId);
      img.style.willChange = 'width,height';
      img.style.width = `${oldExtrinsicImgWidth}px`;
      img.closest<HTMLElement>('.markdown-img-resizer')!.style.width = '';

      const pointermove = (move: PointerEvent) => {
        const deltaX = isCornerDrag
          ? dir * (move.clientX - down.clientX) + (aspectRatio * (move.clientY - down.clientY)) / 2
          : isBottomDrag
            ? (move.clientY - down.clientY) * aspectRatio
            : dir * 2 * (move.clientX - down.clientX);
        const newExtrinsicImgWidth = Math.round(
          clamp(oldExtrinsicImgWidth + deltaX, { min: 128, max: rootWidth }),
        );
        img.style.width = `${newExtrinsicImgWidth}px`;
        img.dataset.resizeWidth = String(
          designWidth ? Math.round((newExtrinsicImgWidth * designWidth) / rootWidth) : newExtrinsicImgWidth,
        );
        img.dataset.widthRatio = String(newExtrinsicImgWidth / rootWidth);
      };
      const pointerup = async () => {
        handle.removeEventListener('pointermove', pointermove);
        handle.removeEventListener('pointerup', pointerup);
        handle.removeEventListener('pointercancel', pointerup);
        if (handle.hasPointerCapture(down.pointerId)) handle.releasePointerCapture(down.pointerId);
        img.style.willChange = '';
        handle.style.cursor = '';
        if ('url' in update) {
          const imageId = img.src.match(imageIdRe)?.[1];
          const { imageUrl } = await xhrJson(`${resizePath}/${imageId}?width=${img.dataset.resizeWidth}`);
          const preloadImg = new Image();
          preloadImg.src = imageUrl;
          await preloadImg.decode();
          update.url(img, imageUrl, Number(img.dataset.widthRatio)!);
          return;
        }
        const text = update.markdown();
        const path = [...text.matchAll(globalImageLinkRe)][index];
        if (!img.dataset.widthRatio || !path[1]) return;
        const { imageUrl } = await xhrJson(`${resizePath}/${path[2]}?width=${img.dataset.resizeWidth}`);
        const before = text.slice(0, path.index);
        const after = text.slice(path.index! + path[0].length);
        update.markdown(before + `![${path[1]}](${imageUrl})` + after);
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

export function wrapImg(arg: { img: HTMLImageElement } | { src: string; alt: string }): HTMLElement {
  const span = frag<HTMLElement>($html`
    <span class="markdown-img-container">
      <span>
        <i class="resize-handle right"></i>
        <i class="resize-handle bottom"></i>
        <i class="resize-handle left"></i>
      </span>
    </span>`);
  const img = 'img' in arg ? arg.img : frag<HTMLImageElement>(`<img src="${arg.src}" alt="${arg.alt}">`);
  if ('img' in arg) img.replaceWith(span);
  span.querySelector('span')?.prepend(img);
  return span;
}

export async function naturalSize(image: Blob): Promise<{ width: number; height: number }> {
  if ('createImageBitmap' in window) return window.createImageBitmap(image);
  const objectUrl = URL.createObjectURL(image);
  const img = new Image();
  try {
    img.src = objectUrl;
    await img.decode();
    return { width: img.naturalWidth, height: img.naturalHeight };
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

const imageIdRe = /&path=([a-z]\w+:[a-z0-9]{12}:[a-z0-9]{8}\.\w{3,4})&/i;
const globalImageLinkRe =
  /!\[([^\n]*)\]\(https:[^)\s]+&path=([a-z]\w+:[a-z0-9]{12}:[a-z0-9]{8}\.\w{3,4})&[^)]+\)/gi;

function dragHandles(img: HTMLImageElement): HTMLElement[] {
  const span = img.closest('.markdown-img-container') ?? wrapImg({ img });
  span.firstElementChild!.classList.add('markdown-img-resizer');
  return [...span.querySelectorAll<HTMLElement>('.resize-handle')];
}

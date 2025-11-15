import { type Prop, frag } from '@/index';
import { clamp } from '@/algo';
import { json as xhrJson } from '@/xhr';

export type UpdateImageHook =
  | { markdown: Prop<string> }
  | { url: (img: HTMLElement, newUrl: string, width: number) => void };

export type ResizeArgs = {
  root: HTMLElement;
  update: UpdateImageHook;
  origin: string;
  designWidth: number;
};

export async function wireMarkdownImgResizers({
  root,
  update,
  designWidth,
  origin,
}: ResizeArgs): Promise<void> {
  const globalImageLinkRe = markdownPicfitRegex(origin);
  let matching = 0;

  for (const img of root.querySelectorAll<HTMLImageElement>('img')) {
    if (!`![](${img.src})`.match(globalImageLinkRe)) continue;
    const index = matching++;

    if (img.closest('.markdown-img-resizer')) continue; // already wrapped
    try {
      await img.decode();
    } catch {
      continue;
    }

    const pointerdown = async (down: PointerEvent) => {
      const handle = down.currentTarget as HTMLElement;
      const rootStyle = window.getComputedStyle(root);
      const rootPadding = parseInt(rootStyle.paddingLeft) + parseInt(rootStyle.paddingRight);
      const rootWidth = root.clientWidth - (isFinite(rootPadding) ? rootPadding : 0);
      const aspectRatio = img.naturalHeight ? img.naturalWidth / img.naturalHeight : 1;
      const imgClientRect = img.getBoundingClientRect();
      const isBottomDrag = handle.className.includes('bottom');
      const isCornerDrag = !isBottomDrag && imgClientRect.bottom - down.clientY < 18;
      const dir = handle.className.includes('left') ? -1 : 1;

      if (isCornerDrag) handle.style.cursor = dir === 1 ? 'nwse-resize' : 'nesw-resize';
      handle.setPointerCapture?.(down.pointerId);

      img.style.willChange = 'width,height';
      img.style.width = `${imgClientRect.width}px`;
      img.closest<HTMLElement>('.markdown-img-resizer')!.style.width = '';

      const pointermove = (move: PointerEvent) => {
        const deltaX = isCornerDrag
          ? dir * (move.clientX - down.clientX) + (aspectRatio * (move.clientY - down.clientY)) / 2
          : isBottomDrag
            ? (move.clientY - down.clientY) * aspectRatio
            : dir * 2 * (move.clientX - down.clientX);
        const viewportImgWidth = Math.round(
          clamp(imgClientRect.width + deltaX, { min: 128, max: rootWidth }),
        );
        img.style.width = `${viewportImgWidth}px`;
        img.dataset.resizeWidth = String(
          designWidth ? Math.round((viewportImgWidth * designWidth) / rootWidth) : viewportImgWidth,
        );
        img.dataset.widthRatio = String(viewportImgWidth / rootWidth);
      };
      const pointerup = async () => {
        handle.removeEventListener('pointermove', pointermove);
        handle.removeEventListener('pointerup', pointerup);
        handle.removeEventListener('pointercancel', pointerup);
        if (handle.hasPointerCapture(down.pointerId)) handle.releasePointerCapture(down.pointerId);
        img.style.willChange = '';
        handle.style.cursor = '';
        if ('url' in update) return urlUpdate(img, update);

        const markdown = update.markdown();
        const link = [...markdown.matchAll(globalImageLinkRe)][index];
        if (!link?.[1] || !img.dataset.widthRatio) return;

        const { imageUrl } = await xhrJson(`/image-url/${link[3]}?width=${img.dataset.resizeWidth}`);
        const before = markdown.slice(0, link.index);
        const after = markdown.slice(link.index! + link[0].length);
        const newMarkdown = before + `![${link[1]}](${imageUrl})` + after;
        update.markdown(newMarkdown);
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

export function markdownPicfitRegex(origin: string = ''): RegExp {
  return new RegExp(
    String.raw`!\[([^\n\]]*)\]\((${regexQuote(
      origin,
    )}[^)\s]+[?&]path=((?:[a-z]\w+:)?[-_a-z0-9]{12}\.\w{3,4})[^)]*)\)`,
    'gi',
  );
}

const imageIdRe = /&path=([a-z]\w+:[-_a-z0-9]{12}\.\w{3,4})&/i;

async function urlUpdate(img: HTMLImageElement, update: Extract<UpdateImageHook, { url: unknown }>) {
  const imageId = img.src.match(imageIdRe)?.[1];
  const { imageUrl } = await xhrJson(`/image-url/${imageId}?width=${img.dataset.resizeWidth}`);
  const preloadImg = new Image();
  preloadImg.src = imageUrl;
  await preloadImg.decode();
  update.url(img, imageUrl, Number(img.dataset.widthRatio)!);
  return;
}

function dragHandles(img: HTMLImageElement): HTMLElement[] {
  const span = img.closest('.markdown-img-container') ?? wrapImg({ img });
  span.firstElementChild!.classList.add('markdown-img-resizer');
  return [...span.querySelectorAll<HTMLElement>('.resize-handle')];
}

function regexQuote(origin: string) {
  return origin.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
}

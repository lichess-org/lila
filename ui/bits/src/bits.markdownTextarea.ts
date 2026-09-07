import { frag } from 'lib';
import { alert, info, spinnerHtml } from 'lib/view';
import { wireMarkdownImgResizers, naturalSize, markdownPicfitRegex } from 'lib/view/markdownImgResizer';
import { text as xhrText, json as xhrJson, ValidationError } from 'lib/xhr';

// also see markdownTextarea.ts

site.load.then(() => {
  for (const markdown of document.querySelectorAll<HTMLElement>('.markdown-textarea')) {
    wireMarkdownTextarea(markdown);
  }
});

function wireMarkdownTextarea(markdown: HTMLElement) {
  const textarea = markdown.querySelector<HTMLTextAreaElement>('textarea');
  if (!textarea) return;

  const previewTab = markdown.querySelector<HTMLButtonElement>('.preview-tab')!;
  const writeTab = markdown.querySelector<HTMLButtonElement>('.write-tab')!;
  const uploadBtn = markdown.querySelector<HTMLButtonElement>('.upload-image');
  const preview = markdown.querySelector<HTMLElement>('.preview')!;

  previewTab.addEventListener('click', async () => {
    preview.innerHTML = `<div class="busy">${spinnerHtml}</div>`;
    preview.classList.remove('none');
    uploadBtn?.classList.add('none');
    writeTab.classList.remove('active');
    previewTab.classList.add('active');
    const rendered = frag<HTMLElement>(
      await xhrText(`/markdown/preview/${markdown.dataset.formatKey ?? 'forum'}`, {
        method: 'POST',
        body: textarea.value,
      }),
    );
    await Promise.all([
      rendered.querySelector('.lpv--autostart') && site.asset.loadEsm('bits.lpv', { init: { el: rendered } }),
      rendered.querySelector('a') && site.asset.loadEsm('bits.expandText', { init: rendered }),
    ]);
    preview.replaceChildren(rendered);
    if (markdownPicfitRegex().test(textarea.value) && !localStorage.getItem('markdown.rtfm')) {
      await info('Drag a side or bottom edge to resize an image.');
      localStorage.setItem('markdown.rtfm', '1');
    }
    await wireMarkdownImgResizers({
      root: preview,
      update: {
        markdown: (text?: string) => (text !== undefined ? (textarea.value = text) : textarea.value),
      },
      origin: markdown.dataset.imageDownloadOrigin!,
      designWidth: Number(markdown.dataset.imageDesignWidth),
      realm: markdown.dataset.markdownRealm!,
    });
  });

  writeTab.addEventListener('click', () => {
    previewTab.classList.remove('active');
    writeTab.classList.add('active');
    uploadBtn?.classList.remove('none');
    preview.innerHTML = '';
    preview.classList.add('none');
    textarea.focus();
  });
  if (!markdown.dataset.imageUploadUrl) return;

  uploadBtn?.addEventListener('click', () => {
    const input = frag<HTMLInputElement>('<input type="file" accept="image/*" multiple />');
    input.onchange = () => {
      if (!input.files) return;
      for (const file of input.files) uploadAndInsert(file);
    };
    input.click();
  });
  textarea.addEventListener('paste', (e: ClipboardEvent) => {
    if (!e.clipboardData) return;
    if (handleDataTransferItems(e.clipboardData.items)) e.preventDefault();
  });
  textarea.addEventListener('drop', (e: DragEvent) => {
    if (!e.dataTransfer) return;
    if (handleDataTransferItems(e.dataTransfer.items)) e.preventDefault();
  });
  textarea.addEventListener('dragover', e => e.preventDefault());

  const handleDataTransferItems = (items: DataTransferItemList) => {
    const images = [...items].filter(i => i.kind === 'file' && i.type.startsWith('image/'));
    if (images.length === 0) return false;
    for (const image of images) uploadAndInsert(image.getAsFile()!);
    return true;
  };

  const uploadAndInsert = async (image: File) => {
    try {
      const count =
        textarea.value?.match(markdownPicfitRegex(markdown.dataset.imageDownloadOrigin))?.length ?? 0;
      if (count >= Number(markdown.dataset.imageCountMax)) {
        throw `You can only upload ${markdown.dataset.imageCountMax} images here.`;
      }
      preview.innerHTML = `<div class="busy"><span>Uploading image...</span>${spinnerHtml}</div>`;
      preview.classList.remove('none');
      const { width, height } = await naturalSize(image);
      const body = new FormData();
      body.append('context', markdown.dataset.imageContext ?? location.href);
      body.append('dim.width', String(width));
      body.append('dim.height', String(height));
      body.append('image', image);

      const { imageUrl } = await xhrJson(markdown.dataset.imageUploadUrl!, { method: 'POST', body });
      if (!imageUrl) throw '';

      const before = textarea.value.slice(0, textarea.selectionStart);
      const after = textarea.value.slice(textarea.selectionEnd);
      const maybeNewline = /\s$/.test(before) ? '' : '\n';

      textarea.value = `${before}${maybeNewline}![${image.name}](${imageUrl})\n${after}`;
      textarea.selectionStart = textarea.selectionEnd = textarea.value.length - after.length;
    } catch (e) {
      alert(e instanceof ValidationError ? e.message : `Image upload failed: ${e}`);
    } finally {
      preview.classList.add('none');
      preview.innerHTML = '';
    }
  };
}

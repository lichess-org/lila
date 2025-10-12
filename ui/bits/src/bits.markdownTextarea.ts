import { alert } from 'lib/view/dialogs';
import { wireMarkdownImgResizers, naturalSize } from 'lib/view/markdownImgResizer';
import { marked } from 'marked';
import { json as xhrJson } from 'lib/xhr';
import { frag } from 'lib';

site.load.then(() => {
  marked.setOptions({ gfm: true, breaks: true });
  for (const markdown of document.querySelectorAll<HTMLTextAreaElement>('.markdown-textarea')) {
    wireMarkdownTextarea(markdown);
  }
});

function wireMarkdownTextarea(markdown: HTMLElement) {
  const textarea = markdown.querySelector<HTMLTextAreaElement>('textarea');
  if (!textarea) return;

  const previewTab = markdown.querySelector<HTMLElement>('.preview')!;
  const writeTab = markdown.querySelector<HTMLElement>('.write')!;
  const uploadBtn = markdown.querySelector<HTMLButtonElement>('.upload-image')!;
  const preview = markdown.querySelector<HTMLElement>('.comment-preview')!;

  previewTab.addEventListener('click', async () => {
    const html = await marked.parse(textarea.value ?? '');
    preview.innerHTML = html;
    preview.classList.remove('none');
    uploadBtn.classList.add('none');
    writeTab.classList.remove('active');
    previewTab.classList.add('active');
    const resizeUrl = markdown.dataset.imageResizeUrl;
    if (!resizeUrl) return;
    wireMarkdownImgResizers({
      root: preview,
      update: {
        markdown: (text?: string) => (text !== undefined ? (textarea.value = text) : textarea.value),
      },
      designWidth: Number(markdown.dataset.imageDesignWidth),
      origin: markdown.dataset.imageDownloadOrigin,
    });
  });

  writeTab.addEventListener('click', () => {
    previewTab.classList.remove('active');
    writeTab.classList.add('active');
    preview.classList.add('none');
    uploadBtn.classList.remove('none');
    preview.innerHTML = '';
    textarea.focus();
  });
  if (!markdown.dataset.imageUploadUrl) return;

  markdown.querySelector<HTMLElement>('.upload-image')?.addEventListener('click', () => {
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
      const { width, height } = await naturalSize(image);
      const body = new FormData();
      body.append('context', markdown.dataset.imageContext ?? location.href);
      body.append('width', String(width));
      body.append('height', String(height));
      body.append('image', image);

      const { imageUrl } = await xhrJson(markdown.dataset.imageUploadUrl!, { method: 'POST', body });
      if (!imageUrl) throw '';

      const before = textarea.value.slice(0, textarea.selectionStart);
      const after = textarea.value.slice(textarea.selectionEnd);
      const maybeNewline = /\s$/.test(before) ? '' : '\n';

      textarea.value = `${before}${maybeNewline}![${image.name}](${imageUrl})\n${after}`;
      textarea.selectionStart = textarea.selectionEnd = textarea.value.length - after.length;
    } catch (e) {
      alert(String(e) || 'Image upload failed.');
    }
  };
}

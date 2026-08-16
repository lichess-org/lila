import { Editor } from '@toast-ui/editor';
import type { Node as NodeType } from 'prosemirror-model';
import type { Selection as SelectionType } from 'prosemirror-state';
import type { EditorView as EditorViewType } from 'prosemirror-view';

import { frag } from 'lib';
import { currentTheme } from 'lib/device';
import { pubsub } from 'lib/pubsub';
import { alert, enter, spinnerHtml } from 'lib/view';
import { wireMarkdownImgResizers, wrapImg, naturalSize } from 'lib/view/markdownImgResizer';
import { ValidationError, json as xhrJson, text as xhrText } from 'lib/xhr';

export function makeToastEditor(el: HTMLElement, text = '', height = '60vh'): Editor {
  const rewire = () =>
    wireMarkdownImgResizers({
      root: document.querySelector<HTMLElement>('.toastui-editor-ww-container .ProseMirror')!,
      update: { url: updateImage },
      designWidth: Number(el.dataset.imageDesignWidth),
      origin: el.dataset.imageDownloadOrigin!,
      realm: el.dataset.markdownRealm!,
    });
  const toastUi = el.querySelector<HTMLElement>('.toastui-container')!;
  const editor = newToast(el, text, rewire, height);

  setupTabListeners(el);
  rewire();

  pubsub.on('theme', () => {
    const themer = toastUi.querySelector<HTMLElement>('.toastui-editor-defaultUI')!;
    themer.classList.remove('toastui-editor-light', 'toastui-editor-dark');
    themer.classList.add(`toastui-editor-${currentTheme()}`);
  });
  // in a modal, <Enter> should complete the action, not submit the post form
  toastUi.addEventListener(
    'keypress',
    enter(target => {
      const okButton = $(target).parents('.toastui-editor-popup-body').find('.toastui-editor-ok-button')[0];
      if (okButton) $(okButton).trigger('click');
      return !okButton;
    }),
  );
  toastUi.querySelector<HTMLElement>('button.link')?.addEventListener('click', () => {
    document.querySelector<HTMLElement>('#toastuiLinkUrlInput')?.focus();
  });
  return editor;
}

export function getSanitizedMarkdown(editor: Editor): string {
  return editor
    .getMarkdown()
    .replace(/<br>/g, '')
    .replace(/\n\s*#\s/g, '\n## ');
}

function newToast(el: HTMLElement, initialValue: string, rewire: () => void, height: string) {
  return new Editor({
    el: el.matches('.toastui-container') ? el : el.querySelector<HTMLElement>('.toastui-container')!,
    usageStatistics: false,
    height,
    theme: currentTheme(),
    initialValue,
    initialEditType: 'wysiwyg',
    hideModeSwitch: true,
    language: $('html').attr('lang') as string,
    toolbarItems: [
      ['heading', 'bold', 'italic', 'strike'],
      ['hr', 'quote'],
      ['ul', 'ol'],
      ['table', 'image', 'link'],
      ['code', 'codeblock'],
      ['scrollSync'],
    ],
    autofocus: false,
    events: { load: editor => initProseMirror(editor.wwEditor.view as EditorViewType, rewire) },
    hooks: { addImageBlobHook: toastImageUploadHook(el) },
  });
}

function initProseMirror(view: EditorViewType, rewire: () => void) {
  if (!view) return;

  view.setProps({
    nodeViews: { image: imageNodeView(rewire) },
    handleClick: clickOutsideTable,
  });
}

function toastImageUploadHook(el: HTMLElement) {
  if (!el.dataset.imageUploadUrl) return () => alert('Image upload not available.');
  return async (image: Blob, setUrlCallback: (url: string, name?: string) => void) => {
    try {
      if (el.querySelectorAll('.markdown-img-resizer').length >= Number(el.dataset.imageCountMax)) {
        throw `You can only upload ${el.dataset.imageCountMax} images here.`;
      }
      const name = image instanceof File ? image.name : 'image';
      const { width, height } = await naturalSize(image);
      if (!width || !height) throw `Unsupported image '${name}'`;
      const formData = new FormData();
      formData.append('context', el.dataset.imageContext ?? location.href);
      formData.append('dim.width', String(width));
      formData.append('dim.height', String(height));
      formData.append('image', image);
      const { imageUrl } = await xhrJson(el.dataset.imageUploadUrl!, {
        method: 'POST',
        body: formData,
      });
      setUrlCallback(imageUrl, name);
    } catch (e) {
      setUrlCallback('');
      alert(e instanceof ValidationError ? e.message : `Image upload failed: ${e}`);
    }
  };
}

function setupTabListeners(el: HTMLElement) {
  const toastUi = el.querySelector<HTMLElement>('.toastui-container')!;
  const previewTab = el.querySelector<HTMLButtonElement>('.preview-tab')!;
  const writeTab = el.querySelector<HTMLButtonElement>('.write-tab')!;
  const content = el.querySelector<HTMLTextAreaElement>('.markdown-content-textarea')!;
  const preview = el.querySelector<HTMLElement>('.preview')!;
  const toastUiPanes = () => toastUi.querySelectorAll<HTMLElement>('.toastui-editor-defaultUI > *');

  previewTab.addEventListener('click', async () => {
    preview.innerHTML = `<div class="busy">${spinnerHtml}</div>`;
    preview.classList.remove('none');
    writeTab.classList.remove('active');
    previewTab.classList.add('active');
    toastUiPanes().forEach(el => (el.style.visibility = 'hidden'));
    const rendered = frag<HTMLElement>(
      await xhrText(`/markdown/preview/${el.dataset.markdownRealm}`, {
        method: 'POST',
        body: content.value,
      }),
    );
    await Promise.all([
      rendered.querySelector('.lpv--autostart') && site.asset.loadEsm('bits.lpv', { init: { el: rendered } }),
      rendered.querySelector('a') && site.asset.loadEsm('bits.expandText', { init: rendered }),
    ]);
    preview.replaceChildren(rendered);
  });

  writeTab.addEventListener('focus', () => {
    previewTab.classList.remove('active');
    writeTab.classList.add('active');
    toastUiPanes().forEach(el => (el.style.visibility = 'visible'));
    preview.classList.add('none');
    preview.innerHTML = '';
    toastUi.querySelector<HTMLElement>('.toastui-editor-ww-container .ProseMirror')?.focus();
  });
}

// interpret clicks outside of a document ending table's inline-end boundary as intent to advance the
// insertion cursor past it, inserting a newline
function clickOutsideTable(view: EditorViewType, _pos: number, event: MouseEvent) {
  if (event.button !== 0) return false;
  let tablePos: number | undefined;
  view.state.doc.forEach((node: NodeType, pos: number) => {
    if (node.type.name !== 'table') return;
    const table = view.nodeDOM(pos);
    if (!(table instanceof HTMLElement)) return;
    const bounds = table.getBoundingClientRect();
    const beyondInlineEnd =
      window.getComputedStyle(table).direction === 'rtl'
        ? event.clientX < bounds.left
        : event.clientX > bounds.right;
    if (beyondInlineEnd && event.clientY >= bounds.top && event.clientY <= bounds.bottom) tablePos = pos;
  });
  if (tablePos === undefined) return false;

  const table = view.state.doc.nodeAt(tablePos)!;
  const afterTable = tablePos + table.nodeSize;
  if (view.state.doc.resolve(afterTable).nodeAfter) return false;
  const transaction = view.state.tr.insert(afterTable, view.state.schema.nodes.paragraph.create());
  const Selection = view.state.selection.constructor as typeof SelectionType;
  view.dispatch(
    transaction.setSelection(Selection.near(transaction.doc.resolve(afterTable), 1)).scrollIntoView(),
  );
  return true;
}

type ProseMirrorProps = { getPos: () => number | undefined; view: EditorViewType };
const proseMirrorProps = new WeakMap<HTMLElement, ProseMirrorProps>();

function imageNodeView(rewire: () => void) {
  return (node: NodeType, view: EditorViewType, getPos: () => number | undefined) => {
    const dom = wrapImg({ src: node.attrs.imageUrl, alt: node.attrs.altText });
    const span = dom.firstElementChild as HTMLElement;
    const img = dom.querySelector<HTMLImageElement>('img')!;
    proseMirrorProps.set(img, { getPos, view });
    setTimeout(rewire);
    return {
      dom,
      update(newNode: NodeType) {
        if (newNode.type !== node.type) return false;
        if (newNode.attrs.imageUrl !== node.attrs.imageUrl) img.src = newNode.attrs.imageUrl || '';
        if (newNode.attrs.styleWidth !== node.attrs.styleWidth)
          span.style.width = newNode.attrs.styleWidth ?? '';
        node = newNode;
        return true;
      },
      ignoreMutation: () => true,
    };
  };
}

function updateImage(img: HTMLElement, imageUrl: string, widthRatio?: number) {
  const { getPos, view } = proseMirrorProps.get(img) || {};
  if (!view) return;
  const pos = getPos?.();
  if (pos === undefined) return;
  view.dispatch(
    view.state.tr.setNodeMarkup(pos, undefined, {
      ...view.state.doc.nodeAt(pos)?.attrs,
      styleWidth: widthRatio ? `${widthRatio * 100}%` : null,
      imageUrl,
    }),
  );
}

import type { EditorView as EditorViewType } from 'prosemirror-view';
import type { Node as NodeType, Schema as SchemaType } from 'prosemirror-model';
import type { EditorState as EditorStateType } from 'prosemirror-state';
import { json as xhrJson } from 'lib/xhr';
import { Editor } from '@toast-ui/editor';
import { currentTheme } from 'lib/device';
import { wireMarkdownImgResizers, wrapImg, naturalSize } from 'lib/view/markdownImgResizer';
import { enter } from 'lib/view';

export function makeToastEditor(el: HTMLTextAreaElement, text: string = '', height: string = '60vh'): Editor {
  const rewire = () =>
    wireMarkdownImgResizers({
      root: document.querySelector<HTMLElement>('.toastui-editor-ww-container .ProseMirror')!,
      update: { url: updateImage },
      designWidth: Number(el.dataset.imageDesignWidth),
      origin: el.dataset.imageDownloadOrigin!,
    });
  const editor = newToast(el, text, rewire, height);
  rewire();

  // in a modal, <Enter> should complete the action, not submit the post form
  $(el).on(
    'keypress',
    enter(target => {
      const okButton = $(target).parents('.toastui-editor-popup-body').find('.toastui-editor-ok-button')[0];
      if (okButton) $(okButton).trigger('click');
      return !okButton;
    }),
  );
  $(el)
    .find('button.link')
    .on('click', () => $('#toastuiLinkUrlInput')[0]?.focus());

  return editor;
}

export function getSanitizedMarkdown(editor: Editor): string {
  return editor
    .getMarkdown()
    .replace(/<br>/g, '')
    .replace(/\n\s*#\s/g, '\n## ');
}

function newToast(el: HTMLElement, initialValue: string, rewire: () => void, editorHeightStyle: string) {
  return new Editor({
    el,
    usageStatistics: false,
    height: editorHeightStyle,
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

  const old = view.state.schema;
  const imageSpec = old.nodes['image'].spec;
  // can't import the ProseMirror javascript because toastui bundles it,
  // so put on the gloves, reach in, and grab some constructors
  const Schema = old.constructor as new (cfg: { nodes: any; marks: any }) => SchemaType;
  const EditorState = view.state.constructor as typeof EditorStateType & {
    create(cfg: any): EditorStateType;
  };
  const Node = view.state.doc.constructor as typeof NodeType & {
    fromJSON(s: SchemaType, j: any): NodeType;
  };
  const nodes = old.spec.nodes.update('image', {
    ...imageSpec,
    attrs: { ...imageSpec.attrs, styleWidth: { default: null } },
  });
  const schema = new Schema({ nodes, marks: old.spec.marks });
  const newState = EditorState.create({
    schema,
    doc: Node.fromJSON(schema, view.state.doc.toJSON()),
    plugins: view.state.plugins,
  });

  view.updateState(newState);
  view.setProps({ nodeViews: { image: imageNodeView(rewire) } });

  let transaction = view.state.tr;
  view.state.doc.descendants((n: NodeType, pos: number) => {
    if (n.type && n.type.name === 'image') {
      transaction = transaction.setNodeMarkup(pos, n.type, n.attrs, n.marks);
    }
  });
  if (transaction.docChanged) view.dispatch(transaction);
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

function toastImageUploadHook(el: HTMLElement) {
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
      throw e;
    }
  };
}

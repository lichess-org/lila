import type { EditorView as EditorViewType } from 'prosemirror-view';
import type { Node as NodeType, Schema as SchemaType } from 'prosemirror-model';
import type { EditorState as EditorStateType } from 'prosemirror-state';
import { json as xhrJson } from 'lib/xhr';
import { Editor } from '@toast-ui/editor';
import { currentTheme } from 'lib/device';
import { wireMarkdownImgResizers, wrapImg } from 'lib/view/markdownImgResizer';

export function makeToastEditor(el: HTMLTextAreaElement, text: string = '', height: string = '60vh'): Editor {
  const editor = newToast(el, text, height);

  rewire();

  // in a modal, <Enter> should complete the action, not submit the post form
  $(el).on('keypress', event => {
    if (event.key != 'Enter') return;
    const okButton = $(event.target)
      .parents('.toastui-editor-popup-body')
      .find('.toastui-editor-ok-button')[0];
    if (okButton) $(okButton).trigger('click');
    return !okButton;
  });
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

function newToast(el: HTMLElement, initialValue: string, height: string) {
  return new Editor({
    el,
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
    events: { load: editor => initProseMirror(editor.wwEditor.view as EditorViewType) },
    hooks: {
      addImageBlobHook: (blob, cb) => {
        const name = blob instanceof File ? blob.name : 'image';
        const formData = new FormData();
        formData.append('image', blob);
        xhrJson(el.getAttribute('data-image-upload-url')!, { method: 'POST', body: formData })
          .then(data => cb(data.imageUrl, name))
          .catch(e => {
            cb('');
            throw e;
          });
      },
    },
  });
}

function initProseMirror(view: EditorViewType) {
  if (!view) return;

  const old = view.state.schema;
  const imageSpec = old.nodes['image']!.spec;
  // can't import the ProseMirror javascript because toastui bundles it,
  // so put on the gloves, reach in, and grab some constructors
  const Schema = (old as any).constructor as new (cfg: { nodes: any; marks: any }) => SchemaType;
  const EditorState = (view.state as any).constructor as typeof EditorStateType & {
    create(cfg: any): EditorStateType;
  };
  const Node = (view.state.doc as any).constructor as typeof NodeType & {
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
  view.setProps({ nodeViews: { image: imageNodeView } });

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

function imageNodeView(node: NodeType, view: EditorViewType, getPos: () => number | undefined) {
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
}

function rewire() {
  wireMarkdownImgResizers({
    root: document.querySelector<HTMLElement>('.toastui-editor-ww-container .ProseMirror')!,
    updateImage: {
      url: (img: HTMLElement, imageUrl: string, widthRatio?: number) => {
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
      },
    },
  });
}

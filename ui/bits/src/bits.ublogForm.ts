import * as xhr from 'common/xhr';
import throttle from 'common/throttle';
import Editor from '@toast-ui/editor';
import Tagify from '@yaireo/tagify';
import { currentTheme } from 'common/theme';
import { wireCropDialog } from './exports/crop';

site.load.then(() => {
  $('.markdown-editor').each(function (this: HTMLTextAreaElement) {
    setupMarkdownEditor(this);
  });
  $('#form3-topics').each(function (this: HTMLTextAreaElement) {
    setupTopics(this);
  });
  $('.flash').addClass('fade');
  wireCropDialog({
    aspectRatio: 8 / 5,
    post: { url: $('.ublog-image-edit').attr('data-post-url')!, field: 'image' },
    max: { pixels: 1600 },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });
});

const setupTopics = (el: HTMLTextAreaElement) =>
  new Tagify(el, {
    whitelist: el.dataset['rel']?.split(','),
    enforceWhitelist: true,
    // userInput: false,
    maxTags: 5,
    dropdown: { enabled: 0, maxItems: 20, highlightFirst: true, closeOnSelect: false },
    originalInputValueFormat: tags => tags.map(t => t.value).join(','),
  });

const setupMarkdownEditor = (el: HTMLTextAreaElement) => {
  const postProcess = (markdown: string) => markdown.replace(/<br>/g, '').replace(/\n\s*#\s/g, '\n## ');

  const editor: Editor = new Editor({
    el,
    usageStatistics: false,
    height: '60vh',
    theme: currentTheme(),
    initialValue: $('#form3-markdown').val() as string,
    initialEditType: 'wysiwyg',
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
    events: {
      change: throttle(500, () => $('#form3-markdown').val(postProcess(editor.getMarkdown()))),
    },
    hooks: {
      addImageBlobHook: (blob, cb) => {
        const formData = new FormData();
        formData.append('image', blob);
        xhr
          .json(el.getAttribute('data-image-upload-url')!, { method: 'POST', body: formData })
          .then(data => cb(data.imageUrl, ''))
          .catch(e => {
            cb('');
            throw e;
          });
      },
    },
  });
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
};

import Tagify from '@yaireo/tagify';

import { throttle } from 'lib/async';

import { wireCropDialog } from './crop';
import { makeToastEditor, getSanitizedMarkdown } from './toastEditor';

site.load.then(() => {
  $('.markdown-toastui').each(function (this: HTMLTextAreaElement) {
    const markdownForm = $('#form3-markdown');
    const editor = makeToastEditor(this, markdownForm.val() as string, '60vh');
    editor.on(
      'change',
      throttle(500, () => markdownForm.val(getSanitizedMarkdown(editor))),
    );
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

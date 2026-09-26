import Tagify from '@yaireo/tagify';

import { throttle } from 'lib/async';

import { wireCropDialog } from './crop';
import { makeToastEditor, getSanitizedMarkdown } from './toastEditor';

site.load.then(() => {
  $('.markdown-toastui').each(function (this: HTMLElement) {
    const markdownForm = this.querySelector<HTMLTextAreaElement>('.markdown-content-textarea')!;
    const editor = makeToastEditor(this, markdownForm.value);
    editor.on(
      'change',
      throttle(500, () => (markdownForm.value = getSanitizedMarkdown(editor))),
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
    onCropped: blob => {
      if (!blob) return;
      const img = document.querySelector<HTMLImageElement>('img.ublog-post-image')!;
      const url = URL.createObjectURL(blob);
      img.src = url;
      img.onload = img.onerror = () => URL.revokeObjectURL(url);
    },
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

import { throttle } from 'lib/async';
import { sortTable } from 'lib/tablesort';
import { makeToastEditor, getSanitizedMarkdown } from './toastEditor';

site.load.then(() => {
  $('.markdown-toastui').each(function (this: HTMLTextAreaElement) {
    const editor = makeToastEditor(this, $('#form3-markdown').val() as string, '60vh');
    editor.on(
      'change',
      throttle(500, () => $('#form3-markdown').val(getSanitizedMarkdown(editor))),
    );
  });
  $('.flash').addClass('fade');
  $('table.cms__pages').each(function (this: HTMLTableElement) {
    sortTable(this, { descending: true });
  });
  $('.cms__pages__search').on('input', function (this: HTMLInputElement) {
    const query = this.value.toLowerCase().trim();
    $('.cms__pages')
      .toggleClass('searching', !!query)
      .find('tbody tr')
      .each(function (this: HTMLTableRowElement) {
        const match =
          $(this).find('.title').text().toLowerCase().includes(query) ||
          $(this).find('.lang').text().toLowerCase() === query;
        this.hidden = !!query && !match;
      });
  });
});

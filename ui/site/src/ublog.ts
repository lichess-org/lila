import * as xhr from 'common/xhr';
import spinner from './component/spinner';
import Editor from '@toast-ui/editor';

lichess.load.then(() => {
  $('.ublog-post-form__image').each(function (this: HTMLFormElement) {
    const form = this;
    $(form)
      .find('input[name="image"]')
      .on('change', () => {
        const replace = (html: string) => $(form).find('.ublog-post-image').replaceWith(html);
        const wrap = (html: string) => '<div class="ublog-post-image">' + html + '</div>';
        replace(wrap(spinner));
        xhr.formToXhr(form).then(
          html => replace(html),
          err => replace(wrap(`<bad>${err}</bad>`))
        );
      });
  });
  $('.flash').addClass('fade');
  $('#markdown-editor').each(function (this: HTMLTextAreaElement) {
    const editor = new Editor({
      el: this,
      usageStatistics: false,
      height: '70vh',
      theme: $('body').data('theme') == 'light' ? 'light' : 'dark',
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
      events: {
        change() {
          $('#form3-markdown').val(editor.getMarkdown());
        },
      },
    });
  });
});

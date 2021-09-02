import * as xhr from 'common/xhr';
import spinner from './component/spinner';

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
});

import * as xhr from 'common/xhr';

lichess.load.then(() => {
  $('.ublog-post-form__image').each(function (this: HTMLFormElement) {
    const form = this;
    $(form)
      .find('input[name="image"]')
      .on('change', () => {
        xhr.formToXhr(form).then(html => $(form).find('img.ublog-post-image').replaceWith(html), alert);
      });
  });
});

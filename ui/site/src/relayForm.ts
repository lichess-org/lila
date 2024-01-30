import * as xhr from 'common/xhr';

lichess.load.then(() => {
  $('.relay-form__image').each(function (this: HTMLFormElement) {
    setupImage(this);
  });
});

const setupImage = (form: HTMLFormElement) => {
  const showText = () =>
    $('.relay-form__image-text').toggleClass('visible', $('.relay-image').hasClass('user-image'));
  const submit = () => {
    const replace = (html: string) => $(form).find('.relay-image,.relay-image--fallback').replaceWith(html);
    const wrap = (html: string) => '<div class="relay-image">' + html + '</div>';
    xhr.formToXhr(form).then(
      html => {
        replace(html);
        showText();
      },
      err => replace(wrap(`<bad>${err}</bad>`)),
    );
    replace(wrap(lichess.spinnerHtml));
    return false;
  };
  $(form).on('submit', submit);
  $(form).find('input[name="image"]').on('change', submit);
  showText();
};

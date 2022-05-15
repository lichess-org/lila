import * as xhr from 'common/xhr';
//import modal from 'common/modal';

lichess.load.then(() => {
  $('form.autosubmit').each(function (this: HTMLFormElement) {
    const form = this,
      $form = $(form);
    $form.find('choice').on('change', function (this: HTMLInputElement) {
      xhr.formToXhr(form).then(() => {
        window.alert('done!!!');
      });
    });
  });
  $('.poll').on('click', 'input.choice', function (this: HTMLInputElement) {
    return true;
  });
});

import * as xhr from 'common/xhr';

lichess.load.then(() => {
  const localPrefs: [string, string, string, number][] = [
    ['behavior', 'arrowSnap', 'arrow.snap', 1],
    ['behavior', 'courtesy', 'courtesy', 0],
    ['behavior', 'scrollMoves', 'scrollMoves', 1],
  ];

  $('.security table form').on('submit', function (this: HTMLFormElement) {
    xhr.text(this.action, { method: 'post', body: new URLSearchParams(new FormData(this) as any) });
    $(this).parent().parent().remove();
    return false;
  });

  $('form.autosubmit').each(function (this: HTMLFormElement) {
    const form = this,
      $form = $(form),
      showSaved = () => $form.find('.saved').removeClass('none');
    $form.find('input').on('change', function (this: HTMLInputElement) {
      localPrefs.forEach(([categ, name, storeKey]) => {
        if (this.name == `${categ}.${name}`) {
          lichess.storage.set(storeKey, this.value);
          showSaved();
        }
      });
      xhr.formToXhr(form).then(() => {
        showSaved();
        lichess.storage.fire('reload-round-tabs');
      });
    });
  });

  localPrefs.forEach(([categ, name, storeKey, def]) =>
    $(`#ir${categ}_${name}_${lichess.storage.get(storeKey) || def}`).prop('checked', true)
  );

  $('form[action="/account/oauth/token/create"]').each(function (this: HTMLFormElement) {
    const form = $(this),
      submit = form.find('button.submit');
    let isDanger = false;
    const checkDanger = () => {
      isDanger = !!form.find('.danger input:checked').length;
      submit.toggleClass('button-red confirm', isDanger);
      submit.attr('data-icon', isDanger ? '' : '');
      submit.attr('title', isDanger ? submit.data('danger-title') : '');
    };
    checkDanger();
    form.find('input').on('change', checkDanger);
    submit.on('click', function (this: HTMLElement) {
      return !isDanger || confirm(this.title);
    });
  });
});

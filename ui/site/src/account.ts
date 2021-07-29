import * as xhr from 'common/xhr';

lichess.load.then(() => {
  const localPrefs: [string, string, string, number][] = [
    ['behavior', 'arrowSnap', 'arrow.snap', 1],
    ['behavior', 'courtesy', 'courtesy', 0],
    ['behavior', 'scrollMoves', 'scrollMoves', 1],
    ['behavior', 'spectatorWakeLock', 'spectatorWakeLock', 0],
  ];

  $('.security table form').on('submit', function (this: HTMLFormElement) {
    xhr.text(this.action, { method: 'post', body: new URLSearchParams(new FormData(this) as any) });
    $(this).parent().parent().remove();
    return false;
  });

  $('form.autosubmit').each(function (this: HTMLFormElement) {
    const form = this,
      $form = $(form),
      showSaved = () => $form.find('.saved').show();
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
});

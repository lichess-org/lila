const $form = $('main.importer form');

$form.on('submit', () => setTimeout(() => $form.html(site.spinnerHtml), 50));

$form.find('input[type=file]').on('change', function (this: HTMLInputElement) {
  const file = this.files?.[0];
  if (!file) return;

  const reader = new FileReader();
  reader.onload = e => $form.find('textarea').val(e.target?.result as string);
  reader.readAsText(file);
});

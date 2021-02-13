$(function () {
  var $form = $('main.importer form');
  $form.on('submit', () => setTimeout(() => $form.html(lichess.spinnerHtml), 50));

  if (window.FileReader) {
    $form.find('input[type=file]').on('change', function () {
      var file = this.files[0];
      if (!file) return;
      var reader = new FileReader();
      reader.onload = function (e) {
        $form.find('textarea').val(e.target.result);
      };
      reader.readAsText(file);
    });
  } else $form.find('.upload').remove();
});

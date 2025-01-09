import { spinnerHtml } from 'common/spinner';

window.lishogi.ready.then(() => {
  const $form = $('main.importer form');
  $form.submit(function () {
    setTimeout(function () {
      $form.html(spinnerHtml);
    }, 50);
  });
  if (window.FileReader) {
    function readFile(file, encoding) {
      if (!file) return;
      const reader = new FileReader();
      reader.onload = function (e) {
        const res = e.target?.result as string;
        if (res && encoding === 'UTF-8' && res.match(/�/)) {
          console.log(
            "UTF-8 didn't work, trying shift-jis, if you still have problems with your import, try converting the file to a different encoding",
          );
          readFile(file, 'shift-jis');
        } else {
          $form.find('textarea').val(res);
        }
      };
      reader.readAsText(file, encoding);
    }
    $form.find('input[type=file]').on('change', function () {
      readFile((this as any).files[0], 'UTF-8');
    });
  } else $form.find('.upload').remove();
});

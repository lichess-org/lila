$(function () {
  var $form = $('main.importer form');
  $form.submit(function () {
    setTimeout(function () {
      $form.html(lishogi.spinnerHtml);
    }, 50);
  });
  if (window.FileReader) {
    function readFile(file, encoding) {
      if (!file) return;
      var reader = new FileReader();
      reader.onload = function (e) {
        var res = e.target.result;
        if (encoding === 'UTF-8' && res.match(/�/)) {
          console.log(
            "UTF-8 didn't work, trying shift-jis, if you still have problems with your import, try converting the file to a different encoding"
          );
          readFile(file, 'shift-jis');
        } else {
          $form.find('textarea').val(res);
        }
      };
      reader.readAsText(file, encoding);
    }
    $form.find('input[type=file]').on('change', function () {
      readFile(this.files[0], 'UTF-8');
    });
  } else $form.find('.upload').remove();
});

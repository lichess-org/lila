import { spinnerHtml } from 'common/spinner';

window.lishogi.ready.then(() => {
  $('.streamer_picture form.upload input[type=file]').change(function () {
    $('.picture_wrap').html(spinnerHtml);
    $(this).parents('form').submit();
  });
});

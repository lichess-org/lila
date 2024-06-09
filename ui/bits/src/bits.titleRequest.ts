import { wireCropDialog } from './load/crop';

site.load.then(() => {
  $('.title-image-edit').each(function (this: HTMLElement) {
    wireCropDialog({
      post: { url: $(this).attr('data-post-url')!, field: 'image' },
      selectClicks: $(this).find('.drop-target'),
      selectDrags: $(this).find('.drop-target'),
    });
  });

  $('.title-mod__actions form').on('submit', function (ev) {
    if (ev.submitter.value == 'feedback' && new FormData(ev.target).get('text')?.toString().trim() == '') {
      ev.preventDefault();
      alert('Please enter feedback text.');
      return false;
    }
  });
});

import * as xhr from 'common/xhr';

lichess.load.then(() => {
  $('.streamer-show, .streamer-list').on('change', '.streamer-subscribe input', (e: Event) => {
    const target = e.target as HTMLInputElement;
    $(target)
      .parents('.streamer-subscribe')
      .each(function (this: HTMLElement) {
        xhr.text(
          $(this)
            .data('action')
            .replace(/set=[^&]+/, `set=${target.checked}`),
          { method: 'post' },
        );
      });
  });
  $('.select-image').on('click', cropDialog);
  $('.drop-target')
    .on('click', cropDialog)
    .on('dragover', e => e.preventDefault())
    .on('drop', e => {
      e.preventDefault();
      if (e.dataTransfer.files.length !== 1) return;
      cropDialog(e.dataTransfer.files[0]);
    });
});

function cropDialog(blob?: Blob) {
  lichess.asset.loadEsm('cropDialog', {
    init: {
      aspectRatio: 1,
      source: blob,
      max: { pixels: 350 },
      post: { url: '/upload/image/streamer', field: 'picture' },
      onCropped: (result: Blob | boolean) => {
        if (result) lichess.reload();
      },
    },
  });
}

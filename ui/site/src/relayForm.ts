import { isSafari } from 'common/device';

if (isSafari()) lichess.asset.loadEsm('cropDialog'); // preload

lichess.load.then(() => {
  const postUrl = $('.relay-image-edit').attr('data-post-url')!;
  $('.select-image').on('click', () => cropDialog(postUrl));
  $('.drop-target')
    .on('click', () => cropDialog(postUrl))
    .on('dragover', e => e.preventDefault())
    .on('drop', e => {
      e.preventDefault();
      if (e.dataTransfer.files.length !== 1) return;
      cropDialog(postUrl, e.dataTransfer.files[0]);
    });
});

function cropDialog(url: string, blob?: Blob) {
  lichess.asset.loadEsm('cropDialog', {
    init: {
      aspectRatio: 2,
      source: blob,
      max: { megabytes: 6 },
      post: { url, field: 'image' },
      onCropped: (result: Blob | boolean) => {
        if (result) lichess.reload();
      },
    },
  });
}

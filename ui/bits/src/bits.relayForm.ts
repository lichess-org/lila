import { wireCropDialog } from './load/crop';

site.load.then(() => {
  wireCropDialog({
    aspectRatio: 2 / 1,
    post: { url: $('.relay-image-edit').attr('data-post-url')!, field: 'image' },
    selectClicks: $('.select-image, .drop-target'),
    selectDrags: $('.drop-target'),
  });
  const pinned = document.querySelector('.relay-pinned-streamer-edit') as HTMLElement;
  if (!pinned) return;

  wireCropDialog({
    aspectRatio: 16 / 9,
    post: { url: pinned.dataset.postUrl!, field: 'image' },
    selectClicks: $('.streamer-select-image, .streamer-drop-target'),
    selectDrags: $('.streamer-drop-target'),
  });

  const streamerInput = pinned.querySelector('#form3-pinnedStreamer') as HTMLInputElement;

  if (!streamerInput.value)
    (pinned.querySelector('.streamer-select-image') as HTMLElement).style.visibility = 'hidden';

  const deleteImageBtn = pinned.querySelector('.streamer-delete-image') as HTMLElement;
  if (!pinned.querySelector('img')) deleteImageBtn.style.visibility = 'hidden';
  else
    deleteImageBtn.onclick = () =>
      fetch(deleteImageBtn.dataset.postUrl!, { method: 'POST', body: new FormData() }).then(site.reload);
});

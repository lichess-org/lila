import * as xhr from 'common/xhr';
import throttle from 'common/throttle';

lichess.load.then(() => {
  $('.flash').addClass('fade');
  $('.ublog-post__like').on(
    'click',
    throttle(1000, function (this: HTMLButtonElement) {
      const button = $(this),
        likeClass = 'ublog-post__like--liked',
        liked = !button.hasClass(likeClass);
      xhr
        .text(`/ublog/${button.data('rel')}/like?v=${liked}`, {
          method: 'post',
        })
        .then(likes => button.text(likes).toggleClass(likeClass, liked));
    })
  );
  $('#form3-tier').on('change', function (this: HTMLSelectElement) {
    (this.parentNode as HTMLFormElement).submit();
  });
});

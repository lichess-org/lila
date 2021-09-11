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
        .then(likes => {
          $('.ublog-post__like__nb').text(likes);
          $('.ublog-post__like').toggleClass(likeClass, liked);
        });
    })
  );
  $('.ublog-post__follow button').on(
    'click',
    throttle(1000, function (this: HTMLButtonElement) {
      const button = $(this),
        followClass = 'followed';
      xhr
        .text(button.data('rel'), {
          method: 'post',
        })
        .then(() => button.parent().toggleClass(followClass));
    })
  );
  $('#form3-tier').on('change', function (this: HTMLSelectElement) {
    (this.parentNode as HTMLFormElement).submit();
  });
});

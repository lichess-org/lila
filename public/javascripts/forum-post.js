$(function () {
  $('.edit.button').add('.edit-post-cancel').click(function(e) {
    e.preventDefault();

    var post = $(this).closest('.post');
    var message = post.find('.message').toggle();
    var form = post.find('form.edit-post-form').toggle();

    form[0].reset();
    form.find('textarea').height(message.height());
  });
});

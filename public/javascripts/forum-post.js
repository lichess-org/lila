$(function () {
  $('.edit.button').add('.edit-post-cancel').click(function(e) {
    e.preventDefault();
    var post = $(this).closest('.post');
    post.find('.message').toggle();
    post.find('form.edit-post-form').toggle()[0].reset();
  });
});

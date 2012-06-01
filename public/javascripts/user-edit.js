$(function() {
  $('div.editable').each(function() {
    $(this).editableSet({
      event: "click",
    action: $(this).attr('data-url'),
    dataType: 'json',
    afterLoad: function() {
      $(this).find('textarea').each(function() {
        var $textarea = $(this).attr('disabled', true);
        $.get($textarea.attr('provider-url'), function(data) {
          $textarea.val(data).attr('disabled', false);
        });
      });
    }
    });
  });
});

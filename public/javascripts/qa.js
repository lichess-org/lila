$(function() {
  var $qa = $('#qa');
  $qa.find('form.question').each(function() {
    var $form = $(this);
    $form.find('input.tm-input').each(function() {
      var $input = $(this);
      var tagApi;
      tagApi = $input.tagsManager({
        prefilled: $input.data('prefill'),
        backspace: [],
        delimiters: [13, 44],
        tagsContainer: $form.find('.tags')
      });
      var tagSource = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.whitespace('value'),
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        local: $.map($input.data('tags').split(','), function(t) {
          return {
            value: t
          };
        })
      });
      tagSource.initialize();
      $input.typeahead({
        minLength: 1,
        highlight: true
      }, {
        source: tagSource.ttAdapter(),
        name: 'tags',
        displayKey: 'value',
        limit: 15
      }).on('typeahead:selected', function(e, d) {
        tagApi.tagsManager("pushTag", d.value);
        $input.val('');
      });
    });
  });
  $qa.on('click', '.your-comment .toggle', function() {
    var $form = $(this).siblings('form');
    $form.toggle(200, function() {
      if ($form.is(':visible')) $form.find('textarea').focus();
    });
  });
  $qa.find('.your-comment form').submit(function() {
    if ($(this).find('textarea').val().length < 20) {
      alert("Comment must be longer than 20 characters");
      return false;
    }
  });
});

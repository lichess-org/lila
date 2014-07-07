$(function() {
  var $qa = $('#qa');
  $qa.find('form.question').each(function() {
    var $form = $(this);
    var $tmInput = $form.find('input.tm-input');
    $tmInput.each(function() {
      var tagApi = $tmInput.tagsManager({
        prefilled: $tmInput.data('prefill'),
        backspace: [],
        delimiters: [13, 44],
        tagsContainer: $form.find('.tags_list'),
        maxTags: 5
      });
      var tagSource = new Bloodhound({
        datumTokenizer: Bloodhound.tokenizers.obj.whitespace('value'),
        queryTokenizer: Bloodhound.tokenizers.whitespace,
        local: $.map($tmInput.data('tags').split(','), function(t) {
          return {
            value: t
          };
        })
      });
      tagSource.initialize();
      $tmInput.typeahead({
        minLength: 1,
        highlight: true
      }, {
        source: tagSource.ttAdapter(),
        name: 'tags',
        displayKey: 'value',
        limit: 15
      }).on('typeahead:selected', function(e, d) {
        tagApi.tagsManager("pushTag", d.value);
        $tmInput.val('');
      });
      $form.submit(function() {
        var tag = $tmInput.val();
        if (tag) {
          tagApi.tagsManager("pushTag", tag);
        }
      });
    });
  });

  $qa.on('click', '.upvote.enabled a', function() {
    var $a = $(this);
    $.ajax({
      method: 'post',
      url: $a.parent().data('post-url'),
      data: {
        vote: $a.data('vote')
      },
      success: function(html) {
        $a.parent().replaceWith(html);
      }
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
  $qa.find('.answer').each(function() {
    var $answer = $(this);
    $answer.on('click', '.toggle-edit-answer', function() {
      $answer.toggleClass('edit');
      if (!$answer.hasClass('edit-loaded')) {
        $answer.addClass('edit-loaded');
        $answer.find('form.edit-answer').submit(function() {
          if ($(this).find('textarea').val().length < 30) {
            alert("Answer must be longer than 30 characters");
            return false;
          }
        });
      }
    });
  });
});

$(function() {
  var urlRegex = /\/study\/(\w{8})#(\w{8})/;
  var width = 744;
  $('div.embed_study').each(function() {
    $(this).find('a').each(function() {
      var matches = this.href.match(urlRegex);
      if (matches && matches[2]) $(this).replaceWith(
        $('<iframe>')
        .addClass('study')
        .css({
          width: width,
          height: width / 1.618
        })
        .attr('src', '/study/embed/' + matches[1] + '/' + matches[2])
      );
    });
  });
});

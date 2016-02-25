module.exports = function(onChange) {
  $('.variant_selector').on('click', 'a', function() {
    onChange($(this).data('variant'));
    return false;
  });
};

module.exports = {
  incrementalId: function(obj, it) {
    obj.id = it + 1;
    return obj;
  },
  assetUrl: $('body').data('asset-url') + '/assets/',
  congratulation: function() {
    var list = [
      'Awesome!',
      'Excellent!',
      'Great job!',
      'Perfect!',
      'Outstanding!',
      'Way to go!',
      'Yes, yes, yes!',
      'You\'re good at this!'
    ];
    return list[Math.floor(Math.random() * list.length)];
  }
};

var m = require('mithril');
var mapView = require('./mapView');

module.exports = function(opts) {
  return {
    controller: function() {
      opts.lessonId = null;
      opts.route = 'map';
      return {
        data: opts.data
      };
    },
    view: mapView
  };
}

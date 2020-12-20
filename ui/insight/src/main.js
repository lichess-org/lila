var m = require('mithril');

var ctrl = require('./ctrl');
var view = require('./view');

module.exports = function(element, opts) {

  var controller = new ctrl(opts, element);

  m.module(element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  return controller;
};

// for multiple-select
jQuery.fn.extend( {
  hover: function( fnOver, fnOut ) {
    return this.on('mouseenter', fnOver ).on('mouseleave', fnOut || fnOver );
  }
} );

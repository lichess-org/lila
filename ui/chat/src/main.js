var m = require('mithril');
var ctrl = require('./ctrl');

module.exports = function(element, opts) {

  var controller = ctrl(opts);

  m.mount(element, {
    oninit: function(vnode) {
      vnode.state = controller;
    },
    view: require('./view')
  });

  lichess.pubsub.emit('chat.ready', controller);

  Mousetrap.bind('/', function() {
    element.querySelector('input.lichess_say').focus();
    return false;
  });

  return {
    newLine: controller.newLine,
    preset: controller.preset
  };
};

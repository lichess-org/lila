// import { sayHello } from "./greet";

// module.exports = function(node: Node, opts: Object) {

//   console.log(node, opts, sayHello("TypeScript"));
// }

import { init } from 'snabbdom';
let patch = init([ // Init patch function with chosen modules
  require('snabbdom/modules/class').default, // makes it easy to toggle classes
  require('snabbdom/modules/props').default, // for setting properties on DOM elements
  require('snabbdom/modules/eventlisteners').default, // attaches event listeners
]);

const makeCtrl = require('./ctrl');
const view = require('./view');

module.exports = (element: Node, opts: Object) => {

  let vnode, ctrl

  let render = () => {
    vnode = patch(vnode, view(ctrl));
  }

  ctrl = makeCtrl(opts)

  patch(element, view(ctrl))

  lichess.pubsub.emit('chat.ready', ctrl)

  Mousetrap.bind('/', function() {
    element.querySelector('input.lichess_say').focus();
    return false;
  })

  return {
    newLine: ctrl.newLine,
    preset: ctrl.preset
  };
};

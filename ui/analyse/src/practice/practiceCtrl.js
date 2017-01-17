var winningChances = require('ceval').winningChances;
var m = require('mithril');

module.exports = function(root) {

  var checkCeval = function() {
  };

  return {
    onCeval: checkCeval,
    close: root.toggleRetro,
    trans: root.trans
  };
};

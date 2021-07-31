const runCtrl = require('./runCtrl');
const runView = require('./runView');

module.exports = function (opts, trans) {
  return {
    controller: function () {
      return runCtrl(opts, trans);
    },
    view: runView,
  };
};

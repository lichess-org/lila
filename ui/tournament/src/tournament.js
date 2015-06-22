var status = require('game').status;

module.exports = {
  myCurrentPairing: function(ctrl) {
    if (!ctrl.userId) return null;
    return ctrl.data.pairings.filter(function(p) {
      return p.s === 0 && (
        p.u[0].toLowerCase() === ctrl.userId || p.u[1].toLowerCase() === ctrl.userId
      );
    })[0];
  }
};

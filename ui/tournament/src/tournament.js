var status = require('game').status;

module.exports = {
  createdByMe: function(ctrl) {
    return ctrl.userId && ctrl.userId === ctrl.data.createdBy;
  },
  containsMe: function(ctrl) {
    return ctrl.userId && ctrl.data.players.filter(function(p) {
      return p.name.toLowerCase() === ctrl.userId && !p.withdraw;
    }).length > 0;
  },
  myCurrentPairing: function(ctrl) {
    if (!ctrl.userId) return null;
    return ctrl.data.pairings.filter(function(p) {
      return p.status < status.ids.mate && (
        p.user1[0] === ctrl.userId || p.user2[0] === ctrl.userId
      );
    })[0];
  }
};

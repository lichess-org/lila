function isIn(ctrl) {
  return ctrl.data.me && !ctrl.data.me.withdraw;
}

module.exports = {
  myCurrentGameId: function(ctrl) {
    if (!ctrl.userId) return null;
    var pairing = ctrl.data.pairings.filter(function(p) {
      return p.s === 0 && (
        p.u[0].toLowerCase() === ctrl.userId || p.u[1].toLowerCase() === ctrl.userId
      );
    })[0]
    return pairing ? pairing.id : null;
  },
  willBePaired: function(ctrl) {
    return isIn(ctrl) && !ctrl.data.pairingsClosed;
  },
  isIn: isIn
};

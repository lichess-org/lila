var m = require('mithril');

function memberActivity(onIdle) {
  var timeout;
  var schedule = function() {
    if (timeout) clearTimeout(timeout);
    timeout = setTimeout(onIdle, 100);
  };
  schedule();

  return {
    renew: function() {
      schedule();
    }
  };
};

module.exports = {
  ctrl: function(members, myId, ownerId) {

    var vm = {
      confing: null // which user is being configured by us
    };
    var active = {}; // recently active contributors

    var owner = function() {
      return members[ownerId];
    };

    var myMember = function() {
      return myId ? members[myId] : null;
    };

    return {
      vm: vm,
      myId: myId,
      set: function(ms) {
        members = ms;
      },
      setActive: function(id) {
        if (active[id]) active[id].renew();
        else active[id] = memberActivity(function() {
          delete(active[id]);
          m.redraw();
        });
        m.redraw();
      },
      isActive: function(id) {
        return !!active[id];
      },
      owner: owner,
      isOwner: function() {
        return myId === ownerId;
      },
      canContribute: function() {
        return myMember() && myMember().role === 'w';
      },
      ordered: function() {
        return Object.keys(members).map(function(id) {
          return members[id];
        }).sort(function(a, b) {
          return a.addedAt > b.addedAt;
        });
      },
    };
  }
};

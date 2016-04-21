var m = require('mithril');

module.exports = {
  init: function(data, ctrl) {

    var send = ctrl.socket.send;
    var userId = ctrl.userId;
    var ownage = userId === data.ownerId;

    function myMember() {
      return userId ? data.members[userId] : null;
    }

    function owner() {
      return data.members[data.ownerId];
    }

    function meOrOwner() {
      return myMember() || owner();
    }

    var vm = {
      position: meOrOwner().position,
      follow: null, // which user is being followed by us
      memberConfig: null // which user is being configured by us
    };

    function orderedMembers() {
      return Object.keys(data.members).map(function(id) {
        return data.members[id];
      }).sort(function(a, b) {
        return a.addedAt > b.addedAt;
      });
    }

    function addChapterId(data) {
      data.chapterId = vm.position.chapterId;
      return data;
    }

    function updateMember(id, f) {
      data.members[id] && f(data.members[id]);
    }

    function checkFollow() {
      if (vm.follow && (!data.members[vm.follow] || data.members[vm.follow].role !== 'w'))
        follow(null);
    }

    function follow(id) {
      if (id === vm.follow || id === ctrl.userId) vm.follow = null;
      else vm.follow = id;
      if (vm.follow) ctrl.userJump(data.members[vm.follow].position.path);
      checkFollow();
    }
    follow(data.ownerId);

    function invite(username) {
      if (ownage) send("invite", username);
    }

    return {
      data: data,
      vm: vm,
      userId: userId,
      position: function() {
        return vm.position;
      },
      setPath: function(path) {
        if (vm.follow && data.members[vm.follow].position.path !== path) follow(null);
        userId && send("setPos", addChapterId({
          path: path
        }));
      },
      deleteVariation: function(path) {
        send("deleteVariation", addChapterId({
          path: path
        }));
      },
      promoteVariation: function(path) {
        send("promoteVariation", addChapterId({
          path: path
        }));
      },
      orderedMembers: orderedMembers,
      follow: follow,
      setRole: function(userId, role) {
        send("setRole", {
          userId: userId,
          role: role
        });
        setTimeout(function() {
          vm.memberConfig = null;
          m.redraw();
        }, 500);
      },
      invite: function(username) {
        send("invite", username);
      },
      kick: function(userId) {
        send("kick", userId);
      },
      socketHandlers: {
        mpos: function(d) {
          updateMember(d.u, function(m) {
            m.position = d.p;
            if (vm.follow === d.u) ctrl.userJump(m.position.path);
          });
        },
        addNode: function(d) {
          if (d.p.chapterId !== vm.position.chapterId) return;
          ctrl.tree.addNode(d.n, d.p.path);
          ctrl.jump(ctrl.vm.path);
          m.redraw();
        },
        delNode: function(d) {
          if (d.p.chapterId !== vm.position.chapterId) return;
          ctrl.tree.deleteNodeAt(d.p.path);
          ctrl.jump(ctrl.vm.path);
          m.redraw();
        },
        reloadMembers: function(d) {
          data.members = d;
          checkFollow();
          m.redraw();
        }
      }
    };
  }
};

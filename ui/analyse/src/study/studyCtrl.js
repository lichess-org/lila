var m = require('mithril');
var partial = require('chessground').util.partial;

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

    function updateAutoShapes() {
      if (!vm.follow) ctrl.chessground.setAutoShapes([]);
      else if (samePosition(vm.position, data.members[vm.follow].position)) {
        ctrl.chessground.setAutoShapes(data.members[vm.follow].shapes);
      }
    }

    function follow(id) {
      if (id === vm.follow || id === ctrl.userId) vm.follow = null;
      else vm.follow = id;
      if (vm.follow) ctrl.userJump(data.members[vm.follow].position.path);
      checkFollow();
      updateAutoShapes();
    }
    if (ownage) ctrl.userJump(owner().position.path);
    else follow(data.ownerId);

    function invite(username) {
      if (ownage) send("invite", username);
    }

    function samePosition(p1, p2) {
      return p1.chapterId === p2.chapterId && p1.path === p2.path;
    }

    ctrl.chessground.set({
      drawable: {
        onChange: partial(send, "shapes")
      }
    });

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
        }, 400);
      },
      invite: function(username) {
        send("invite", username);
      },
      kick: function(userId) {
        send("kick", userId);
        vm.memberConfig = null;
      },
      onShowGround: function() {
        updateAutoShapes();
      },
      socketHandlers: {
        mpos: function(d) {
          updateMember(d.u, function(member) {
            member.position = d.p;
            if (vm.follow === d.u) {
              ctrl.userJump(member.position.path);
              m.redraw();
            }
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
          updateAutoShapes();
          m.redraw();
        },
        reloadMemberShapes: function(d) {
          data.members[d.u].shapes = d.shapes;
          updateAutoShapes();
          m.redraw();
        }
      }
    };
  }
};

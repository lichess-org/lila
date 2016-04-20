var m = require('mithril');

module.exports = {
  init: function(data, ctrl) {

    var send = ctrl.socket.send;
    var userId = ctrl.userId;

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
      follow: null
    };

    function joiners() {
      var members = [];
      for (var id in data.members)
        if (id !== data.ownerId)
          members.push(data.members[id]);
      return members;
    }

    function addChapterId(data) {
      data.chapterId = vm.position.chapterId;
      return data;
    }

    function updateMember(id, f) {
      data.members[id] && f(data.members[id]);
    }

    function follow(id) {
      if (id === vm.follow || id === ctrl.userId) vm.follow = null;
      else vm.follow = id;
      if (vm.follow) ctrl.userJump(data.members[vm.follow].position.path);
    }
    follow(data.ownerId);

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
      orderedMembers: function() {
        return [owner()].concat(joiners());
      },
      follow: follow,
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
        }
      }
    };
  }
};

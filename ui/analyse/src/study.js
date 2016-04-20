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
      position: meOrOwner().position
    };

    function addChapterId(data) {
      data.chapterId = vm.position.chapterId;
      return data;
    }

    function updateMember(id, f) {
      data.members[id] && f(data.members[id]);
    }

    return {
      position: function() {
        return vm.position;
      },
      setPath: function(path) {
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
      socketHandlers: {
        mpos: function(d) {
          updateMember(d.u, function(m) {
            m.position = d.p;
            ctrl.userJump(m.position.path);
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

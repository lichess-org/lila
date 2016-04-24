var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var memberCtrl = require('./studyMembers').ctrl;

module.exports = {
  // data.position.path represents the server state
  // ctrl.vm.path is the client state
  init: function(data, ctrl) {

    var send = ctrl.socket.send;

    var members = memberCtrl(data.members, ctrl.userId, data.ownerId);

    var sri = lichess.StrongSocket.sri;

    function addChapterId(req) {
      req.chapterId = data.position.chapterId;
      return req;
    }

    function updateShapes() {
      var shapes = ctrl.vm.path === data.position.path ? data.shapes : [];
      ctrl.chessground.setShapes(shapes);
    }
    ctrl.userJump(data.position.path);
    updateShapes();

    function samePosition(p1, p2) {
      return p1.chapterId === p2.chapterId && p1.path === p2.path;
    }

    ctrl.chessground.set({
      drawable: {
        onChange: function(shapes) {
          if (members.canContribute()) send("shapes", shapes);
        }
      }
    });

    return {
      data: data,
      members: members,
      position: function() {
        return data.position;
      },
      setPath: throttle(300, false, function(path) {
        if (members.canContribute() && path !== data.position.path) {
          data.shapes = [];
          send("setPath", addChapterId({
            path: path
          }));
        }
      }),
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
      setRole: function(id, role) {
        send("setRole", {
          userId: id,
          role: role
        });
        setTimeout(function() {
          members.vm.confing = null;
          m.redraw();
        }, 400);
      },
      invite: function(username) {
        send("invite", username);
      },
      kick: function(id) {
        send("kick", id);
        vm.memberConfig = null;
      },
      onShowGround: function() {
        updateShapes();
      },
      socketHandlers: {
        path: function(d) {
          var position = d.p, who = d.w;
          if (position.chapterId !== data.position.chapterId) return;
          data.position.path = position.path;
          members.setActive(who.u);
          if (who.s === sri) return;
          if (!ctrl.tree.pathExists(position.path)) lichess.reload();
          data.position.path = position.path;
          data.shapes = [];
          ctrl.userJump(position.path);
          m.redraw();
        },
        addNode: function(d) {
          var position = d.p,
            node = d.n,
            who = d.w;
          members.setActive(who.u);
          if (who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(d.p.path)) lichess.reload();
          ctrl.tree.addNode(node, position.path);
          data.position.path = position.path + node.id;
          ctrl.jump(data.position.path);
          m.redraw();
        },
        delNode: function(d) {
          var position = d.p,
            byId = d.u,
            who = d.w;
          members.setActive(who.u);
          if (who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(d.p.path)) lichess.reload();
          ctrl.tree.deleteNodeAt(position.path);
          ctrl.jump(ctrl.vm.path);
          m.redraw();
        },
        reload: function() {
          lichess.reload();
        },
        members: function(d) {
          members.set(d);
          m.redraw();
        },
        shapes: function(d) {
          data.shapes = d;
          updateShapes();
          m.redraw();
        }
      }
    };
  }
};

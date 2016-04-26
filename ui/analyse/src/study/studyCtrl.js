var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var storedProp = require('../util').storedProp;
var memberCtrl = require('./studyMembers').ctrl;
var chapterCtrl = require('./studyChapters').ctrl;
var xhr = require('./studyXhr');

module.exports = {
  // data.position.path represents the server state
  // ctrl.vm.path is the client state
  init: function(data, ctrl) {

    var send = ctrl.socket.send;

    var members = memberCtrl(data.members, ctrl.userId, data.ownerId, send);
    var chapters = chapterCtrl(data.chapters, send);

    var sri = lichess.StrongSocket.sri;

    var vm = {
      loading: false,
      tab: storedProp('study.tab', 'members'),
      behind: false // false if syncing, else incremental number of missed event
    };

    var contributing = function() {
      return members.canContribute() && vm.behind === false;
    };

    var contribute = function(t, d) {
      if (contributing()) send(t, d);
    };

    var addChapterId = function(req) {
      req.chapterId = data.position.chapterId;
      return req;
    }
    ctrl.userJump(data.position.path);

    var samePosition = function(p1, p2) {
      return p1.chapterId === p2.chapterId && p1.path === p2.path;
    }

    var onReload = function(d) {
      var s = d.study;
      data.position = s.position;
      members.set(s.members);
      chapters.set(s.chapters);
      ctrl.reloadData(d.analysis);
      ctrl.chessground.set({
        orientation: d.analysis.orientation
      });
      vm.loading = false;
      m.redraw();
    };

    var xhrReload = function() {
      vm.loading = true;
      return xhr.reload(data.id).then(onReload);
    };

    var activity = function(userId) {
      members.setActive(userId);
      vm.behind !== false && vm.behind++;
    };

    ctrl.chessground.set({
      drawable: {
        onChange: function(shapes) {
          if (members.canContribute()) {
            ctrl.tree.setShapes(shapes, ctrl.vm.path);
            contribute("shapes", addChapterId({
              path: ctrl.vm.path,
              shapes: shapes
            }));
          }
        }
      }
    });

    return {
      data: data,
      members: members,
      chapters: chapters,
      vm: vm,
      position: function() {
        return data.position;
      },
      currentChapter: function() {
        return chapters.get(data.position.chapterId);
      },
      setPath: throttle(300, false, function(path) {
        contribute("setPath", addChapterId({
          path: path
        }));
      }),
      deleteVariation: function(path) {
        contribute("deleteVariation", addChapterId({
          path: path
        }));
      },
      promoteVariation: function(path) {
        contribute("promoteVariation", addChapterId({
          path: path
        }));
      },
      setChapter: function(id) {
        if (id === data.position.chapterId) return;
        contribute("setChapter", id);
        vm.loading = true;
      },
      toggleSync: function() {
        if (vm.behind !== false) {
          xhrReload().then(function() {
            vm.behind = false;
          });
        } else vm.behind = 0;
      },
      anaMoveConfig: function(req) {
        if (contributing()) req.chapterId = data.position.chapterId;
      },
      socketHandlers: {
        path: function(d) {
          var position = d.p,
            who = d.w;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(position.path)) xhrReload();
          data.position.path = position.path;
          activity(who.u);
          if (who.s === sri) return;
          data.position.path = position.path;
          ctrl.userJump(position.path);
          m.redraw();
        },
        addNode: function(d) {
          var position = d.p,
            node = d.n,
            who = d.w;
          if (position.chapterId !== data.position.chapterId) return;
          activity(who.u);
          if (who.s === sri) {
            data.position.path = position.path + node.id;
            return;
          }
          var newPath = ctrl.tree.addNode(node, position.path);
          ctrl.tree.addDests(d.d, newPath, d.o);
          if (!newPath) xhrReload();
          data.position.path = newPath;
          ctrl.jump(data.position.path);
          m.redraw();
        },
        delNode: function(d) {
          var position = d.p,
            byId = d.u,
            who = d.w;
          activity(who.u);
          if (who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(d.p.path)) xhrReload();
          ctrl.tree.deleteNodeAt(position.path);
          ctrl.jump(ctrl.vm.path);
          m.redraw();
        },
        reload: xhrReload,
        members: function(d) {
          members.set(d);
          m.redraw();
        },
        chapters: function(d) {
          chapters.set(d);
          m.redraw();
        },
        shapes: function(d) {
          var position = d.p,
            who = d.w;
          activity(who.u);
          if (who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          ctrl.chessground.setShapes(d.s);
          m.redraw();
        }
      }
    };
  }
};

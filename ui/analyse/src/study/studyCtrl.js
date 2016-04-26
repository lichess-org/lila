var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var storedProp = require('../util').storedProp;
var memberCtrl = require('./studyMembers').ctrl;
var chapterCtrl = require('./studyChapters').ctrl;
var xhr = require('./studyXhr');

$.fn.scrollTo = function(target, options, callback) {
  if (typeof options == 'function' && arguments.length == 2) {
    callback = options;
    options = target;
  }
  var settings = $.extend({
    scrollTarget: target,
    offsetTop: 50,
    duration: 500,
    easing: 'swing'
  }, options);
  return this.each(function() {
    var scrollPane = $(this);
    var scrollTarget = (typeof settings.scrollTarget == "number") ? settings.scrollTarget : $(settings.scrollTarget);
    var scrollY = (typeof scrollTarget == "number") ? scrollTarget : scrollTarget.offset().top + scrollPane.scrollTop() - parseInt(settings.offsetTop);
    scrollPane.animate({
      scrollTop: scrollY
    }, parseInt(settings.duration), settings.easing, function() {
      if (typeof callback == 'function') {
        callback.call(this);
      }
    });
  });
};

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
      tab: storedProp('study.tab', 'members')
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
      data.shapes = s.shapes;
      members.set(s.members);
      chapters.set(s.chapters);
      ctrl.reloadData(d.analysis);
      ctrl.chessground.set({
        orientation: d.analysis.orientation
      });
      vm.loading = false;
    };

    var xhrReload = function() {
      vm.loading = true;
      xhr.reload(data.id).then(onReload);
    };

    ctrl.chessground.set({
      drawable: {
        onChange: function(shapes) {
          if (members.canContribute()) {
            ctrl.tree.setShapes(shapes, ctrl.vm.path);
            send("shapes", addChapterId({
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
      setChapter: function(id) {
        send("setChapter", id);
        vm.loading = true;
      },
      setTab: function(tab) {
        vm.tab(tab);
        m.redraw.strategy("all");
      },
      socketHandlers: {
        path: function(d) {
          var position = d.p,
            who = d.w;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(position.path)) xhrReload();
          data.position.path = position.path;
          members.setActive(who.u);
          if (who.s === sri) return;
          data.position.path = position.path;
          data.shapes = [];
          ctrl.userJump(position.path);
          m.redraw();
        },
        addNode: function(d) {
          var position = d.p,
            node = d.n,
            who = d.w;
          if (position.chapterId !== data.position.chapterId) return;
          members.setActive(who.u);
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
          members.setActive(who.u);
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
          members.setActive(who.u);
          if (who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          ctrl.chessground.setShapes(d.s);
          m.redraw();
        }
      }
    };
  }
};

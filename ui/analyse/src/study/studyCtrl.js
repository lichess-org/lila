var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var storedProp = require('../util').storedProp;
var memberCtrl = require('./studyMembers').ctrl;
var chapterCtrl = require('./studyChapters').ctrl;
var commentFormCtrl = require('./commentForm').ctrl;
var glyphFormCtrl = require('./studyGlyph').ctrl;
var studyFormCtrl = require('./studyForm').ctrl;
var tour = require('./studyTour');
var xhr = require('./studyXhr');

module.exports = {
  // data.position.path represents the server state
  // ctrl.vm.path is the client state
  init: function(data, chat, ctrl) {

    var send = ctrl.socket.send;

    var sri = lichess.StrongSocket.sri;

    var vm = {
      loading: false,
      tab: storedProp('study.tab', 'members'),
      behind: false, // false if syncing, else incremental number of missed event
      catchingUp: false, // was behind, is syncing back
      chapterId: null // only useful when not synchronized
    };

    var form = studyFormCtrl(function(data, isNew) {
      send("editStudy", data);
      if (isNew) chapters.form.openInitial();
    }, function() {
      return data;
    });
    var members = memberCtrl(data.members, ctrl.userId, data.ownerId, send, partial(vm.tab, 'members'));
    var chapters = chapterCtrl(data.chapters, send, partial(vm.tab, 'chapters'), ctrl);

    var currentChapterId = function() {
      return vm.chapterId || data.position.chapterId;
    }

    var contributing = function() {
      return members.canContribute() && vm.behind === false;
    };

    var contribute = function(t, d) {
      if (contributing()) {
        send(t, d);
        return true;
      } else if (!members.canContribute()) vm.behind = 0;
    };

    var commentForm = commentFormCtrl(ctrl);
    var glyphForm = glyphFormCtrl(ctrl);

    var addChapterId = function(req) {
      req.chapterId = data.position.chapterId;
      return req;
    }
    ctrl.userJump(data.position.path);

    var configureAnalysis = function() {
      chat.writeable(!!members.myMember());
      if (!data.features.computer) ctrl.ceval.enabled(false);
      ctrl.ceval.allowed(data.features.computer);
      if (!data.features.explorer) ctrl.explorer.disable();
      ctrl.explorer.allowed(data.features.explorer);
    };
    configureAnalysis();

    var onReload = function(d) {
      var s = d.study;
      if (data.visibility === 'public' && s.visibility === 'private' && !members.myMember())
        return lichess.reload();
      if (s.position !== data.position) commentForm.close();
      data.position = s.position;
      data.name = document.title = s.name;
      data.visibility = s.visibility;
      data.settings = s.settings;
      data.features = s.features;
      members.dict(s.members);
      chapters.list(s.chapters);
      configureAnalysis();
      ctrl.reloadData(d.analysis);
      ctrl.chessground.set({
        orientation: d.analysis.orientation
      });
      vm.loading = false;
      if (vm.behind === false || vm.catchingUp) ctrl.userJump(data.position.path);
      else ctrl.jumpToLast();
      vm.catchingUp = false;
      m.redraw.strategy("all");
      m.redraw();
    };

    var xhrReload = function() {
      vm.loading = true;
      return xhr.reload(data.id, vm.chapterId).then(onReload);
    };

    var activity = function(userId) {
      members.setActive(userId);
      if (vm.behind !== false && vm.behind < 99) {
        vm.behind++;
        if (vm.behind === 1) tour.offline();
      }
    };

    var onSetPath = throttle(300, false, function(path) {
      if (path !== data.position.path) contribute("setPath", addChapterId({
        path: path
      }));
    });

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
      form: form,
      members: members,
      chapters: chapters,
      commentForm: commentForm,
      glyphForm: glyphForm,
      vm: vm,
      position: function() {
        return data.position;
      },
      currentChapter: function() {
        return chapters.get(currentChapterId());
      },
      withPosition: function(obj) {
        obj.chapterId = currentChapterId();
        obj.path = ctrl.vm.path;
        return obj;
      },
      setPath: function(path, node) {
        onSetPath(path);
        setTimeout(partial(commentForm.onSetPath, path, node), 100);
      },
      deleteNode: function(path) {
        contribute("deleteNode", addChapterId({
          path: path,
          jumpTo: ctrl.vm.path
        }));
      },
      promoteNode: function(path) {
        contribute("promoteNode", addChapterId({
          path: path
        }));
      },
      setChapter: function(id) {
        if (id === currentChapterId()) return;
        if (!contribute("setChapter", id)) {
          vm.chapterId = id;
          xhrReload();
        }
        vm.loading = true;
      },
      toggleSync: function() {
        if (vm.behind !== false) {
          vm.chapterId = null;
          vm.catchingUp = true;
          xhrReload().then(function() {
            vm.behind = false;
            m.redraw();
          });
        } else {
          vm.behind = 0;
          vm.chapterId = currentChapterId();
        }
      },
      anaMoveConfig: function(req) {
        if (contributing()) addChapterId(req);
      },
      contribute: contribute,
      userJump: ctrl.userJump,
      socketHandlers: {
        path: function(d) {
          var position = d.p,
            who = d.w;
          who && activity(who.u);
          if (vm.behind !== false) return;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(position.path)) xhrReload();
          data.position.path = position.path;
          if (who && who.s === sri) return;
          data.position.path = position.path;
          ctrl.userJump(position.path);
          m.redraw();
        },
        addNode: function(d) {
          var position = d.p,
            node = d.n,
            who = d.w;
          if (position.chapterId !== currentChapterId()) return;
          who && activity(who.u);
          if (who && who.s === sri) {
            data.position.path = position.path + node.id;
            return;
          }
          var newPath = ctrl.tree.addNode(node, position.path);
          ctrl.tree.addDests(d.d, newPath, d.o);
          if (!newPath) xhrReload();
          data.position.path = newPath;
          if (vm.behind === false) ctrl.jump(data.position.path);
          m.redraw();
        },
        deleteNode: function(d) {
          var position = d.p,
            who = d.w;
          who && activity(who.u);
          if (vm.behind !== false) return;
          if (who && who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
          ctrl.tree.deleteNodeAt(position.path);
          ctrl.jump(ctrl.vm.path);
        },
        promoteNode: function(d) {
          var position = d.p,
            who = d.w;
          who && activity(who.u);
          if (vm.behind !== false) return;
          if (who && who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
          ctrl.tree.promoteNodeAt(position.path);
          ctrl.jump(ctrl.vm.path);
        },
        reload: xhrReload,
        changeChapter: function(d) {
          d.w && activity(d.w.u);
          if (vm.behind === false) xhrReload();
        },
        members: function(d) {
          members.update(d);
          configureAnalysis();
          m.redraw();
        },
        chapters: function(d) {
          chapters.list(d);
          m.redraw();
        },
        shapes: function(d) {
          var position = d.p,
            who = d.w;
          who && activity(who.u);
          if (vm.behind !== false) return;
          if (who && who.s === sri) return;
          if (position.chapterId !== data.position.chapterId) return;
          ctrl.chessground.setShapes(d.s);
          m.redraw();
        },
        setComment: function(d) {
          var position = d.p,
            who = d.w;
          who && activity(who.u);
          if (who && who.s === sri) commentForm.dirty(false);
          if (vm.behind !== false) return;
          if (position.chapterId !== data.position.chapterId) return;
          ctrl.tree.setCommentAt(d.c, position.path);
          m.redraw();
        },
        deleteComment: function(d) {
          var position = d.p,
            who = d.w;
          who && activity(who.u);
          if (vm.behind !== false) return;
          if (position.chapterId !== data.position.chapterId) return;
          ctrl.tree.deleteCommentAt(d.id, position.path);
          m.redraw();
        },
        glyphs: function(d) {
          var position = d.p,
            who = d.w;
          who && activity(who.u);
          if (who && who.s === sri) glyphForm.dirty(false);
          if (vm.behind !== false) return;
          if (position.chapterId !== data.position.chapterId) return;
          ctrl.tree.setGlyphsAt(d.g, position.path);
          m.redraw();
        },
        following_onlines: members.inviteForm.setCandidates,
        following_leaves: members.inviteForm.delCandidate,
        following_enters: members.inviteForm.addCandidate
      }
    };
  }
};

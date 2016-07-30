var m = require('mithril');
var partial = require('chessground').util.partial;
var throttle = require('../util').throttle;
var storedProp = require('../util').storedProp;
var memberCtrl = require('./studyMembers').ctrl;
var chapterCtrl = require('./studyChapters').ctrl;
var commentFormCtrl = require('./commentForm').ctrl;
var glyphFormCtrl = require('./studyGlyph').ctrl;
var studyFormCtrl = require('./studyForm').ctrl;
var notifCtrl = require('./notif').ctrl;
var tours = require('./studyTour');
var xhr = require('./studyXhr');
var concealFeedback = require('./concealFeedback');

module.exports = {
  // data.position.path represents the server state
  // ctrl.vm.path is the client state
  init: function(data, ctrl) {

    var send = ctrl.socket.send;

    var sri = lichess.StrongSocket.sri;

    var vm = {
      loading: false,
      tab: m.prop(data.chapters.length > 1 ? 'chapters' : 'members'),
      behind: false, // false if syncing, else incremental number of missed event
      catchingUp: false, // was behind, is syncing back
      chapterId: null // only useful when not synchronized
    };

    var notif = notifCtrl();
    var form = studyFormCtrl(function(data, isNew) {
      send("editStudy", data);
      if (isNew) chapters.newForm.openInitial();
    }, function() {
      return data;
    });
    var startTour = function() {
      tours.study(ctrl);
    };
    var members = memberCtrl(data.members, ctrl.userId, data.ownerId, send, partial(vm.tab, 'members'), startTour, notif);
    var chapters = chapterCtrl(data.chapters, send, partial(vm.tab, 'chapters'), partial(xhr.chapterConfig, data.id), ctrl);

    var currentChapterId = function() {
      return vm.chapterId || data.position.chapterId;
    }
    var currentChapter = function() {
      return chapters.get(currentChapterId());
    };
    var isChapterOwner = function() {
      return ctrl.userId === data.chapter.ownerId;
    };

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
      lichess.pubsub.emit('chat.writeable')(!!members.myMember());
      if (!data.chapter.features.computer) ctrl.ceval.enabled(false);
      ctrl.ceval.allowed(data.chapter.features.computer);
      if (!data.chapter.features.explorer) ctrl.explorer.disable();
      ctrl.explorer.allowed(data.chapter.features.explorer);
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
      data.visibility = s.visibility;
      data.views = s.views;
      data.chapter = s.chapter;
      data.likes = s.likes;
      data.liked = s.liked;
      members.dict(s.members);
      chapters.list(s.chapters);
      configureAnalysis();
      ctrl.reloadData(d.analysis);
      ctrl.chessground.set({
        orientation: d.analysis.orientation
      });
      vm.loading = false;
      if (vm.behind === false || vm.catchingUp) ctrl.userJump(data.position.path);
      else ctrl.userJump('');
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
        if (vm.behind === 1 && lichess.once('study-offline')) tours.offline();
      }
    };

    var onSetPath = throttle(300, false, function(path) {
      if (path !== data.position.path) contribute("setPath", addChapterId({
        path: path
      }));
    });

    if (members.canContribute()) form.openIfNew();

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
      notif: notif,
      commentForm: commentForm,
      glyphForm: glyphForm,
      vm: vm,
      toggleLike: function(v) {
        send("like", {
          liked: !data.liked
        });
      },
      position: function() {
        return data.position;
      },
      currentChapter: currentChapter,
      isChapterOwner: isChapterOwner,
      canJumpTo: function(path) {
        return data.chapter.conceal === null || isChapterOwner() || (
          ctrl.tree.lastMainlineNode(path).ply <= data.chapter.conceal
        );
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
      startTour: startTour,
      userJump: ctrl.userJump,
      currentNode: function() {
        return ctrl.vm.node;
      },
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
            concealFeedback(ctrl, position.path, node);
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
          ctrl.tree.setShapes(d.s, ctrl.vm.path);
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
        conceal: function(d) {
          var position = d.p;
          if (position.chapterId !== data.position.chapterId) return;
          data.chapter.conceal = d.ply;
          m.redraw();
        },
        liking: function(d) {
          data.likes = d.l.likes;
          if (d.w && d.w.s === sri) data.liked = d.l.me;
          m.redraw();
        },
        following_onlines: members.inviteForm.setFollowings,
        following_leaves: members.inviteForm.delFollowing,
        following_enters: members.inviteForm.addFollowing,
        crowd: function(d) {
          members.setSpectators(d.users);
        }
      }
    };
  }
};

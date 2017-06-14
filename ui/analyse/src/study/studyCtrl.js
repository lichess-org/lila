var m = require('mithril');
var throttle = require('common').throttle;
var memberCtrl = require('./studyMembers').ctrl;
var chapterCtrl = require('./studyChapters').ctrl;
var practiceCtrl = require('./practice/studyPracticeCtrl');
var commentFormCtrl = require('./commentForm').ctrl;
var glyphFormCtrl = require('./studyGlyph').ctrl;
var studyFormCtrl = require('./studyForm').ctrl;
var notifCtrl = require('./notif').ctrl;
var shareCtrl = require('./studyShare').ctrl;
var tagsCtrl = require('./studyTags').ctrl;
var tours = require('./studyTour');
var xhr = require('./studyXhr');

// data.position.path represents the server state
// ctrl.vm.path is the client state
module.exports = function(data, ctrl, tagTypes, practiceData) {

  var send = ctrl.socket.send;

  var sri = lichess.StrongSocket && lichess.StrongSocket.sri;

  var vm = (function() {
    var isManualChapter = data.chapter.id !== data.position.chapterId;
    var sticked = data.features.sticky && !ctrl.vm.initialPath && !isManualChapter && !practiceData;
    return {
      loading: false,
      nextChapterId: false,
      tab: m.prop(data.chapters.length > 1 ? 'chapters' : 'members'),
      chapterId: sticked ? data.position.chapterId : data.chapter.id,
      // path is at ctrl.vm.path
      mode: {
        sticky: sticked,
        write: true
      }
    };
  })();

  var notif = notifCtrl();
  var form = studyFormCtrl(function(d, isNew) {
    send("editStudy", d);
    if (isNew && data.chapter.setup.variant.key === 'standard' && ctrl.vm.mainline.length === 1 && !data.chapter.setup.fromFen)
      chapters.newForm.openInitial();
  }, function() {
    return data;
  });
  var startTour = function() {
    tours.study(ctrl);
  };
  var members = memberCtrl({
    initDict: data.members,
    myId: practice ? null : ctrl.userId,
    ownerId: data.ownerId,
    send: send,
    setTab: lichess.partial(vm.tab, 'members'),
    startTour: startTour,
    notif: notif
  });

  var chapters = chapterCtrl(data.chapters, send, lichess.partial(vm.tab, 'chapters'), lichess.partial(xhr.chapterConfig, data.id), ctrl);

  var currentChapter = function() {
    return chapters.get(vm.chapterId);
  };
  var isChapterOwner = function() {
    return ctrl.userId === data.chapter.ownerId;
  };

  var makeChange = function(t, d) {
    if (vm.mode.write) {
      send(t, d);
      return true;
    } else vm.mode.sticky = false;
  };

  var commentForm = commentFormCtrl(ctrl);
  var glyphForm = glyphFormCtrl(ctrl);
  var tags = tagsCtrl(ctrl, function() {
    return data.chapter;
  }, members, tagTypes);

  var addChapterId = function(req) {
    req.ch = data.position.chapterId;
    return req;
  }
  if (vm.mode.sticky) ctrl.userJump(data.position.path);

  var configureAnalysis = function() {
    if (ctrl.embed) return;
    var canContribute = members.canContribute();
    // unwrite if member lost priviledges
    vm.mode.write = vm.mode.write && canContribute;
    // unstick if study becomes non-sticky
    vm.mode.sticky = vm.mode.sticky && data.features.sticky;
    lichess.pubsub.emit('chat.writeable')(data.features.chat);
    lichess.pubsub.emit('chat.permissions')({local: canContribute});
    var computer = data.chapter.features.computer || data.chapter.practice;
    if (!computer) ctrl.getCeval().enabled(false);
    ctrl.getCeval().allowed(computer);
    if (!data.chapter.features.explorer) ctrl.explorer.disable();
    ctrl.explorer.allowed(data.chapter.features.explorer);
  };
  configureAnalysis();

  var configurePractice = function() {
    if (!data.chapter.practice && ctrl.practice) ctrl.togglePractice();
    if (data.chapter.practice) ctrl.restartPractice();
    if (practice) practice.onReload();
  };

  var onReload = function(d) {
    var s = d.study;
    if (vm.mode.sticky && s.position !== data.position) commentForm.close();
    ['position', 'name', 'visibility', 'features', 'settings', 'chapter', 'likes', 'liked'].forEach(function(key) {
      data[key] = s[key];
    });
    document.title = data.name;
    members.dict(s.members);
    chapters.list(s.chapters);
    ctrl.unflip();
    ctrl.reloadData(d.analysis);
    configureAnalysis();
    vm.loading = false;

    ctrl.chessground = undefined; // don't apply changes to old cg; wait for new cg

    if (vm.mode.sticky) {
      vm.chapterId = data.position.chapterId;
      ctrl.userJump(data.position.path);
    }

    configurePractice();

    m.redraw.strategy("all"); // create a new cg
    m.redraw();
    ctrl.startCeval();
  };

  var xhrReload = function() {
    vm.loading = true;
    return xhr.reload(
      practice ? 'practice/load' : 'study',
      data.id,
      vm.mode.sticky ? null : vm.chapterId
    ).then(onReload, lichess.reload);
  };

  var onSetPath = throttle(300, false, function(path) {
    if (vm.mode.sticky && path !== data.position.path) makeChange("setPath", addChapterId({
      path: path
    }));
  });

  if (members.canContribute()) form.openIfNew();

  var currentNode = function() {
    return ctrl.vm.node;
  };

  var share = shareCtrl(data, currentChapter, currentNode);

  var practice = practiceData && practiceCtrl(ctrl, data, practiceData);

  var mutateCgConfig = function(config) {
    config.drawable.onChange = function(shapes) {
      if (vm.mode.write) {
        ctrl.tree.setShapes(shapes, ctrl.vm.path);
        makeChange("shapes", addChapterId({
          path: ctrl.vm.path,
          shapes: shapes
        }));
      }
    };
  }

  var wrongChapter = function(serverPosition) {
    if (serverPosition.chapterId !== vm.chapterId) {
      // sticky should really be on the same chapter
      if (vm.mode.sticky) xhrReload();
      return true;
    }
  };

  return {
    data: data,
    form: form,
    members: members,
    chapters: chapters,
    notif: notif,
    commentForm: commentForm,
    glyphForm: glyphForm,
    share: share,
    tags: tags,
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
    onJump: practice ? practice.onJump : function() {},
    withPosition: function(obj) {
      obj.ch = vm.chapterId;
      obj.path = ctrl.vm.path;
      return obj;
    },
    setPath: function(path, node) {
      onSetPath(path);
      setTimeout(lichess.partial(commentForm.onSetPath, path, node), 100);
    },
    deleteNode: function(path) {
      makeChange("deleteNode", addChapterId({
        path: path,
        jumpTo: ctrl.vm.path
      }));
    },
    promote: function(path, toMainline) {
      makeChange("promote", addChapterId({
        toMainline: toMainline,
        path: path
      }));
    },
    setChapter: function(id, force) {
      if (id === vm.chapterId && !force) return;
      if (!vm.mode.sticky || !makeChange("setChapter", id)) {
        vm.chapterId = id;
        xhrReload();
      }
      vm.loading = true;
      vm.nextChapterId = id;
      m.redraw();
    },
    toggleSticky: function() {
      vm.mode.sticky = !vm.mode.sticky && data.features.sticky;
      xhrReload();
    },
    toggleWrite: function() {
      vm.mode.write = !vm.mode.write && members.canContribute();
    },
    makeChange: makeChange,
    startTour: startTour,
    userJump: ctrl.userJump,
    currentNode: currentNode,
    practice: practice,
    mutateCgConfig: mutateCgConfig,
    socketHandlers: {
      path: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        // #TODO if not sticky, still follow if on the parent path (live games)
        if (!vm.mode.sticky) return;
        if (position.chapterId !== data.position.chapterId ||
          !ctrl.tree.pathExists(position.path)) {
          return xhrReload();
        }
        data.position.path = position.path;
        if (who && who.s === sri) return;
        ctrl.userJump(position.path);
        m.redraw();
      },
      addNode: function(d) {
        var position = d.p,
          node = d.n,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(position)) return;
        // node author already has the node
        if (who && who.s === sri) {
          data.position.path = position.path + node.id;
          return;
        }
        var newPath = ctrl.tree.addNode(node, position.path);
        ctrl.tree.addDests(d.d, newPath, d.o);
        if (!newPath) return xhrReload();
        data.position.path = newPath;
        if (vm.mode.sticky) ctrl.jump(data.position.path);
        m.redraw();
      },
      deleteNode: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(position)) return;
        // deleter already has it done
        if (who && who.s === sri) return;
        if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
        ctrl.tree.deleteNodeAt(position.path);
        if (vm.mode.sticky) ctrl.jump(ctrl.vm.path);
        m.redraw();
      },
      promote: function(d, toMainline) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(position)) return;
        if (who && who.s === sri) return;
        if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
        ctrl.tree.promoteAt(position.path, toMainline);
        if (vm.mode.sticky) ctrl.jump(ctrl.vm.path);
      },
      reload: xhrReload,
      changeChapter: function(d) {
        d.w && members.setActive(d.w.u);
        data.position = d.p;
        if (vm.mode.sticky) xhrReload();
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
        who && members.setActive(who.u);
        if (wrongChapter(position)) return;
        if (who && who.s === sri) return;
        ctrl.tree.setShapes(d.s, ctrl.vm.path);
        if (ctrl.vm.path === position.path && ctrl.chessground) ctrl.chessground.setShapes(d.s);
        m.redraw();
      },
      setComment: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(position)) return;
        if (who && who.s === sri) commentForm.dirty(false);
        ctrl.tree.setCommentAt(d.c, position.path);
        m.redraw();
      },
      setTags: function(d) {
        d.w && members.setActive(d.w.u);
        if (wrongChapter(position)) return;
        data.chapter.tags = d.tags;
        m.redraw();
      },
      deleteComment: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(position)) return;
        ctrl.tree.deleteCommentAt(d.id, position.path);
        m.redraw();
      },
      glyphs: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(position)) return;
        if (who && who.s === sri) glyphForm.dirty(false);
        ctrl.tree.setGlyphsAt(d.g, position.path);
        m.redraw();
      },
      conceal: function(d) {
        var position = d.p;
        if (wrongChapter(position)) return;
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
      },
      error: function(msg) {
        alert(msg);
      }
    }
  };
};

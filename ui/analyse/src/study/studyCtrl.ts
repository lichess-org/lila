import { throttle, prop } from 'common';
import AnalyseController from '../ctrl';
import { ctrl as memberCtrl } from './studyMembers';
import { ctrl as chapterCtrl } from './studyChapters';
import practiceCtrl from './practice/studyPracticeCtrl';
import { ctrl as commentFormCtrl } from './commentForm';
import { ctrl as glyphFormCtrl } from './studyGlyph';
import { ctrl as studyFormCtrl } from './studyForm';
import { ctrl as notifCtrl } from './notif';
import { ctrl as shareCtrl } from './studyShare';
import { ctrl as tagsCtrl } from './studyTags';
import * as tours from './studyTour';
import * as xhr from './studyXhr';
import { path as treePath } from 'tree';
import { TagTypes, StudyData } from './interfaces';

// data.position.path represents the server state
// ctrl.path is the client state
export default function(data: StudyData, ctrl: AnalyseController, tagTypes: TagTypes, practiceData) {

  const send = ctrl.socket.send;
  const redraw = ctrl.redraw;

  const sri: string = window.lichess.StrongSocket.sri;

  var vm = (function() {
    const isManualChapter = data.chapter.id !== data.position.chapterId;
    const sticked = data.features.sticky && !ctrl.initialPath && !isManualChapter && !practiceData;
    return {
      loading: false,
      nextChapterId: false,
      tab: prop(data.chapters.length > 1 ? 'chapters' : 'members'),
      chapterId: sticked ? data.position.chapterId : data.chapter.id,
      // path is at ctrl.vm.path
      mode: {
        sticky: sticked,
        write: true
      },
      behind: 0 // how many events missed because sync=off
    };
  })();

  const notif = notifCtrl(redraw);

  var form = studyFormCtrl(function(d, isNew) {
    send("editStudy", d);
    if (isNew && data.chapter.setup.variant.key === 'standard' && ctrl.mainline.length === 1 && !data.chapter.setup.fromFen)
      chapters.newForm.openInitial();
  }, function() {
    return data;
  });
  function startTour() {
    tours.study(ctrl);
  };
  var members = memberCtrl({
    initDict: data.members,
    myId: practiceData ? undefined : ctrl.opts.userId,
    ownerId: data.ownerId,
    send: send,
    setTab() { vm.tab('members') },
    startTour: startTour,
    notif: notif,
    onBecomingContributor: function() {
      vm.mode.write = true;
    },
    redraw: redraw
  });

  var chapters = chapterCtrl(
    data.chapters,
    send,
    () => vm.tab('chapters'),
    chapterId => xhr.chapterConfig(data.id, chapterId),
    ctrl);

  function currentChapter() {
    return chapters.get(vm.chapterId);
  };
  var isChapterOwner = function() {
    return ctrl.opts.userId === data.chapter.ownerId;
  };

  function makeChange(t: string, d: any): boolean {
    if (vm.mode.write) {
      send(t, d);
      return true;
    }
    return vm.mode.sticky = false;
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
    window.lichess.pubsub.emit('chat.writeable')(data.features.chat);
    window.lichess.pubsub.emit('chat.permissions')({local: canContribute});
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
    var prevPath = ctrl.path;
    var sameChapter = data.chapter.id === s.chapter.id;
    vm.mode.sticky = (vm.mode.sticky && s.features.sticky) || (!data.features.sticky && s.features.sticky);
    if (vm.mode.sticky) vm.behind = 0;
    if (vm.mode.sticky && s.position !== data.position) commentForm.close();
    ['position', 'name', 'visibility', 'features', 'settings', 'chapter', 'likes', 'liked'].forEach(function(key) {
      data[key] = s[key];
    });
    if (vm.mode.sticky && !data.features.sticky)
    document.title = data.name;
    members.dict(s.members);
    chapters.list(s.chapters);
    ctrl.flipped = false;

    var merge = !vm.mode.write && sameChapter;
    ctrl.reloadData(d.analysis, merge);
    configureAnalysis();
    vm.loading = false;

    // don't apply changes to old cg; wait for new cg
    // #TODO fixme
    // ctrl.chessground = undefined;

    if (vm.mode.sticky) {
      vm.chapterId = data.position.chapterId;
      ctrl.userJump(data.position.path);
    } else {
      // path could be gone
      var path = sameChapter ? ctrl.tree.longestValidPath(prevPath) : treePath.root;
      ctrl.userJump(path);
    }

    configurePractice();

    // m.redraw.strategy("all"); // create a new cg
    redraw();
    ctrl.startCeval();
  };

  var xhrReload = function() {
    vm.loading = true;
    return xhr.reload(
      practice ? 'practice/load' : 'study',
      data.id,
      vm.mode.sticky ? undefined : vm.chapterId
    ).then(onReload, window.lichess.reload);
  };

  var onSetPath = throttle(300, false, function(path) {
    if (vm.mode.sticky && path !== data.position.path) makeChange("setPath", addChapterId({
      path: path
    }));
  });

  if (members.canContribute()) form.openIfNew();

  function currentNode() {
    return ctrl.node;
  };

  const share = shareCtrl(data, currentChapter, currentNode, redraw);

  var practice = practiceData && practiceCtrl(ctrl, data, practiceData);

  function mutateCgConfig(config) {
    config.drawable.onChange = function(shapes) {
      if (vm.mode.write) {
        ctrl.tree.setShapes(shapes, ctrl.path);
        makeChange("shapes", addChapterId({
          path: ctrl.path,
          shapes: shapes
        }));
      }
    };
  }

  function wrongChapter(serverData) {
    if (serverData.p.chapterId !== vm.chapterId) {
      // sticky should really be on the same chapter
      if (vm.mode.sticky && serverData.sticky) xhrReload();
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
    toggleLike() {
      send("like", {
        liked: !data.liked
      });
    },
    position: function() {
      return data.position;
    },
    currentChapter: currentChapter,
    isChapterOwner: isChapterOwner,
    canJumpTo(path: Tree.Path) {
      return data.chapter.conceal === null ||
        isChapterOwner() ||
        treePath.contains(ctrl.path, path) || // can always go back
        ctrl.tree.lastMainlineNode(path).ply <= data.chapter.conceal!;
    },
    onJump: practice ? practice.onJump : function() {},
    withPosition: function(obj) {
      obj.ch = vm.chapterId;
      obj.path = ctrl.path;
      return obj;
    },
    setPath: function(path, node) {
      onSetPath(path);
      setTimeout(() => commentForm.onSetPath(path, node), 100);
    },
    deleteNode: function(path) {
      makeChange("deleteNode", addChapterId({
        path: path,
        jumpTo: ctrl.path
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
        vm.mode.sticky = false;
        vm.chapterId = id;
        xhrReload();
      }
      vm.loading = true;
      vm.nextChapterId = id;
      redraw();
    },
    toggleSticky: function() {
      vm.mode.sticky = !vm.mode.sticky && data.features.sticky;
      xhrReload();
    },
    toggleWrite: function() {
      vm.mode.write = !vm.mode.write && members.canContribute();
      xhrReload();
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
        if (!vm.mode.sticky) {
          vm.behind++;
          return;
        }
        if (position.chapterId !== data.position.chapterId ||
          !ctrl.tree.pathExists(position.path)) {
          return xhrReload();
        }
        data.position.path = position.path;
        if (who && who.s === sri) return;
        ctrl.userJump(position.path);
        redraw();
      },
      addNode: function(d) {
        var position = d.p,
          node = d.n,
          who = d.w,
          sticky = d.s;
        who && members.setActive(who.u);
        if (d.s && !vm.mode.sticky) vm.behind++;
        if (wrongChapter(d)) return;
        // node author already has the node
        if (sticky && who && who.s === sri) {
          data.position.path = position.path + node.id;
          return;
        }
        var newPath = ctrl.tree.addNode(node, position.path);
        if (!newPath) return xhrReload();
        ctrl.tree.addDests(d.d, newPath, d.o);
        if (sticky) data.position.path = newPath;
        if ((sticky && vm.mode.sticky) || (
          position.path === ctrl.path &&
            position.path === treePath.fromNodeList(ctrl.mainline)
        )) ctrl.jump(newPath);
        redraw();
      },
      deleteNode: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        // deleter already has it done
        if (who && who.s === sri) return;
        if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
        ctrl.tree.deleteNodeAt(position.path);
        if (vm.mode.sticky) ctrl.jump(ctrl.path);
        redraw();
      },
      promote: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) return;
        if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
        ctrl.tree.promoteAt(position.path, d.toMainline);
        if (vm.mode.sticky) ctrl.jump(ctrl.path);
      },
      reload: xhrReload,
      changeChapter: function(d) {
        d.w && members.setActive(d.w.u);
        if (!vm.mode.sticky) vm.behind++;
        data.position = d.p;
        if (vm.mode.sticky) xhrReload();
      },
      addChapter: function(d) {
        d.w && members.setActive(d.w.u);
        if (d.s && !vm.mode.sticky) vm.behind++;
        if (d.s) data.position = d.p;
        else if (d.w && d.w.s === sri) {
          vm.mode.write = true;
          vm.chapterId = d.p.chapterId;
        }
        xhrReload();
      },
      members: function(d) {
        members.update(d);
        configureAnalysis();
        redraw();
      },
      chapters: function(d) {
        chapters.list(d);
        redraw();
      },
      shapes: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) return;
        ctrl.tree.setShapes(d.s, ctrl.path);
        if (ctrl.path === position.path && ctrl.chessground) ctrl.chessground.setShapes(d.s);
        redraw();
      },
      setComment: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) commentForm.dirty(false);
        ctrl.tree.setCommentAt(d.c, position.path);
        redraw();
      },
      setTags: function(d) {
        d.w && members.setActive(d.w.u);
        if (d.chapterId !== vm.chapterId) return;
        data.chapter.tags = d.tags;
        redraw();
      },
      deleteComment: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        ctrl.tree.deleteCommentAt(d.id, position.path);
        redraw();
      },
      glyphs: function(d) {
        var position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) glyphForm.dirty(false);
        ctrl.tree.setGlyphsAt(d.g, position.path);
        redraw();
      },
      conceal: function(d) {
        if (wrongChapter(d)) return;
        data.chapter.conceal = d.ply;
        redraw();
      },
      liking: function(d) {
        data.likes = d.l.likes;
        if (d.w && d.w.s === sri) data.liked = d.l.me;
        redraw();
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

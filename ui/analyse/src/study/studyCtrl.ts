import { throttle, prop } from 'common';
import AnalyseCtrl from '../ctrl';
import { ctrl as memberCtrl } from './studyMembers';
import { ctrl as chapterCtrl } from './studyChapters';
import practiceCtrl from './practice/studyPracticeCtrl';
import { StudyPracticeData, StudyPracticeCtrl } from './practice/interfaces';
import { ctrl as commentFormCtrl } from './commentForm';
import { ctrl as glyphFormCtrl } from './studyGlyph';
import { ctrl as studyFormCtrl, StudyFormCtrl } from './studyForm';
import { ctrl as notifCtrl } from './notif';
import { ctrl as shareCtrl } from './studyShare';
import { ctrl as tagsCtrl } from './studyTags';
import * as tours from './studyTour';
import * as xhr from './studyXhr';
import { path as treePath } from 'tree';
import { StudyCtrl, StudyVm, Tab, TagTypes, StudyData, StudyChapterMeta, ReloadData } from './interfaces';

const li = window.lichess;

// data.position.path represents the server state
// ctrl.path is the client state
export default function(data: StudyData, ctrl: AnalyseCtrl, tagTypes: TagTypes, practiceData?: StudyPracticeData): StudyCtrl {

  const send = ctrl.socket.send;
  const redraw = ctrl.redraw;

  const sri: string = li.StrongSocket ? li.StrongSocket.sri : '';

  const vm: StudyVm = (function() {
    const isManualChapter = data.chapter.id !== data.position.chapterId;
    const sticked = data.features.sticky && !ctrl.initialPath && !isManualChapter && !practiceData;
    return {
      loading: false,
      tab: prop<Tab>(data.chapters.length > 1 ? 'chapters' : 'members'),
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

  const form: StudyFormCtrl = studyFormCtrl((d, isNew) => {
    send("editStudy", d);
    if (isNew && data.chapter.setup.variant.key === 'standard' && ctrl.mainline.length === 1 && !data.chapter.setup.fromFen)
      chapters.newForm.openInitial();
  }, () => data, redraw);

  function startTour() {
    tours.study(ctrl);
  };

  const members = memberCtrl({
    initDict: data.members,
    myId: practiceData ? null : ctrl.opts.userId,
    ownerId: data.ownerId,
    send,
    tab: vm.tab,
    startTour,
    notif,
    onBecomingContributor() {
      vm.mode.write = true;
    },
    redraw
  });

  const chapters = chapterCtrl(
    data.chapters,
    send,
    () => vm.tab('chapters'),
    chapterId => xhr.chapterConfig(data.id, chapterId),
    ctrl);

  function currentChapter(): StudyChapterMeta {
    return chapters.get(vm.chapterId)!;
  };
  function isChapterOwner() {
    return ctrl.opts.userId === data.chapter.ownerId;
  };

  function makeChange(t: string, d: any): boolean {
    if (vm.mode.write) {
      send(t, d);
      return true;
    }
    return vm.mode.sticky = false;
  };

  const commentForm = commentFormCtrl(ctrl);
  const glyphForm = glyphFormCtrl(ctrl);
  const tags = tagsCtrl(ctrl, () => data.chapter, tagTypes);

  function addChapterId(req) {
    req.ch = vm.chapterId;
    return req;
  }
  if (vm.mode.sticky) ctrl.userJump(data.position.path);

  function configureAnalysis() {
    if (ctrl.embed) return;
    const canContribute = members.canContribute();
    // unwrite if member lost priviledges
    vm.mode.write = vm.mode.write && canContribute;
    li.pubsub.emit('chat.writeable')(data.features.chat);
    li.pubsub.emit('chat.permissions')({local: canContribute});
    const computer = data.chapter.features.computer || data.chapter.practice;
    if (!computer) ctrl.getCeval().enabled(false);
    ctrl.getCeval().allowed(computer);
    if (!data.chapter.features.explorer) ctrl.explorer.disable();
    ctrl.explorer.allowed(data.chapter.features.explorer);
  };
  configureAnalysis();

  function configurePractice() {
    if (!data.chapter.practice && ctrl.practice) ctrl.togglePractice();
    if (data.chapter.practice) ctrl.restartPractice();
    if (practice) practice.onReload();
  };

  function onReload(d: ReloadData) {
    const s = d.study!;
    const prevPath = ctrl.path;
    const sameChapter = data.chapter.id === s.chapter.id;
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

    const merge = !vm.mode.write && sameChapter;
    ctrl.reloadData(d.analysis, merge);
    configureAnalysis();
    vm.loading = false;

    if (vm.mode.sticky) {
      vm.chapterId = data.position.chapterId;
      ctrl.userJump(data.position.path);
    } else {
      // path could be gone
      const path = sameChapter ? ctrl.tree.longestValidPath(prevPath) : treePath.root;
      ctrl.userJump(path);
    }

    configurePractice();

    redraw();
    ctrl.startCeval();
  };

  function xhrReload() {
    vm.loading = true;
    return xhr.reload(
      practice ? 'practice/load' : 'study',
      data.id,
      vm.mode.sticky ? undefined : vm.chapterId
    ).then(onReload, li.reload);
  };

  const onSetPath = throttle(300, false, function(path) {
    if (vm.mode.sticky && path !== data.position.path) makeChange("setPath", addChapterId({
      path
    }));
  });

  if (members.canContribute()) form.openIfNew();

  function currentNode() {
    return ctrl.node;
  };

  const share = shareCtrl(data, currentChapter, currentNode, redraw);

  const practice: StudyPracticeCtrl | undefined = practiceData && practiceCtrl(ctrl, data, practiceData);

  function mutateCgConfig(config) {
    config.drawable.onChange = function(shapes) {
      if (vm.mode.write) {
        ctrl.tree.setShapes(shapes, ctrl.path);
        makeChange("shapes", addChapterId({
          path: ctrl.path,
          shapes
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
    data,
    form,
    members,
    chapters,
    notif,
    commentForm,
    glyphForm,
    share,
    tags,
    vm,
    toggleLike() {
      send("like", {
        liked: !data.liked
      });
    },
    position() {
      return data.position;
    },
    currentChapter,
    isChapterOwner,
    canJumpTo(path: Tree.Path) {
      return data.chapter.conceal === undefined ||
        isChapterOwner() ||
        treePath.contains(ctrl.path, path) || // can always go back
        ctrl.tree.lastMainlineNode(path).ply <= data.chapter.conceal!;
    },
    onJump: practice ? practice.onJump : $.noop,
    withPosition(obj) {
      obj.ch = vm.chapterId;
      obj.path = ctrl.path;
      return obj;
    },
    setPath(path, node) {
      onSetPath(path);
      setTimeout(() => commentForm.onSetPath(path, node), 100);
    },
    deleteNode(path) {
      makeChange("deleteNode", addChapterId({
        path,
        jumpTo: ctrl.path
      }));
    },
    promote(path, toMainline) {
      makeChange("promote", addChapterId({
        toMainline,
        path
      }));
    },
    setChapter(id, force) {
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
    makeChange,
    startTour,
    userJump: ctrl.userJump,
    currentNode,
    practice,
    mutateCgConfig,
    redraw,
    socketHandlers: {
      path(d) {
        const position = d.p,
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
      addNode(d) {
        const position = d.p,
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
        const newPath = ctrl.tree.addNode(node, position.path);
        if (!newPath) return xhrReload();
        ctrl.tree.addDests(d.d, newPath, d.o);
        if (sticky) data.position.path = newPath;
        if ((sticky && vm.mode.sticky) || (
          position.path === ctrl.path &&
            position.path === treePath.fromNodeList(ctrl.mainline)
        )) ctrl.jump(newPath);
        redraw();
      },
      deleteNode(d) {
        const position = d.p,
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
      promote(d) {
        const position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) return;
        if (!ctrl.tree.pathExists(d.p.path)) return xhrReload();
        ctrl.tree.promoteAt(position.path, d.toMainline);
        if (vm.mode.sticky) ctrl.jump(ctrl.path);
      },
      reload: xhrReload,
      changeChapter(d) {
        d.w && members.setActive(d.w.u);
        if (!vm.mode.sticky) vm.behind++;
        data.position = d.p;
        if (vm.mode.sticky) xhrReload();
      },
      addChapter(d) {
        d.w && members.setActive(d.w.u);
        if (d.s && !vm.mode.sticky) vm.behind++;
        if (d.s) data.position = d.p;
        else if (d.w && d.w.s === sri) {
          vm.mode.write = true;
          vm.chapterId = d.p.chapterId;
        }
        xhrReload();
      },
      members(d) {
        members.update(d);
        configureAnalysis();
        redraw();
      },
      chapters(d) {
        chapters.list(d);
        if (!currentChapter()) {
          vm.chapterId = d[0].id;
          if (!vm.mode.sticky) xhrReload();
        }
        redraw();
      },
      shapes(d) {
        const position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) return;
        ctrl.tree.setShapes(d.s, ctrl.path);
        if (ctrl.path === position.path && ctrl.chessground) ctrl.chessground.setShapes(d.s);
        redraw();
      },
      setComment(d) {
        const position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) commentForm.dirty(false);
        ctrl.tree.setCommentAt(d.c, position.path);
        redraw();
      },
      setTags(d) {
        d.w && members.setActive(d.w.u);
        if (d.chapterId !== vm.chapterId) return;
        data.chapter.tags = d.tags;
        redraw();
      },
      deleteComment(d) {
        const position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        ctrl.tree.deleteCommentAt(d.id, position.path);
        redraw();
      },
      glyphs(d) {
        const position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        if (who && who.s === sri) glyphForm.dirty(false);
        ctrl.tree.setGlyphsAt(d.g, position.path);
        redraw();
      },
      clock(d) {
        const position = d.p,
          who = d.w;
        who && members.setActive(who.u);
        if (wrongChapter(d)) return;
        ctrl.tree.setClockAt(d.c, position.path);
        redraw();
      },
      conceal(d) {
        if (wrongChapter(d)) return;
        data.chapter.conceal = d.ply;
        redraw();
      },
      liking(d) {
        data.likes = d.l.likes;
        if (d.w && d.w.s === sri) data.liked = d.l.me;
        redraw();
      },
      following_onlines: members.inviteForm.setFollowings,
      following_leaves: members.inviteForm.delFollowing,
      following_enters: members.inviteForm.addFollowing,
      crowd(d) {
        members.setSpectators(d.users);
      },
      error(msg) {
        alert(msg);
      }
    }
  };
};

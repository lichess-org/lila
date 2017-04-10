/// <reference types="types/lichess-jquery" />

import { AnalyseController, AnalyseOpts } from './interfaces';

import ctrl = require('./ctrl');
import view = require('./view');
import studyView = require('./study/studyView');
import studyPracticeView = require('./study/practice/studyPracticeView');
import boot = require('./boot');
import * as m from 'mithril';
import { Chessground } from 'chessground';

export function mithril(opts: AnalyseOpts) {
  const controller: AnalyseController = new ctrl(opts);

  m.module<AnalyseController>(opts.element, {
    controller: function() {
      return controller;
    },
    view: view
  });

  if (controller.study && opts.sideElement) m.module(opts.sideElement, {
    controller: function() {
      m.redraw.strategy("diff"); // prevents double full redraw on page load
      return controller.study;
    },
    view: controller.studyPractice ? studyPracticeView.main : studyView.main
  });

  return {
    socketReceive: controller.socket.receive,
    jumpToIndex: function(index: number): void {
      controller.jumpToIndex(index);
      m.redraw();
    },
    path: function(): Tree.Path {
      return controller.vm.path;
    },
    setChapter: function(id: string) {
      if (controller.study) controller.study.setChapter(id);
    }
  }
}

export { boot };

// that's for the rest of lichess to access chessground
// without having to include it a second time
window.Chessground = Chessground;

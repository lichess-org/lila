import type { AnalyseNvuiContext } from '../analyse.nvui';
import { type LooseVNodes, hl } from 'lib/snabbdom';
import type AnalyseCtrl from '../ctrl';
import type { RetroCtrl } from '../retrospect/retroCtrl';
import { renderSan } from 'lib/nvui/chess';
import { liveText } from 'lib/nvui/notify';
import { clickHook } from '../view/nvuiView';

export function renderRetro(ctx: AnalyseNvuiContext): LooseVNodes {
  const { ctrl } = ctx;
  if (ctrl.ongoing || ctrl.synthetic || !ctrl.hasFullComputerAnalysis()) return;

  const nodes: LooseVNodes = [
    hl(
      'button.retro-toggle',
      clickHook(ctrl.toggleRetro, ctrl.redraw),
      ctrl.retro ? 'Stop learning from mistakes' : i18n.site.learnFromYourMistakes,
    ),
  ];
  if (ctrl.retro) {
    const current = ctrl.retro.current();
    const mistakes = ctrl.retro.completion();

    let state = ctrl.retro.feedback();
    if (ctrl.retro.isSolving() && current && ctrl.path !== current.prev.path) state = 'offTrack';

    nodes.push(
      hl('label', `Mistake ${Math.min(mistakes[0] + 1, mistakes[1])} of ${mistakes[1]}`),
      retroStateBtns[state]?.(ctx as RetroContext),
    );
  }
  return nodes;
}

interface RetroContext extends AnalyseNvuiContext {
  readonly ctrl: AnalyseCtrl & { retro: RetroCtrl };
}

function solveAndSkipBtns({ ctrl }: RetroContext): LooseVNodes {
  return [
    hl(
      'button.retro-solve',
      clickHook(() => ctrl.retro.feedback('view'), ctrl.redraw),
      i18n.site.viewTheSolution,
    ),
    hl('button.retro-skip', clickHook(ctrl.retro.skip, ctrl.redraw), i18n.site.skipThisMove),
  ];
}

function nextMistakeBtn(ctx: RetroContext): LooseVNodes {
  const { ctrl } = ctx;
  return ctrl.retro.current()
    ? hl('button.retro-next', clickHook(ctrl.retro.skip, ctrl.redraw), i18n.site.next)
    : doneWithMistakes(ctx);
}

function doneWithMistakes({ ctrl }: RetroContext, prelude = ''): LooseVNodes {
  const noMistakes = !ctrl.retro.completion()[1];
  return [
    liveText(
      (prelude ? prelude + '. ' : '') +
        i18n.site[
          noMistakes
            ? ctrl.retro.color === 'white'
              ? 'noMistakesFoundForWhite'
              : 'noMistakesFoundForBlack'
            : ctrl.retro.color === 'white'
              ? 'doneReviewingWhiteMistakes'
              : 'doneReviewingBlackMistakes'
        ],
    ),
    !noMistakes && hl('button.retro-again', clickHook(ctrl.retro.reset, ctrl.redraw), i18n.site.doItAgain),
    hl(
      'button.retro-flip',
      clickHook(ctrl.retro.flip, ctrl.redraw),
      i18n.site[ctrl.retro.color === 'white' ? 'reviewBlackMistakes' : 'reviewWhiteMistakes'],
    ),
  ];
}

let debounceRedraw: number;

const retroStateBtns = {
  offTrack({ ctrl }: RetroContext): LooseVNodes {
    return [
      hl('p', i18n.site.youBrowsedAway),
      hl('button.retro-resume', clickHook(ctrl.retro.jumpToNext, ctrl.redraw), i18n.site.resumeLearning),
    ];
  },
  fail(ctx: RetroContext): LooseVNodes {
    return retroStateBtns.find(ctx, `${i18n.site.youCanDoBetter}. `, true);
  },
  win(ctx: RetroContext): LooseVNodes {
    ctx.ctrl.retro.feedback('find');
    return retroStateBtns.find(ctx, `${i18n.study.goodMove}. `);
  },
  view(ctx: RetroContext): LooseVNodes {
    const { ctrl } = ctx;
    if (!ctrl.retro.current()) return doneWithMistakes(ctx);
    const node = ctrl.retro.current()!.solution.node;
    return [
      liveText(`${i18n.site.solution} ${renderSan(node.san, node.uci, ctx.moveStyle.get())}.`),
      nextMistakeBtn(ctx),
    ];
  },
  find(ctx: RetroContext, prelude = '', tryAgain = false): LooseVNodes {
    const { ctrl } = ctx;
    const node = ctrl.retro.current()?.fault.node;
    if (!node) return doneWithMistakes(ctx, prelude);
    const c = ctrl.retro.color;
    const trailer =
      c === 'white'
        ? tryAgain
          ? i18n.site.tryAnotherMoveForWhite
          : i18n.site.findBetterMoveForWhite
        : tryAgain
          ? i18n.site.tryAnotherMoveForBlack
          : i18n.site.findBetterMoveForBlack;
    return [
      liveText(
        prelude +
          `Turn ${Math.floor((node.ply + 1) / 2)}. ${i18n.site[c]} ` +
          `played ${renderSan(node.san, node.uci, ctx.moveStyle.get())}. ${trailer}`,
      ),
      solveAndSkipBtns(ctx),
    ];
  },
  eval({ ctrl }: RetroContext) {
    clearTimeout(debounceRedraw);
    debounceRedraw = setTimeout(ctrl.redraw, 200);
    return undefined;
  },
};

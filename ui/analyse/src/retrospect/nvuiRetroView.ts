import type { AnalyseNvuiContext } from '../analyse.nvui';
import { type LooseVNodes, hl } from 'lib/snabbdom';
import { type VNodeData } from 'snabbdom';
import type AnalyseCtrl from '../ctrl';
import type { RetroCtrl } from '../retrospect/retroCtrl';
import { renderSan } from 'lib/nvui/chess';
import { liveText } from 'lib/nvui/notify';
import { clickHook, renderCurrentNode } from '../view/nvuiView';

export function renderRetro(nvuiCtx: AnalyseNvuiContext): LooseVNodes {
  const ctx = makeContext(nvuiCtx);
  const { ctrl } = ctx;
  if (ctrl.ongoing || ctrl.synthetic || !ctrl.hasFullComputerAnalysis()) return;
  const current = ctrl.retro?.current();
  const mistakes = ctrl.retro?.completion();

  let state = ctrl.retro?.feedback();
  if (ctrl.retro?.isSolving() && current && ctrl.path !== current.prev.path) state = 'offTrack';

  return [
    hl(
      'button.retro-toggle',
      ctx.focusFriendlyHook(ctrl.toggleRetro),
      ctrl.retro ? i18n.site.finished : i18n.site.learnFromYourMistakes,
    ),
    hl('div.retro-view', { key: 'retro-view' }, [
      hl('label', mistakes && `Mistake ${Math.min(mistakes[0] + 1, mistakes[1])} of ${mistakes[1]}`),
      retroStateView[state ?? 'none'](ctx),
    ]),
  ];
}

function doneWithMistakes({ spoken, ctrl, focusFriendlyHook }: RetroContext, prelude = ''): LooseVNodes {
  const noMistakes = !ctrl.retro.completion()[1];
  return [
    spoken(
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
    !noMistakes && hl('button.retro-again', focusFriendlyHook(ctrl.retro.reset), i18n.site.doItAgain),
    hl(
      'button.retro-flip',
      focusFriendlyHook(ctrl.retro.flip),
      i18n.site[ctrl.retro.color === 'white' ? 'reviewBlackMistakes' : 'reviewWhiteMistakes'],
    ),
  ];
}

let debounceRedraw: number;

const retroStateView = {
  offTrack({ spoken, ctrl, moveStyle, focusFriendlyHook }: RetroContext): LooseVNodes {
    return [
      spoken(i18n.site.youBrowsedAway + ', ' + renderCurrentNode({ ctrl, moveStyle }) + '.'),
      hl('button.retro-resume', focusFriendlyHook(ctrl.retro.jumpToNext), i18n.site.resumeLearning),
    ];
  },
  fail(ctx: RetroContext): LooseVNodes {
    return retroStateView.find(ctx, `${i18n.site.youCanDoBetter}, `, true);
  },
  win(ctx: RetroContext): LooseVNodes {
    ctx.ctrl.retro.feedback('find');
    return retroStateView.find(ctx, `${i18n.study.goodMove}, `);
  },
  view(ctx: RetroContext): LooseVNodes {
    const { ctrl, spoken, focusFriendlyHook } = ctx;
    if (!ctrl.retro.current()) return doneWithMistakes(ctx);
    const node = ctrl.retro.current()!.solution.node;
    const solution = `${i18n.site.solution} ${renderSan(node.san, node.uci, ctx.moveStyle.get())}.`;
    return ctrl.retro.current()
      ? [spoken(solution), hl('button.retro-next', focusFriendlyHook(ctrl.retro.skip), i18n.site.next)]
      : doneWithMistakes(ctx, solution);
  },
  find(ctx: RetroContext, prelude = '', tryAgain = false): LooseVNodes {
    const { ctrl, spoken, focusFriendlyHook } = ctx;
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
      spoken(
        prelude +
          `Turn ${Math.floor((node.ply + 1) / 2)}, ${i18n.site[c]} ` +
          `played ${renderSan(node.san, node.uci, ctx.moveStyle.get())}, ${trailer}`,
      ),
      hl(
        'button.retro-solve',
        focusFriendlyHook(() => ctrl.retro.feedback('view')),
        i18n.site.viewTheSolution,
      ),
      hl('button.retro-skip', focusFriendlyHook(ctrl.retro.skip), i18n.site.skipThisMove),
      ,
    ];
  },
  eval({ ctrl }: RetroContext) {
    clearTimeout(debounceRedraw);
    debounceRedraw = setTimeout(ctrl.redraw, 200);
    return undefined;
  },
  none(ctx: RetroContext): LooseVNodes {
    return ctx.spoken('');
  },
};

function makeContext(nvuiCtx: AnalyseNvuiContext): RetroContext {
  return {
    ...nvuiCtx,
    spoken: (text: string) => liveText(text, 'assertive', 'p.retro-spoken'),
    focusFriendlyHook: (callback: () => void): VNodeData =>
      clickHook(callback, () => {
        document.querySelector<HTMLElement>('p.retro-spoken')?.focus();
        nvuiCtx.ctrl.redraw();
      }),
  } as RetroContext;
}

interface RetroContext extends AnalyseNvuiContext {
  readonly ctrl: AnalyseCtrl & { retro: RetroCtrl };
  spoken: (text: string) => LooseVNodes;
  focusFriendlyHook(callback: () => void): VNodeData;
}

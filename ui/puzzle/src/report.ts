import { report as xhrReport } from './xhr';
import type PuzzleCtrl from './ctrl';
import type { PuzzleId, ThemeKey } from './interfaces';
import { winningChances } from 'lib/ceval';
import * as licon from 'lib/licon';
import { type StoredProp, storedIntProp } from 'lib/storage';
import { domDialog } from 'lib/view';
import { plyToTurn, pieceCount } from 'lib/game/chess';
import type { ClientEval, PvData, TreeNode } from 'lib/tree/types';

// bump when logic is changed, to distinguish cached clients from new ones
const version = 10;

export default class Report {
  // if local eval suspect multiple solutions, report the puzzle, once at most
  private reported: boolean = false;
  // timestamp (ms) of the last time the user clicked on the hide report dialog toggle
  private tsHideReportDialog: StoredProp<number>;
  // number of evals that have triggered the `winningChances.hasMultipleSolutions` method
  // this is used to reduce the number of fps due to fluke eval
  private evalsWithMultipleSolutions = 0;

  constructor() {
    this.tsHideReportDialog = storedIntProp('puzzle.report.hide.ts', 0);
  }

  // (?)take the eval as arg instead of taking it from the node to be sure it's the most up to date
  // All non-mates puzzle should have one and only one solution, if that is not the case, report it back to backend
  checkForMultipleSolutions(ev: ClientEval, ctrl: PuzzleCtrl, threatMode: boolean): void {
    // first, make sure we're in view mode so we know the solution is the mainline
    // do not check, checkmate puzzles
    if (
      !ctrl.session.userId ||
      this.reported ||
      ctrl.mode !== 'view' ||
      // Sometimes there is a race condition where a threat eval is sent, while `ctrl.threatMode()`
      // is not yet set to true. So we need to check for `threatMode` as well.
      ctrl.threatMode() ||
      threatMode ||
      // the `mate` key theme is not sent, as it is considered redubant with `mateInX`
      ctrl.data.puzzle.themes.some((t: ThemeKey) => t.toLowerCase().includes('mate')) ||
      // positions with 7 pieces or less can be checked with the tablebase
      pieceCount(ev.fen) <= 7 ||
      // dynamic import from web worker feature is shared by all stockfish 16+ WASMs
      !ctrl.ceval.engines.active?.requires?.includes('dynamicImportFromWorker') ||
      // if the user has chosen to hide the dialog less than a week ago
      this.tsHideReportDialog() > Date.now() - 1000 * 3600 * 24 * 7
    )
      return;
    const node = ctrl.node;
    // more resilient than checking the turn directly, if eventually puzzles get generated from 'from position' games
    const nodeTurn = node.fen.includes(' w ') ? 'white' : 'black';
    if (
      nextMoveInSolution(node) &&
      nodeTurn === ctrl.pov &&
      ctrl.mainline.some((n: TreeNode) => n.id === node.id)
    ) {
      const [bestEval, secondBestEval] = [ev.pvs[0], ev.pvs[1]];
      // stricter than lichess-puzzler v49 check in how it defines similar moves
      if (
        (ev.depth > 50 || ev.nodes > 25_000_000) &&
        bestEval &&
        secondBestEval &&
        winningChances.hasMultipleSolutions(ctrl.pov, bestEval, secondBestEval)
      ) {
        this.evalsWithMultipleSolutions += 1;
      } else {
        this.evalsWithMultipleSolutions = 0;
      }
      if (this.evalsWithMultipleSolutions === 2) {
        // in all case, we do not want to show the dialog more than once
        this.reported = true;
        const engine = ctrl.ceval.engines.active;
        const engineName = engine?.short || engine.name;
        const reason = `(v${version}, ${engineName}) after move ${plyToTurn(node.ply)}. ${node.san}, at depth ${ev.depth}, multiple solutions:\n\n${ev.pvs.map(pv => `${pvEvalToStr(pv)}: ${pv.moves.join(' ')}`).join('\n\n')}`;
        this.reportDialog(ctrl.data.puzzle.id, reason);
      }
    }
  }

  private reportDialog = (puzzleId: PuzzleId, reason: string) => {
    const switchButton =
      `<div class="switch switch-report-puzzle" title="temporarily disable reporting puzzles">` +
      `<input id="puzzle-toggle-report" class="cmn-toggle cmn-toggle--subtle" type="checkbox">` +
      `<label for="puzzle-toggle-report"></label></div>`;

    const hideButtonDiv = `<div style="display:flex; flex-flow: row nowrap; align-items: center; justify-content: center">${switchButton}<span style="padding-left: 1em"> Hide this for a week</span></div>`;

    const hideDialogInput = () => document.querySelector('.switch-report-puzzle input') as HTMLInputElement;

    domDialog({
      focus: '.apply',
      modal: true,
      htmlText:
        '<div><strong style="font-size:1.5em">' +
        'Report multiple solutions' +
        '</strong><br /><br />' +
        '<p>' +
        'You have found a puzzle with multiple solutions, report it?' +
        '</p><br />' +
        hideButtonDiv +
        '<br /><br />' +
        `<button type="reset" class="button button-empty button-red text reset" data-icon="${licon.X}">No</button>` +
        `<button type="submit" class="button button-green text apply" data-icon="${licon.Checkmark}">Yes</button>`,
    }).then(dlg => {
      $('.switch-report-puzzle', dlg.view).on('click', () => {
        const input = hideDialogInput();
        input.checked = !input.checked;
      });
      $('.reset', dlg.view).on('click', () => {
        if (hideDialogInput().checked) {
          this.tsHideReportDialog(Date.now());
        }

        dlg.close();
      });
      $('.apply', dlg.view).on('click', () => {
        xhrReport(puzzleId, reason);
        dlg.close();
      });
      dlg.show();
    });
  };
}

// since we check the nodes of the opposite side, to know if we're
// in the solution we need to check the following move
const nextMoveInSolution = (before: TreeNode) => {
  const node = before.children[0];
  return node && (node.puzzle === 'good' || node.puzzle === 'win');
};

const pvEvalToStr = (pv: PvData): string => {
  return pv.mate ? `#${pv.mate}` : `${pv.cp}`;
};

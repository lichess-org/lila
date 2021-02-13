import * as gridHacks from 'common/gridHacks';

export function start(container: HTMLElement): void {

  if (!gridHacks.needsBoardHeightFix()) return;

  const runHacks = () => gridHacks.fixMainBoardHeight(container);

  gridHacks.runner(runHacks);

  gridHacks.bindChessgroundResizeOnce(runHacks);
}

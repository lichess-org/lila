import type AnalyseCtrl from '../ctrl';
import { localAnalysisDialog } from './localAnalysisDialog';

export async function initModule(ctrl: AnalyseCtrl): Promise<void> {
  return localAnalysisDialog(ctrl);
}

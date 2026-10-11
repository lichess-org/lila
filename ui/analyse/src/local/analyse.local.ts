import type AnalyseCtrl from '../ctrl';
import { localAnalysisDialog } from './localAnalysisDialog';

export async function initModule(ctrl: AnalyseCtrl): Promise<void> {
  await site.asset.loadI18n('localAnalysis');
  return localAnalysisDialog(ctrl);
}

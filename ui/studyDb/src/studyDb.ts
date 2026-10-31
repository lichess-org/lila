import { StudyDbCtrl } from './studyDbCtrl';

export interface InitOptions {
  action?: 'toggle' | 'open' | 'close';
}

let ctrl: StudyDbCtrl | undefined;

export function initModule({ action = 'toggle' }: InitOptions = {}): void {
  ctrl ??= new StudyDbCtrl();
  ctrl[action]();
}

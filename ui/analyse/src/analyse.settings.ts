import { fetchSettings } from './settingsCtrl';
import { settingsView } from './view/settingsView';

fetchSettings().then(ctrl => {
  document.querySelector('.analysis-settings')?.replaceWith(settingsView(ctrl));
});

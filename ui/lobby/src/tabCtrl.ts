import LobbyController from './ctrl';
import * as xhr from './xhr';
import { AppTab as AppTabKey, Tab as TabKey } from './interfaces';

export class TabCtrl {
  storagePostfix = document.body.dataset.user ? `:${document.body.dataset.user}` : '';
  active: TabKey;
  appTab: AppTabKey;

  private tabs: { [tab in TabKey]?: SecondaryTab } = {
    quick: { primary: 'quick', available: () => !this.ctrl.me?.isBot },
    realtime: {
      primary: 'lobby',
      i18n: 'Real time',
      available: () => !this.ctrl.me?.isBot,
      enter: () => this.ctrl.socket.realTimeIn(),
      exit: () => this.ctrl.socket.realTimeOut(),
    },
    correspondence: {
      primary: 'lobby',
      i18n: 'Correspondence',
      available: () => !this.ctrl.me?.isBot,
      enter: () => xhr.seeks().then(this.ctrl.setSeeks),
    },
    tournament: { primary: 'tournament', available: () => !!this.ctrl.me },
    playing: { primary: 'your', i18n: 'Playing', available: () => this.ctrl.data.nbNowPlaying > 0 },
    recent: { primary: 'your', i18n: 'Recent', available: () => !!this.ctrl.me },
  };

  constructor(readonly ctrl: LobbyController) {
    this.active = localStorage.getItem('lobby.tab' + this.storagePostfix) as TabKey;
    if (!this.active || !this.tabs[this.active]?.available())
      this.active = Object.entries(this.tabs).filter(([, tab]) => tab.available())[0][0] as TabKey;

    this.appTab = this.tabs[this.active]!.primary;
  }

  get showingHooks(): boolean {
    return this.active === 'realtime'; // || this.tab === 'variant';
  }

  get visibleAppTabNames(): [TabKey, string][] {
    const primaryI18n: [AppTabKey, string][] = [
      ['quick', 'Quick'],
      ['lobby', 'Lobby'],
      ['tournament', 'Tournament'],
      ['your', 'Your games'],
    ];
    return primaryI18n.filter(([k]) => Object.values(this.tabs).some(t => t.available() && t.primary === k));
  }

  get visibleSecondaryTabNames(): [TabKey, string][] {
    const appTab = this.appTab;
    return Object.entries(this.tabs)
      .filter(([, t]) => t.available() && t.i18n && t.primary === appTab)
      .map(([k, t]) => [k, t.i18n] as [TabKey, string]);
  }

  isShowing(tab: TabKey) {
    return this.active === tab || this.tabs[this.active]?.primary === tab;
  }

  setTab(newKey: TabKey) {
    if (newKey === 'your') newKey = this.getPreferredSecondary('your');
    else if (newKey === 'lobby') newKey = this.getPreferredSecondary('lobby');
    this.tabs[this.active]?.exit?.();
    this.active = newKey;
    const tab = this.tabs[newKey];
    this.appTab = tab?.primary || 'your';
    tab?.enter?.();
    if (tab?.primary === 'your' || tab?.primary === 'lobby')
      localStorage.setItem(`lobby.tab.${tab.primary}` + this.storagePostfix, this.active);
    localStorage.setItem('lobby.tab' + this.storagePostfix, this.active);
  }

  getPreferredSecondary<Key extends TabKey>(tab: AppTabKey) {
    const secondary = localStorage.getItem(`lobby.tab.${tab}` + this.storagePostfix) as Key;
    if (this.tabs[secondary]?.available()) return secondary;
    return Object.entries(this.tabs).filter(([, t]) => t.primary === tab && t.available())[0][0] as Key;
  }
}

interface SecondaryTab {
  i18n?: string;
  primary: AppTabKey;
  available(): boolean;
  enter?: () => void;
  exit?: () => void;
}

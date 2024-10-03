import { Switch } from 'common/switch';
import LobbyController from './ctrl';
import * as xhr from './xhr';
import { AppTab as AppTabKey, Tab as TabKey } from './interfaces';

export class TabCtrl {
  app: Switch<AppTabKey, Switch<TabKey, SecondaryTab>> = new Switch({ storage: 'lobby.tab' });

  private tabs: { [tab in TabKey]?: SecondaryTab } = {
    realtime: {
      i18n: 'Real time',
      enter: () => this.ctrl.socket.realTimeIn(),
      exit: () => this.ctrl.socket.realTimeOut(),
    },
    correspondence: {
      i18n: 'Correspondence',
      enter: () => xhr.seeks().then(this.ctrl.setSeeks),
    },
    playing: { i18n: 'Playing' },
    recent: { i18n: 'Recent' },
    tournament: {},
    quick: {},
  };

  constructor(readonly ctrl: LobbyController) {
    if (!this.app.key) this.app.set('quick');
    const mapTabs = (tabs: TabKey[]) =>
      new Map(tabs.map(tab => [tab, this.tabs[tab]!]) as [TabKey, SecondaryTab][]);

    if (!ctrl.me?.isBot) {
      this.app.add('quick', new Switch({}));

      const lobbySwitch = new Switch({
        storage: 'lobby.tab.lobby',
        items: mapTabs(['realtime', 'correspondence']),
      });
      if (!lobbySwitch.key) lobbySwitch.set('realtime');

      this.app.add('lobby', lobbySwitch);

      if (ctrl.me) this.app.add('tournament', new Switch({}));
    }
    if (ctrl.me) {
      const yourSwitch = new Switch({
        storage: 'lobby.tab.your',
        items: mapTabs(ctrl.data.nbNowPlaying ? ['playing', 'recent'] : ['recent']),
      });
      if (!yourSwitch.key) yourSwitch.set('recent');

      this.app.add('your', yourSwitch);
      if (ctrl.me?.isBot) this.app.set('your');
    }
    if (!this.app.key) this.app.set('quick');
  }

  get primary() {
    return this.app.key;
  }

  get active() {
    return this.app.value?.key || this.app.key || 'lobby';
  }

  get showingHooks() {
    return this.active === 'realtime'; // || this.active === 'variant';
  }

  get primaries(): [TabKey, string][] {
    const primaryI18n: [AppTabKey, string][] = [
      ['quick', 'Quick'],
      ['lobby', 'Lobby'],
      ['tournament', 'Tournament'],
      ['your', 'Your games'],
    ];
    return primaryI18n.filter(([k]) => this.app.items.has(k));
  }

  get secondaries() {
    return [...(this.app.value?.items.entries() ?? [])].map(([k, v]) => [k, v.i18n]) as [TabKey, string][];
  }

  isShowing(tab: TabKey) {
    return this.active === tab || this.primary === tab;
  }

  setTab(tab: TabKey) {
    const oldAppKey = this.app.key;
    const oldActiveKey = this.active;
    //console.log(`setTab(tab = '${tab}, oldAppKey = ${oldAppKey}, oldActiveKey = ${oldActiveKey})`);
    if (oldAppKey === tab || oldActiveKey === tab) return;

    if (this.app.items.has(tab as AppTabKey)) {
      this.app.set(tab as AppTabKey);
    } else {
      const secondary = [...this.app.items.values()].find(x => x?.items.has(tab))!;
      this.app.set(this.app.keyOf(secondary));

      secondary.set(tab);
    }
    this.tabs[oldActiveKey]?.exit?.();
    this.tabs[tab]?.enter?.();
  }
}

type SecondaryTab = { i18n?: string; enter?: () => void; exit?: () => void };

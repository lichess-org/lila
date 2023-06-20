import { VNode } from 'snabbdom';

export interface NotifyOpts {
  el: Element;
  data?: NotifyData;
  incoming: boolean;
  isVisible(): boolean;
  updateUnread(nb: number | 'increment'): boolean; // true if changed
  show(): void;
  setNotified(): void;
  pulse(): void;
}

export interface NotifyData {
  pager: Paginator<Notification>;
  unread: number;
  i18n: I18nDict;
}

export interface BumpUnread {}

interface NotificationUser {
  id: string;
  name: string;
  patron?: boolean;
}

interface NotificationContent {
  text: string;
  user?: NotificationUser;
  [key: string]: any;
}

export interface Notification {
  content: NotificationContent;
  type: string;
  read: boolean;
  date: number;
}

export interface Ctrl {
  data(): NotifyData | undefined;
  initiating(): boolean;
  scrolling(): boolean;
  update(data: NotifyData): void;
  bumpUnread(): void;
  nextPage(): void;
  previousPage(): void;
  loadPage(page: number): void;
  onShow(): void;
  setMsgRead(user: string): void;
  setAllRead(): void;
  clear(): void;
}

export type Redraw = () => void;

export interface Renderers {
  [key: string]: Renderer;
}

export interface Renderer {
  html(n: Notification): VNode;
  text(n: Notification): string;
}

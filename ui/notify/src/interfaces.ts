import { VNode } from 'snabbdom';

export interface NotifyOpts {
  data?: NotifyData;
  incoming: boolean;
  isVisible(): boolean;
  setCount(nb: number): void;
  show(): void;
  setNotified(): void;
  pulse(): void;
}

export interface NotifyData {
  pager: Paginator<Notification>;
  unread: number;
  i18n: I18nDict;
}

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
  update(data: NotifyData, incoming: boolean): void;
  data(): NotifyData | undefined;
  initiating(): boolean;
  scrolling(): boolean;
  nextPage(): void;
  previousPage(): void;
  loadPage(page: number): void;
  setVisible(): void;
  setMsgRead(user: string): void;
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

import type { VNode } from 'snabbdom';

export type NotifyOpts = {
  el: Element;
  data?: NotifyData;
  incoming: boolean;
  isVisible(): boolean;
  updateUnread(nb: number | 'increment'): boolean; // true if changed
  show(): void;
  setNotified(): void;
  pulse(): void;
};

export type NotifyData = {
  pager: Paginator<Notification>;
  unread: number;
};

export type BumpUnread = object;

type NotificationUser = {
  id: string;
  name: string;
  patron?: boolean;
};

type NotificationContent = {
  text: string;
  user?: NotificationUser;
  [key: string]: any;
};

export type Notification = {
  content: NotificationContent;
  type: string;
  read: boolean;
  date: number;
};

export type Ctrl = {
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
};

export type Redraw = () => void;

export type Renderers = Record<string, Renderer>;

export type Renderer = {
  html(n: Notification): VNode;
  text(n: Notification): string;
};

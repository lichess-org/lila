import { VNode } from 'snabbdom/vnode';

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
}

interface NotificationUser {
  id: string;
  name: string;
  patron?: boolean;
}

interface NotificationContent {
  text: string;
  user: NotificationUser;
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
}

export type Redraw = () => void;

export interface Renderers {
  [key: string]: Renderer;
}

interface Renderer {
  html(n: Notification): VNode;
  text(n: Notification): string;
}

export interface MsgOpts {
  data: MsgData;
  i18n: any;
}
export interface MsgData {
  me: User;
  threads: Thread[];
  convo?: Convo;
}
export interface Thread {
  id: string;
  contact: User;
  lastMsg?: LastMsg;
}
export interface User {
  id: string;
  name: string;
  title?: string;
  patron: boolean;
  online: boolean;
}
export interface LastMsg extends BaseMsg {
  read?: boolean;
}
export interface ConvoMsg extends BaseMsg {
  id: string;
}
export interface Msg extends ConvoMsg {
  thread: string;
}
export interface BaseMsg {
  user: string;
  text: string;
  date: Date;
}
export interface Convo {
  thread: Thread;
  msgs: ConvoMsg[];
}

export interface Daily {
  date: Date;
  msgs: ConvoMsg[][];
}

export type Redraw = () => void;

export interface MsgOpts {
  data: MsgData;
  i18n: any;
}
export interface MsgData {
  me: User;
  contacts: Contact[];
  convo?: Convo;
}
export interface Contact {
  user: User;
  lastMsg?: LastMsg;
}
export interface User {
  id: string;
  name: string;
  title?: string;
  patron: boolean;
  online: boolean;
}
export interface Msg {
  user: string;
  text: string;
  date: Date;
}
export interface LastMsg extends Msg {
  read: boolean;
}
export interface Convo {
  user: User;
  msgs: Msg[];
  relations: Relations;
  postable: boolean;
}

export interface Relations {
  in?: boolean;
  out?: boolean;
}

export interface Daily {
  date: Date;
  msgs: Msg[][];
}

export interface SearchRes {
  contacts: Contact[];
  friends: User[];
  users: User[];
}

export type Redraw = () => void;

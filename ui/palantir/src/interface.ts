export type State =
  | 'off'
  | 'opening'
  | 'getting-media'
  | 'ready'
  | 'calling'
  | 'answering'
  | 'getting-stream'
  | 'on'
  | 'stopping';

export interface PalantirOpts {
  uid: string;
  redraw(): void;
}

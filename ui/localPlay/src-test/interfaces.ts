export interface BvbOpts {}

export interface CgHost {
  cgUserMove(orig: Key, dest: Key): void;
  cgOpts(withFen: boolean): CgConfig;
}

// these used to be enums in index.d.ts, had to instantiate them because enum values
// cannot be imported by isolated modules

export const Coords = {
  Hidden: 0,
  Inside: 1,
  Outside: 2,
  All: 3,
};
export type Coords = (typeof Coords)[keyof typeof Coords];

export const AutoQueen = {
  Never: 1,
  OnPremove: 2,
  Always: 3,
};
export type AutoQueen = (typeof AutoQueen)[keyof typeof AutoQueen];

export const ShowClockTenths = {
  Never: 0,
  Below10Secs: 1,
  Always: 2,
};
export type ShowClockTenths = (typeof ShowClockTenths)[keyof typeof ShowClockTenths];

export const ShowResizeHandle = {
  Never: 0,
  OnlyAtStart: 1,
  Always: 2,
};
export type ShowResizeHandle = (typeof ShowResizeHandle)[keyof typeof ShowResizeHandle];

export const MoveEvent = {
  Click: 0,
  Drag: 1,
  ClickOrDrag: 2,
};
export type MoveEvent = (typeof MoveEvent)[keyof typeof MoveEvent];

export const Replay = {
  Never: 0,
  OnlySlowGames: 1,
  Always: 2,
};
export type Replay = (typeof Replay)[keyof typeof Replay];

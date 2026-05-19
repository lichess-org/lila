export function isLandscapeLayout() {
  return isAtLeastXSmall() || window.innerWidth > window.innerHeight;
}

export const isAtLeastXXSmall = (w = window.innerWidth) => w >= 500; // $mq-xx-small
export const isAtLeastXSmall = (w = window.innerWidth) => w >= 650; // $mq-x-small
export const isAtLeastSmall = (w = window.innerWidth) => w >= 800; // $mq-small

import type { VNodeData } from 'lib/view';

import type { RelayTeamName } from './interfaces';

export const broadcasterDeepLink = (url: string): string => {
  const parsed = new URL(url);
  return 'lichess-broadcaster:/' + parsed.pathname;
};

export const teamLinkData = (teamName: RelayTeamName): VNodeData => ({
  attrs: {
    href: `#team-results/${encodeURIComponent(teamName)}`,
  },
});

import type { POVTeamMatch } from './interfaces';

export const finishedTeamMatchCount = (matches: POVTeamMatch[]): number =>
  matches.filter(match => match.points !== undefined).length;

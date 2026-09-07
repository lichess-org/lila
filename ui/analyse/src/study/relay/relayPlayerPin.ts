import { myUserId } from 'lib';
import { storedMap } from 'lib/storage';

import type { ChapterPreview, StudyPlayer } from '../interfaces';
import type { TourId } from './interfaces';
import { playerId } from './playerId';
import { type RelayPlayerId } from './relayPlayers';

export default class RelayPlayerPin {
  private readonly pins = new Set<RelayPlayerId>();
  private readonly store = storedMap<RelayPlayerId[]>(`relay.players.pins.${myUserId()}`, 50, () => []);

  constructor(
    private readonly tourId: TourId,
    private readonly redraw: () => void,
  ) {
    this.pins = new Set(this.store(this.tourId));
  }

  isPinned: (id: RelayPlayerId | undefined) => boolean = id => id !== undefined && this.pins.has(id);

  isPlayerPinned: (p: StudyPlayer) => boolean = p => this.isPinned(playerId(p));

  isChapterPinned: (c: ChapterPreview) => boolean = c =>
    this.anyPinned() &&
    !!c.players &&
    (this.isPlayerPinned(c.players.white) || this.isPlayerPinned(c.players.black));

  anyPinned: () => boolean = () => this.pins.size > 0;

  togglePin: (id: RelayPlayerId) => void = id => {
    if (this.pins.has(id)) this.pins.delete(id);
    else this.pins.add(id);
    this.save();
    this.redraw();
  };

  private save() {
    this.store(this.tourId, Array.from(this.pins));
  }
}

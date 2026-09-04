// The combined FIDE list has a "inactive" flag iff the player is inactive in the standard time control.
db.fide_player.updateMany({ inactive: true }, { $set: { inactive: ['standard'] } });
db.fide_player.updateMany({ inactive: { $not: { $type: 'array' } } }, { $set: { inactive: [] } });

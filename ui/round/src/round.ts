export function firstPly(d) {
  return d.steps[0].ply;
}

export function lastPly(d) {
  return d.steps[d.steps.length - 1].ply;
}

export function plyStep(d, ply) {
  return d.steps[ply - firstPly(d)];
}

export function merge(old, cfg) {
  var data = cfg;

  if (data.clock) {
    data.clock.showTenths = data.pref.clockTenths;
    data.clock.showBar = data.pref.clockBar;
  }

  if (data.correspondence)
  data.correspondence.showBar = data.pref.clockBar;

  if (['horde', 'crazyhouse'].indexOf(data.game.variant.key) !== -1)
  data.pref.showCaptured = false;

  var changes: any = {};
  if (old.opponent) {
    if (!old.opponent.offeringDraw && cfg.opponent.offeringDraw)
    changes.drawOffer = true;
    if (!old.opponent.proposingTakeback && cfg.opponent.proposingTakeback)
    changes.takebackOffer = true;
    if (!old.opponent.offeringRematch && cfg.opponent.offeringRematch)
    changes.rematchOffer = true;
  }

  return {
    data,
    changes
  };
};

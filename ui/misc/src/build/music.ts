import { assetUrl } from 'common/assets';

function lishogiOrchestra() {
  const soundDir = assetUrl('sound/instrument/', { noVersion: true });

  const makeSoundPair = (sound: string) => {
    return [soundDir + sound + '.ogg', soundDir + sound + '.mp3'];
  };

  const instruments: Record<string, any[]> = {
      celesta: [],
      clav: [],
      swells: [],
    },
    noteOverlap = 15,
    noteTimeout = 300,
    maxPitch = 23;
  let currentNotes = 0;

  // load celesta and clav sounds
  for (let i = 1; i <= 24; i++) {
    let fn: string;
    if (i > 9) fn = 'c0' + i;
    else fn = 'c00' + i;
    instruments.celesta.push(
      new window.Howl({
        src: makeSoundPair('celesta/' + fn),
        volume: 0.3,
      }),
    );
    instruments.clav.push(
      new window.Howl({
        src: makeSoundPair('clav/' + fn),
        volume: 0.2,
      }),
    );
  }
  // load swell sounds
  for (let i = 1; i <= 3; i++) {
    instruments.swells.push(
      new Howl({
        src: makeSoundPair('swells/swell' + i),
        volume: 0.5,
      }),
    );
  }

  const play = (instrument, pitch) => {
    pitch = Math.round(Math.max(0, Math.min(maxPitch, pitch)));
    if (instrument === 'swells') pitch = Math.floor(pitch / 8);
    if (currentNotes < noteOverlap) {
      currentNotes++;
      instruments[instrument][pitch].play();
      setTimeout(() => {
        currentNotes--;
      }, noteTimeout);
    }
  };

  play('swells', 0);

  return {
    play: play,
  };
}

function playMusic(): { jump: (node: Tree.Node) => void } {
  const orchestra = lishogiOrchestra();

  const isPawn = notation => {
    return notation && (notation.includes('P') || notation.includes('歩'));
  };

  // support 12x12 board
  const rankToInt = file => {
    return 'abcdefghijkl'.indexOf(file);
  };

  // 7f = (7 - 1) * 12 + 5 = 77
  const keyToInt = (key: string): number => {
    return (parseInt(key[0]) - 1) * 12 + rankToInt(key[1]);
  };

  const usiBase = 122,
    keyToPitch = (key: string): number => {
      return keyToInt(key) / (usiBase / 23);
    };

  const jump = (node: Tree.Node) => {
    if (node.usi) {
      const pitch = keyToPitch(node.usi.slice(2)),
        instrument = isPawn(node.notation) ? 'clav' : 'celesta';
      orchestra.play(instrument, pitch);
      if (node.check) orchestra.play('swells', pitch);
      else if (node.capture) {
        orchestra.play('swells', pitch);
        const capturePitch = keyToPitch(node.usi.slice(0, 2));
        orchestra.play(instrument, capturePitch);
      }
    } else {
      orchestra.play('swells', 0);
    }
  };

  return {
    jump,
  };
}

window.lishogi.registerModule(__bundlename__, playMusic);

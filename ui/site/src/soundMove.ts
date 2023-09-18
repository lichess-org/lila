export async function initModule(): Promise<SoundMove> {
  let currentNotes = 0;

  const volumes: { [instrument: string]: number } = {
      celesta: 0.3,
      clav: 0.2,
      swells: 0.8,
    },
    noteOverlap = 15,
    noteTimeout = 300,
    maxPitch = 23,
    uciBase = 64;

  const play = (instrument = 'swells', pitch = Math.random() * 24) => {
    pitch = Math.round(Math.max(0, Math.min(maxPitch, pitch)));
    if (instrument === 'swells') pitch = Math.floor(pitch / 8);
    if (currentNotes < noteOverlap) {
      currentNotes++;
      lichess.sound.play(`orchestra.${instrument}.${pitch}`, volumes[instrument]);
      setTimeout(() => {
        currentNotes--;
      }, noteTimeout);
    }
  };

  const load = async (instrument: string, index: number, filename: string) =>
    lichess.sound.load(
      `orchestra.${instrument}.${index}`,
      `${lichess.sound.baseUrl}/instrument/${instrument}/${filename}`,
    );

  const isPawn = (san: string) => san[0] === san[0].toLowerCase();
  const isKing = (san: string) => san[0] === 'K';
  const hasCastle = (san: string) => san.startsWith('O-O');
  const hasCheck = (san: string) => san.includes('+');

  const hasMate = (san: string) => san.includes('#');
  const hasCapture = (san: string) => san.includes('x');
  const fileToInt = (file: string) => 'abcdefgh'.indexOf(file);
  const keyToInt = (key: string) => fileToInt(key[0]) * 8 + parseInt(key[1]) - 1;
  const keyToPitch = (key: string) => keyToInt(key) / (uciBase / 23);

  const promises = [];
  for (const inst of ['celesta', 'clav']) {
    for (let i = 1; i <= 24; i++) {
      promises.push(load(inst, i - 1, 'c' + `${i}`.padStart(3, '0')));
    }
  }
  for (let i = 1; i <= 3; i++) promises.push(load('swells', i - 1, `swell${i}`));

  await Promise.all(promises);

  return o => {
    if (o?.filter === 'game') return;
    if (o?.san && o.uci) {
      const pitch = keyToPitch(o.uci!.slice(2));
      const instrument = isPawn(o.san) || isKing(o.san) ? 'clav' : 'celesta';
      play(instrument, pitch);
      if (hasCastle(o.san)) play('swells', pitch);
      else if (hasCheck(o.san)) play('swells', pitch);
      else if (hasCapture(o.san)) {
        play('swells', pitch);
        const capturePitch = keyToPitch(o.uci!.slice(0, 2));
        play(instrument, capturePitch);
      } else if (hasMate(o.san)) play('swells', pitch);
    } else play();
  };
}

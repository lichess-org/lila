export const formatMs = (msTime: number): string => {
  const date = new Date(Math.max(0, msTime + 500)),
    hours = date.getUTCHours(),
    minutes = date.getUTCMinutes(),
    seconds = date.getUTCSeconds();
  return hours > 0 ? hours + ':' + pad(minutes) + ':' + pad(seconds) : minutes + ':' + pad(seconds);
};

export const otbClockIsRunning = (fen: string): boolean => !fen.includes('PPPPPPPP/RNBQKBNR');

export const lichessClockIsRunning = (fen: string, color: Color): boolean =>
  color == 'white' ? !fen.includes('PPPPPPPP/RNBQKBNR') : !fen.startsWith('rnbqkbnr/pppppppp');

const pad = (x: number) => (x < 10 ? '0' : '') + x;

function getPlayer(data, color) {
  if (data.player.color == color) return data.player;
  if (data.opponent.color == color) return data.opponent;
  return null;
}

module.exports = {
  getPlayer: getPlayer
};

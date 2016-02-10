var variantConfirms = {
  'chess960': "This is a Chess960 game!\n\nThe starting position of the pieces on the players' home ranks is randomized.",
  'kingOfTheHill': "This is a King of the Hill game!\n\nThe game can be won by bringing the king to the center.",
  'threeCheck': "This is a Three-check game!\n\nThe game can be won by checking the opponent 3 times.",
  "antichess": "This is an antichess chess game!\n\nIf you can take a piece, you must. The game can be won by losing all your pieces.",
  "atomic": "This is an atomic chess game!\n\nCapturing a piece causes an explosion, taking out your piece and surrounding non-pawns. Win by mating or exploding your opponent's king.",
  "horde": "This is a horde chess game!\n\Black must take all white pawns to win. White must checkmate the black king.",
  "racingKings": "This is a racing kings game!\n\nPlayers must race their kings to the eighth rank. Checks are not allowed.",
  "crazyhouse": "This is a crazyhouse game!\n\nEvery time a piece is captured the capturing player gets a piece of the same type and of their color in their pocket."
};

function storageKey(key) {
  return 'lobby.variant.' + key;
}

module.exports = {
  confirm: function(variant) {
    return Object.keys(variantConfirms).every(function(key) {
      var v = variantConfirms[key]
      if (variant === key && !lichess.storage.get(storageKey(key))) {
        var c = confirm(v);
        if (c) lichess.storage.set(storageKey(key), 1);
        return c;
      } else return true;
    })
  }
};

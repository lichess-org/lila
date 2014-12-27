var variantConfirms = {
  '960': "This is a Chess960 game!\n\nThe starting position of the pieces on the players' home ranks is randomized.\nRead more: http://wikipedia.org/wiki/Chess960\n\nDo you want to play Chess960?",
  'KotH': "This is a King of the Hill game!\n\nThe game can be won by bringing the king to the center.\nRead more: http://lichess.org/king-of-the-hill",
  '3+': "This is a Three-check game!\n\nThe game can be won by checking the opponent 3 times.\nRead more: http://en.wikipedia.org/wiki/Three-check_chess",
  "Anti": "This is an antichess chess game!\n\n If you can take a piece, you must. The game can be won by losing all your pieces."
};

function storageKey(key) {
  return 'lichess.variant.' + key;
}

module.exports = {
  confirm: function(variant) {
    return Object.keys(variantConfirms).every(function(key) {
      var v = variantConfirms[key]
      if (variant === key && !storage.get(storageKey(key))) {
        var c = confirm(v);
        if (c) storage.set(storageKey(key), 1);
        return c;
      } else return true;
    })
  }
};

// https://github.com/ornicar/scalachess/blob/master/src/main/scala/Status.scala

var ids = {
  created: 10,
  started: 20,
  aborted: 25,
  mate: 30,
  resign: 31,
  stalemate: 32,
  timeout: 33,
  draw: 34,
  outoftime: 35,
  cheat: 36,
  noStart: 37,
  variantEnd: 60
};

function started(data) {
  return data.game.status.id >= ids.started;
}

function finished(data) {
  return data.game.status.id >= ids.mate;
}

function aborted(data) {
  return data.game.status.id == ids.aborted;
}


module.exports = {
  ids: ids,
  started: started,
  finished: finished,
  aborted: aborted
};

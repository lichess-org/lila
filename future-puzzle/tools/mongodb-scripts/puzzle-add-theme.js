puzzleDb = connect(`mongodb://localhost:27317/puzzler`);
ids = ''.split(' ');
ids.forEach(id => {
  puzzleDb.puzzle2_round.updateOne(
    { _id: 'lichess:' + id },
    {
      $push: { t: '+vukovicMate' },
    },
  );
});
puzzleDb.puzzle2_puzzle.updateMany({ _id: { $in: ids } }, { $set: { dirty: true } });

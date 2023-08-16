const perf = 'rapid';

const ratings = {
  STL_Caruana: 2773,
  STL_Dominguez: 2786,
  STL_So: 2741,
  STL_Carlsen: 2881,
  'STL_Vachier-Lagrave': 2860,
  STL_Grischuk: 2784,
  STL_Aronian: 2778,
  STL_Xiong: 2730,
};

for (k of Object.keys(ratings)) {
  const rating = ratings[k];
  const id = k.toLowerCase();
  const user = db.user4.findOne({ _id: id });
  if (user.perfs[perf] && user.perfs[perf].nb) {
    const set = { [`perfs.${perf}.gl.r`]: rating };
    const push = {
      [`perfs.${perf}.re`]: {
        $each: [NumberInt(rating)],
        $position: 0,
      },
    };
    db.user4.update({ _id: id }, { $set: set, $push: push });
  } else {
    db.user4.update(
      { _id: id },
      {
        $set: {
          [`perfs.${perf}`]: {
            gl: {
              r: rating,
              d: 150,
              v: 0.06,
            },
            nb: NumberInt(0),
            re: [],
          },
        },
      },
    );
  }
}

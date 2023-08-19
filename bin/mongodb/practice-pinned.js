const ids =
  'BJy6fEDf fE4k21MW 8yadFPpU PDkQDt6u 96Lij7wH Rg2cMBZ6 9ogFv8Ac tuoBxVE5 Qj281y1p MnsJEWnI RUQASaZm o734CNqp ITWY4GN2 9cKgYrHb g1fxVZu9 s5pLU7Of xebrDvFe A4ujYOer pt20yRkT MkDViieT 9c6GrCTk Z1DKk4Rl'.split(
    ' ',
  );

ids.forEach(id => {
  var study = db.study.findOne({ _id: id });
  print(`${study._id} ${study.name}`);
  db.study_chapter
    .find({
      studyId: id,
      'root.co': { $exists: 1 },
    })
    .forEach(chapter => {
      print('  - ' + chapter._id + ' ' + chapter.name);
      db.study_chapter.update(
        { _id: chapter._id },
        { $set: { description: chapter.root.co[0].text }, $unset: { 'root.co': 1 } },
      );
    });
});

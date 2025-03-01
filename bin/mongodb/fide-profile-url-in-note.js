// convert FIDE profile URL from http://ratings.fide.com/card.phtml?event={playerId}
// to https://ratings.fide.com/profile/{playerId}
db.note.find({ text: /card\.phtml/, mod: true }).forEach(note => {
  const newText = note.text.replace(/card\.phtml\?event=(\d+)/g, 'profile/$1');
  db.note.updateOne({ _id: note._id }, { $set: { text: newText } });
});

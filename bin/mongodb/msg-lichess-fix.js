let count = 0;
const badText = 'msg:lichobileNewAppAvailable\n\nmsg:lichobileNewAppDownload';
const replaceMsgText = {
  $set: {
    text: 'There is a new Lichess mobile app available!\n\nPlease download the new official Lichess app at lichess.org/app.',
  },
};
db.msg_thread.find({ _id: /^lichess\//, 'lastMsg.text': badText }).forEach(function (t) {
  print(count++);
  db.msg_msg.updateMany(
    {
      tid: t._id,
      user: 'lichess',
      text: badText,
    },
    replaceMsgText,
  );
  db.msg_thread.updateOne(
    { _id: t._id },
    { $set: { 'lastMsg.text': 'There is a new Lichess mobile app available!' } },
  );
});

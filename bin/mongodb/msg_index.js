db.msg_thread.ensureIndex({users:1,'lastMsg.date':-1});
db.msg_msg.ensureIndex({thread:1,date:-1})

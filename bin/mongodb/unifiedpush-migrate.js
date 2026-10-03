const result = db.push_subscription.deleteMany({
  $expr: {
    $eq: [{ $strLenCP: '$_id' }, 64],
  },
});

print(result.deletedCount + ' Unified Push subscriptions deleted');

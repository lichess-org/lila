const lccUrl = (id, round) => `https://view.livechesscloud.com/#${id}/${round}`;

db.relay
  .find({ 'sync.upstream.lcc': { $exists: 1 } })
  .sort({ $natural: -1 })
  .forEach(relay => {
    db.relay.updateOne(
      { _id: relay._id },
      {
        $set: {
          'sync.upstream': { url: lccUrl(relay.sync.upstream.lcc, relay.sync.upstream.round) },
        },
      },
    );
  });

db.relay
  .find({ 'sync.upstream.urls': { $exists: 1 } })
  .sort({ $natural: -1 })
  .forEach(relay => {
    db.relay.updateOne(
      { _id: relay._id },
      {
        $set: {
          'sync.upstream.urls': relay.sync.upstream.urls
            .filter(u => !!u)
            .map(url => (url.lcc ? lccUrl(url.lcc, url.round) : url.url)),
        },
      },
    );
  });

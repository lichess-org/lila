// MongoDB initialization script for Lichess
db = db.getSiblingDB('lichess');

// Create lichess user
db.createUser({
    user: 'lichess',
    pwd: 'password',
    roles: [
        {
            role: 'readWrite',
            db: 'lichess'
        }
    ]
});

// Create basic collections with indexes
db.createCollection('game5');
db.createCollection('user4');
db.createCollection('seek');
db.createCollection('lobby_room');
db.createCollection('puzzle2_puzzle');
db.createCollection('tournament2');
db.createCollection('study');
db.createCollection('forum_post');
db.createCollection('chat');
db.createCollection('security');
db.createCollection('relation');
db.createCollection('pref');
db.createCollection('bookmark');
db.createCollection('crosstable2');
db.createCollection('fishnet_analysis');
db.createCollection('fishnet_client');

// Create indexes for performance
// Game indexes
db.game5.createIndex({ 'us': 1 });
db.game5.createIndex({ 'ca': 1 });
db.game5.createIndex({ 'ra': 1 });
db.game5.createIndex({ 'st': 1 });
db.game5.createIndex({ 'us': 1, 'ca': 1 });

// User indexes
db.user4.createIndex({ 'username': 1 }, { unique: true });
db.user4.createIndex({ 'email': 1 }, { sparse: true });

// Seek indexes
db.seek.createIndex({ 'user': 1 });
db.seek.createIndex({ 'createdAt': 1 });

// Puzzle indexes
db.puzzle2_puzzle.createIndex({ 'id': 1 }, { unique: true });
db.puzzle2_puzzle.createIndex({ 'themes': 1 });
db.puzzle2_puzzle.createIndex({ 'rating': 1 });

// Tournament indexes
db.tournament2.createIndex({ 'startsAt': 1 });
db.tournament2.createIndex({ 'status': 1 });
db.tournament2.createIndex({ 'createdBy': 1 });

// Study indexes
db.study.createIndex({ 'ownerId': 1 });
db.study.createIndex({ 'createdAt': 1 });
db.study.createIndex({ 'visibility': 1 });

// Forum indexes
db.forum_post.createIndex({ 'categId': 1 });
db.forum_post.createIndex({ 'topicId': 1 });
db.forum_post.createIndex({ 'createdAt': 1 });

// Chat indexes
db.chat.createIndex({ '_id': 1 });
db.chat.createIndex({ 'date': 1 });

// Security indexes
db.security.createIndex({ 'user': 1 });
db.security.createIndex({ 'date': 1 });

// Relation indexes
db.relation.createIndex({ 'u1': 1, 'u2': 1 }, { unique: true });
db.relation.createIndex({ 'u2': 1 });

// Preference indexes
db.pref.createIndex({ '_id': 1 }, { unique: true });

// Bookmark indexes
db.bookmark.createIndex({ 'u': 1 });
db.bookmark.createIndex({ 'g': 1 });

// Crosstable indexes
db.crosstable2.createIndex({ '_id': 1 }, { unique: true });

// Fishnet indexes
db.fishnet_analysis.createIndex({ 'acquired': 1 });
db.fishnet_analysis.createIndex({ 'sender.system': 1 });
db.fishnet_client.createIndex({ 'userId': 1 });

print('MongoDB initialization completed successfully'); 
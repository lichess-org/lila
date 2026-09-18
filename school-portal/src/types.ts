export type LevelStatus = 'completed' | 'active' | 'locked';

export type Level = {
    id: number;
    title: string;
    theme: string;
    status: LevelStatus;
    puzzleId: string;
    rewardCoins: number;
    rewardXp: number;
};

export type Puzzle = {
    id: string;
    fen: string;
    moves: string[];
    rating: number;
    hint: string;
    category: 'Мат' | 'Вилка' | 'Атака';
};

export type User = {
    username: string;
    level: number;
    xp: number;
    coins: number;
    completedLevels: number[];
    matchesPlayed: number;
};

export type SchoolMember = {
    username: string;
    level: number;
    rating: number;
    online: boolean;
    status: string;
};

export type SchoolGame = {
    id: number;
    white: string;
    black: string;
    timeControl: string;
    status: 'open' | 'playing';
};

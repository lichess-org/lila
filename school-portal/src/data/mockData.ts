import type { Level, Puzzle, SchoolGame, SchoolMember, User } from '../types';

export const puzzles: Puzzle[] = [
    {
        id: 'back-rank',
        fen: '6k1/5ppp/8/8/8/8/5PPP/3R2K1 w - - 0 1',
        moves: ['d1d8'],
        rating: 820,
        hint: 'Ищи шах по восьмой горизонтали.',
        category: 'Мат',
    },
    {
        id: 'knight-fork',
        fen: '4k3/8/8/8/8/2N5/8/4K3 w - - 0 1',
        moves: ['c3b5', 'e8d7', 'b5c7'],
        rating: 880,
        hint: 'Конь любит нападать сразу на несколько фигур.',
        category: 'Вилка',
    },
    {
        id: 'queen-mate',
        fen: '6k1/5ppp/8/8/8/6Q1/5PPP/6K1 w - - 0 1',
        moves: ['g3g7'],
        rating: 760,
        hint: 'Ферзь может поставить мат рядом с королём.',
        category: 'Мат',
    },
    {
        id: 'discovered-attack',
        fen: '4k3/8/8/8/8/2B5/8/4R1K1 w - - 0 1',
        moves: ['c3g7', 'e8d7', 'e1e7'],
        rating: 940,
        hint: 'Убери слона с линии и открой атаку ладьи.',
        category: 'Атака',
    },
    {
        id: 'rook-check',
        fen: '4k3/8/8/8/8/8/4R3/4K3 w - - 0 1',
        moves: ['e2e8'],
        rating: 700,
        hint: 'Ладья может ворваться на восьмую горизонталь.',
        category: 'Атака',
    },
    {
        id: 'opponent-first',
        fen: '4k3/8/8/8/4P3/8/8/4K3 b - - 0 1',
        moves: ['e8e7', 'e1e2'],
        rating: 620,
        hint: 'Сначала дождись хода соперника, затем отвечай королём.',
        category: 'Вилка',
    },
];

export const levels: Level[] = [
    { id: 1, title: 'Мат в один', theme: 'Последняя линия', status: 'completed', puzzleId: 'back-rank', rewardCoins: 25, rewardXp: 50 },
    { id: 2, title: 'Тактическая вилка', theme: 'Двойной удар', status: 'active', puzzleId: 'knight-fork', rewardCoins: 25, rewardXp: 50 },
    { id: 3, title: 'Ферзь-охотник', theme: 'Матовая атака', status: 'locked', puzzleId: 'queen-mate', rewardCoins: 30, rewardXp: 60 },
    { id: 4, title: 'Открытая линия', theme: 'Вскрытая атака', status: 'locked', puzzleId: 'discovered-attack', rewardCoins: 35, rewardXp: 70 },
    { id: 5, title: 'Вторжение ладьи', theme: 'Сила проходной линии', status: 'locked', puzzleId: 'rook-check', rewardCoins: 40, rewardXp: 80 },
    { id: 6, title: 'Ответный ход', theme: 'Терпение и расчёт', status: 'locked', puzzleId: 'opponent-first', rewardCoins: 50, rewardXp: 100 },
];

export const initialUser: User = {
    username: 'Иван',
    level: 3,
    xp: 320,
    coins: 150,
    completedLevels: [1],
    matchesPlayed: 18,
};

export const schoolMembers: SchoolMember[] = [
    { username: 'Мария', level: 6, rating: 1240, online: true, status: 'Решает задачу' },
    { username: 'Алексей', level: 4, rating: 1110, online: true, status: 'Готов сыграть' },
    { username: 'София', level: 3, rating: 980, online: true, status: 'В партии' },
    { username: 'Даниил', level: 2, rating: 860, online: false, status: 'Был недавно' },
    { username: 'Елена', level: 5, rating: 1180, online: false, status: 'Нет на месте' },
    { username: 'Никита', level: 1, rating: 720, online: true, status: 'Готов сыграть' },
];

export const schoolGames: SchoolGame[] = [
    { id: 1, white: 'Алексей', black: 'Мария', timeControl: '10 + 0', status: 'playing' },
    { id: 2, white: 'Никита', black: 'Иван', timeControl: '5 + 3', status: 'playing' },
    { id: 3, white: 'София', black: 'Елена', timeControl: '15 + 10', status: 'open' },
];

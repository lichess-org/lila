import { useCallback, useMemo, useState } from 'react';
import { initialUser, levels as initialLevels, puzzles, schoolGames, schoolMembers } from './data/mockData';
import type { Level, SchoolGame, SchoolMember, User } from './types';
import { Profile } from './components/Profile';
import { PuzzleArena } from './components/PuzzleArena';
import { Roadmap } from './components/Roadmap';
import { BotGame } from './components/BotGame';

type Screen = 'play' | 'community' | 'games' | 'puzzles' | 'profile' | 'stats' | 'arena';

const tabs: { id: Exclude<Screen, 'arena'>; label: string; icon: string }[] = [
    { id: 'play', label: 'Играть', icon: '♟' },
    { id: 'community', label: 'Сообщество', icon: '♙' },
    { id: 'games', label: 'Игры', icon: '⚔' },
    { id: 'puzzles', label: 'Задачи', icon: '♞' },
    { id: 'profile', label: 'Профиль', icon: '●' },
    { id: 'stats', label: 'Статистика', icon: '▥' },
];

function Header({ user, screen, onNavigate }: { user: User; screen: Screen; onNavigate: (screen: Screen) => void }) {
    const xpLevel = Math.floor(user.xp / 100);
    return <header className="app-header">
        <button className="brand" onClick={() => onNavigate('play')} aria-label="На главную"><span className="brand-mark">♞</span><span><strong>ШКОЛА</strong><small>ШАХМАТНЫЙ КАБИНЕТ</small></span></button>
        <nav className="main-nav" aria-label="Основная навигация">{tabs.map(tab => <button key={tab.id} className={screen === tab.id || (screen === 'arena' && (tab.id === 'play' || tab.id === 'puzzles')) ? 'nav-active' : ''} onClick={() => onNavigate(tab.id)}><span className="nav-icon">{tab.icon}</span><span>{tab.label}</span></button>)}</nav>
        <div className="header-stats"><span className="header-level">УР. <strong>{xpLevel}</strong></span><span className="header-coins">🪙 <strong>{user.coins}</strong></span><button className="header-avatar" onClick={() => onNavigate('profile')}>И</button></div>
    </header>;
}

function PlayHome() {
    const [elo, setElo] = useState(1000);
    return <BotGame elo={elo} onEloChange={setElo} />;
}

function Community({ members }: { members: SchoolMember[] }) {
    const [onlineOnly, setOnlineOnly] = useState(true);
    const [notice, setNotice] = useState('');
    const visible = [...(onlineOnly ? members.filter(member => member.online) : members)].sort((a, b) => b.rating - a.rating);
    return <main className="workspace-screen"><section className="workspace-title"><div><span className="eyebrow">РЕЙТИНГ ШКОЛЫ</span><h1>Сообщество</h1><p className="workspace-lead">Ученики школы отсортированы по рейтингу. Зелёная точка показывает активных на сайте.</p></div><span className="member-count"><strong>{members.filter(member => member.online).length}</strong> онлайн</span></section><div className="workspace-toolbar"><button className={onlineOnly ? 'filter-active' : ''} onClick={() => setOnlineOnly(true)}>Онлайн</button><button className={!onlineOnly ? 'filter-active' : ''} onClick={() => setOnlineOnly(false)}>Все ученики</button><span>Рейтинг · {visible.length} участников</span></div>{notice && <div className="action-notice">{notice}</div>}<section className="leaderboard">{visible.map((member, index) => <article className="leaderboard-row" key={member.username}><strong className="rank">{index + 1}</strong><span className={`member-avatar ${member.online ? 'online' : ''}`}>{member.username[0]}</span><div className="member-info"><strong>{member.username}</strong><span>Ученик · уровень {member.level}</span></div><span className="member-status-dot" title={member.online ? 'Онлайн' : 'Не в сети'} /><strong className="leader-rating">{member.rating}</strong><button className="outline-action" onClick={() => setNotice(`Приглашение отправлено ученику ${member.username}.`)}>Играть</button></article>)}</section></main>;
}

function SchoolGames({ games }: { games: SchoolGame[] }) {
    const [notice, setNotice] = useState('');
    return <main className="workspace-screen"><section className="workspace-title"><div><span className="eyebrow">ВНУТРИ ШКОЛЫ</span><h1>Игры учеников</h1><p className="workspace-lead">Наблюдай за партиями или присоединяйся к свободной доске.</p></div><button className="primary-action" onClick={() => setNotice('Твоя доска создана. Ждём соперника.')}>+ Создать игру</button></section>{notice && <div className="action-notice">{notice}</div>}<section className="game-list">{games.map(game => <article className="game-row" key={game.id}><span className={`game-state ${game.status}`} /><div className="game-players"><strong>{game.white}</strong><span> против </span><strong>{game.black}</strong></div><span className="game-time">{game.timeControl}</span><span className={`game-label ${game.status}`}>{game.status === 'open' ? 'Открыта' : 'Идёт игра'}</span><button className="outline-action" onClick={() => setNotice(game.status === 'open' ? `Ты присоединился к игре ${game.white} — ${game.black}.` : `Открываем наблюдение за игрой ${game.white} — ${game.black}.`)}>{game.status === 'open' ? 'Играть' : 'Смотреть'}</button></article>)}</section></main>;
}

function Statistics({ user, levels }: { user: User; levels: Level[] }) {
    const completed = user.completedLevels.length;
    const xpIntoLevel = user.xp % 100;
    return <main className="workspace-screen"><section className="workspace-title"><div><span className="eyebrow">ЛИЧНАЯ АНАЛИТИКА</span><h1>Статистика</h1><p className="workspace-lead">Разбор твоей активности: темп, точность и любимые тактические темы.</p></div></section><section className="stats-grid"><article><span>⌁</span><strong>{Math.round(user.xp / 10)}%</strong><small>Точность решений</small></article><article><span>◷</span><strong>7</strong><small>Дней активности</small></article><article><span>♞</span><strong>3</strong><small>Любимая тема: вилки</small></article><article><span>↗</span><strong>+{user.xp - 280}</strong><small>XP за последние 30 дней</small></article></section><section className="stats-detail"><div className="subheading"><div><span className="eyebrow">ДИНАМИКА XP</span><h2>До следующего уровня {100 - xpIntoLevel} XP</h2></div><strong>Уровень {Math.floor(user.xp / 100)}</strong></div><div className="stats-progress"><span style={{ width: `${xpIntoLevel}%` }} /></div><div className="stats-level-line"><span>Уровень {Math.floor(user.xp / 100)}</span><span>{xpIntoLevel} / 100 XP</span><span>Уровень {Math.floor(user.xp / 100) + 1}</span></div></section><section className="stats-breakdown"><div className="subheading"><div><span className="eyebrow">АКТИВНОСТЬ</span><h2>Последние результаты</h2></div></div><div className="result-line"><span>Задачи</span><strong>{completed} решено</strong><i style={{ width: `${completed / levels.length * 100}%` }} /></div><div className="result-line"><span>Партии против бота</span><strong>{user.matchesPlayed} сыграно</strong><i style={{ width: '72%' }} /></div><div className="result-line"><span>Игры с учениками</span><strong>6 сыграно</strong><i style={{ width: '44%' }} /></div></section></main>;
}

export default function App() {
    const [screen, setScreen] = useState<Screen>('play');
    const [user, setUser] = useState(initialUser);
    const [levels, setLevels] = useState(initialLevels);
    const [selectedLevel, setSelectedLevel] = useState<Level | null>(null);
    const selectedPuzzle = useMemo(() => selectedLevel && puzzles.find(puzzle => puzzle.id === selectedLevel.puzzleId), [selectedLevel]);
    const completeLevel = useCallback((level: Level) => { setUser(current => { if (current.completedLevels.includes(level.id)) return current; const xp = current.xp + level.rewardXp; return { ...current, xp, coins: current.coins + level.rewardCoins, level: Math.floor(xp / 100), completedLevels: [...current.completedLevels, level.id] }; }); setLevels(current => current.map(item => item.id === level.id ? { ...item, status: 'completed' } : item.id === level.id + 1 ? { ...item, status: 'active' } : item)); }, []);
    const openLevel = useCallback((level: Level) => { setSelectedLevel(level); setScreen('arena'); }, []);
    const navigate = useCallback((nextScreen: Screen) => { setScreen(nextScreen); if (nextScreen !== 'arena') setSelectedLevel(null); }, []);
    return <div className="app-shell"><Header user={user} screen={screen} onNavigate={navigate} />{screen === 'play' && <PlayHome />}{screen === 'community' && <Community members={schoolMembers} />}{screen === 'games' && <SchoolGames games={schoolGames} />}{screen === 'puzzles' && <Roadmap levels={levels} onSelect={openLevel} />}{screen === 'profile' && <Profile user={user} totalLevels={levels.length} />}{screen === 'stats' && <Statistics user={user} levels={levels} />}{screen === 'arena' && selectedLevel && selectedPuzzle && <PuzzleArena level={selectedLevel} puzzle={selectedPuzzle} onComplete={completeLevel} onBack={() => navigate('puzzles')} />}</div>;
}

import type { User } from '../types';

type ProfileProps = {
    user: User;
    totalLevels: number;
};

export function Profile({ user }: ProfileProps) {
    const currentLevel = Math.floor(user.xp / 100);
    const progress = user.xp % 100;
    return <main className="profile-screen"><section className="profile-cover"><span className="cover-mark">♞</span><span className="cover-grid" /></section><section className="profile-content"><div className="profile-header"><div className="avatar">И<span>В</span></div><div><span className="eyebrow">УЧЕНИК ШКОЛЫ</span><h1>{user.username}</h1><p>Уровень растёт каждые 100 XP за тренировки и партии.</p></div><div className="profile-level"><span>УРОВЕНЬ</span><strong>{currentLevel}</strong></div></div><div className="profile-grid"><div className="profile-card progress-card"><div className="card-top"><span>Прогресс XP</span><strong>{progress} / 100</strong></div><div className="big-progress"><span style={{ width: `${progress}%` }} /></div><p>До уровня {currentLevel + 1} осталось {100 - progress} XP</p></div><div className="profile-card coins-card"><span className="coin-icon">🪙</span><div><span className="card-label">ШКОЛЬНЫЕ МОНЕТЫ</span><strong>{user.coins}</strong><p>Награды за задачи и партии учеников.</p></div></div><div className="profile-card stat-card"><span className="stat-icon">✦</span><div><span className="card-label">ВСЕГО XP</span><strong>{user.xp}</strong><p>Твой общий опыт в школе.</p></div></div><div className="profile-card stat-card"><span className="stat-icon">♟</span><div><span className="card-label">СЫГРАНО ПАРТИЙ</span><strong>{user.matchesPlayed}</strong><p>Бот и ученики школы.</p></div></div></div></section></main>;
}

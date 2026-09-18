import { useMemo, useState } from 'react';
import { puzzles } from '../data/mockData';
import type { Level } from '../types';

type RoadmapProps = { levels: Level[]; onSelect: (level: Level) => void };
type Filter = 'all' | 'Мат' | 'Вилка' | 'Атака';

const categories: Filter[] = ['all', 'Мат', 'Вилка', 'Атака'];

export function Roadmap({ levels, onSelect }: RoadmapProps) {
    const [filter, setFilter] = useState<Filter>('all');
    const items = useMemo(() => levels.map(level => ({ level, puzzle: puzzles.find(puzzle => puzzle.id === level.puzzleId)! })).filter(item => filter === 'all' || item.puzzle.category === filter), [filter, levels]);
    const groups = categories.slice(1).map(category => ({ category, items: items.filter(item => item.puzzle.category === category) })).filter(group => group.items.length);
    return <main className="roadmap-screen"><section className="puzzles-heading"><div><span className="eyebrow">БИБЛИОТЕКА LICHESS</span><h1>Задачи</h1><p>Выбирай тему, рейтинг и позицию. Здесь нет маршрута: только задачи, которые можно решать в любом порядке.</p></div><div className="puzzle-summary"><strong>{puzzles.length}</strong><span>позиций в базе</span></div></section><section className="puzzle-toolbar">{categories.map(category => <button key={category} className={filter === category ? 'filter-active' : ''} onClick={() => setFilter(category)}>{category === 'all' ? 'Все задачи' : category}</button>)}<span className="toolbar-spacer" /><span className="toolbar-meta">{items.length} позиций</span></section><section className="puzzle-groups">{groups.map(group => <section className="puzzle-group" key={group.category}><div className="group-heading"><div><h2>{group.category}</h2><p>{group.category === 'Мат' ? 'Матовые комбинации' : group.category === 'Вилка' ? 'Двойной удар и нападение на несколько фигур' : 'Вскрытие линий и атака короля'}</p></div><span>{group.items.length} задач</span></div><div className="level-grid">{group.items.map(({ level, puzzle }) => <article className="level-card compact-card puzzle-card" key={puzzle.id}><button className="level-card-button" onClick={() => onSelect(level)}><div className="level-card-head"><span className="level-number">{puzzle.category === 'Мат' ? '♛' : puzzle.category === 'Вилка' ? '♞' : '♜'}</span><span className="level-label">{puzzle.category.toUpperCase()}</span><span className="level-status-dot" /></div><h2>{level.title}</h2><p>{level.theme}</p><div className="level-rewards"><span>Рейтинг {puzzle.rating}</span><span>✦ {level.rewardXp} XP</span></div><span className="card-footer">Решать задачу →</span></button></article>)}</div></section>)}</section></main>;
}

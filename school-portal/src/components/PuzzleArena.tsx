import { useCallback, useEffect, useMemo, useState } from 'react';
import { Chess } from 'chess.js';
import type { Level, Puzzle } from '../types';
import { ChessBoard } from './ChessBoard';

type PuzzleArenaProps = {
    level: Level;
    puzzle: Puzzle;
    onComplete: (level: Level) => void;
    onBack: () => void;
};

export function PuzzleArena({ level, puzzle, onComplete, onBack }: PuzzleArenaProps) {
    const [fen, setFen] = useState(puzzle.fen);
    const [moveIndex, setMoveIndex] = useState(0);
    const [message, setMessage] = useState('Твой ход. Найди лучший тактический удар.');
    const [boardVersion, setBoardVersion] = useState(0);
    const [completed, setCompleted] = useState(false);
    const chess = useMemo(() => new Chess(fen), [fen]);
    const playerColor = puzzle.fen.split(' ')[1] === 'b' ? 'black' : 'white';
    const playerStarts = puzzle.fen.split(' ')[1] === (playerColor === 'white' ? 'w' : 'b');
    const playerMoveParity = playerStarts ? 0 : 1;
    const isPlayerTurn = moveIndex < puzzle.moves.length && moveIndex % 2 === playerMoveParity && chess.turn() === (playerColor === 'white' ? 'w' : 'b');
    const progress = Math.min(moveIndex / puzzle.moves.length, 1);

    const playMove = useCallback((uci: string, actor: 'player' | 'opponent') => {
        const next = new Chess(fen);
        const move = next.move({ from: uci.slice(0, 2), to: uci.slice(2, 4), promotion: 'q' });
        if (!move) return;
        const nextIndex = moveIndex + 1;
        setFen(next.fen());
        setMoveIndex(nextIndex);
        setMessage(actor === 'opponent' ? 'Соперник сходил. Теперь твой ответ.' : 'Отлично! Смотри продолжение.');
        if (nextIndex >= puzzle.moves.length) {
            setCompleted(true);
            setMessage('Тактика решена идеально.');
            onComplete(level);
        }
    }, [fen, level, moveIndex, onComplete, puzzle.moves.length]);

    useEffect(() => {
        if (completed || moveIndex >= puzzle.moves.length || isPlayerTurn) return;
        const timer = window.setTimeout(() => playMove(puzzle.moves[moveIndex], 'opponent'), 300);
        return () => window.clearTimeout(timer);
    }, [completed, isPlayerTurn, moveIndex, playMove, puzzle.moves]);

    const handleMove = useCallback((orig: string, dest: string, isLegal: boolean) => {
        const attempted = `${orig}${dest}`;
        const expected = puzzle.moves[moveIndex];
        if (!isLegal || !isPlayerTurn || attempted !== expected) {
            setMessage('Неверный ход, попробуй ещё раз!');
            setBoardVersion(version => version + 1);
            return;
        }
        playMove(attempted, 'player');
    }, [isPlayerTurn, moveIndex, playMove, puzzle.moves]);

    return (
        <main className="arena-screen">
            <button className="back-button" onClick={onBack}>← Все задачи</button>
            <div className="arena-layout">
                <section className="arena-board-panel">
                    <div className="arena-kicker">УРОВЕНЬ {level.id} / ТАКТИКА {puzzle.rating}</div>
                    <h1>{level.title}</h1>
                    <p className="arena-subtitle">{level.theme}</p>
                    <div className="board-frame">
                        <ChessBoard
                            key={boardVersion}
                            fen={fen}
                            orientation={playerColor}
                            onMove={handleMove}
                            interactive={isPlayerTurn && !completed}
                        />
                    </div>
                </section>
                <aside className="arena-sidebar">
                    <div className="panel-heading"><span>Миссия</span><span>{Math.round(progress * 100)}%</span></div>
                    <div className="mission-progress"><span style={{ width: `${Math.max(progress * 100, 4)}%` }} /></div>
                    <div className="mission-card">
                        <span className="mission-icon">♞</span>
                        <div>
                            <strong>Тактическая тренировка</strong>
                            <p>{message}</p>
                        </div>
                    </div>
                    <div className="hint-card"><span>💡</span><p>{puzzle.hint}</p></div>
                    {completed && (
                        <div className="success-banner">
                            <span className="success-mark">✓</span>
                            <div><strong>Уровень пройден!</strong><span>+{level.rewardXp} XP, +{level.rewardCoins} Монет</span></div>
                            <button onClick={onBack}>К карте уровней</button>
                        </div>
                    )}
                </aside>
            </div>
        </main>
    );
}

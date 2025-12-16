import React, { useState } from 'react';
import { saveAnswer } from '../services/api';

function QuestionPage({ projectData, onComplete }) {
    const questions = projectData.questions || [];
    const [answers, setAnswers] = useState({});
    const [currentIndex, setCurrentIndex] = useState(0);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');

    const currentQuestion = questions[currentIndex];
    const progress = ((currentIndex + 1) / questions.length) * 100;

    const handleAnswerChange = (e) => {
        setAnswers(prev => ({
            ...prev,
            [currentQuestion.id]: e.target.value
        }));
    };

    const handleNext = async () => {
        const currentAnswer = answers[currentQuestion.id];

        if (!currentAnswer || currentAnswer.trim() === '') {
            setError('답변을 입력해주세요');
            return;
        }

        setLoading(true);
        setError('');

        try {
            await saveAnswer(currentQuestion.id, currentAnswer);

            if (currentIndex < questions.length - 1) {
                setCurrentIndex(currentIndex + 1);
            } else {
                // 모든 답변 완료
                onComplete();
            }
        } catch (err) {
            console.error('답변 저장 실패:', err);
            setError('답변 저장에 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handlePrevious = () => {
        if (currentIndex > 0) {
            setCurrentIndex(currentIndex - 1);
            setError('');
        }
    };

    return (
        <div className="container">
            <div className="card">
                <h2 className="card-title">질문에 답변해주세요</h2>

                <div className="progress-bar">
                    <div className="progress-fill" style={{ width: `${progress}%` }}></div>
                </div>

                <p style={{ fontSize: '14px', color: '#7f8c8d', marginBottom: '30px' }}>
                    {currentIndex + 1} / {questions.length}
                </p>

                {error && (
                    <div className="alert alert-error">{error}</div>
                )}

                <div className="question-card">
                    <div style={{ marginBottom: '20px' }}>
                        <span className="question-section">{currentQuestion.section}</span>
                    </div>

                    <div style={{ display: 'flex', alignItems: 'flex-start', marginBottom: '20px' }}>
                        <span className="question-number">{currentIndex + 1}</span>
                        <p className="question-text">{currentQuestion.questionText}</p>
                    </div>

                    <textarea
                        className="form-input"
                        value={answers[currentQuestion.id] || ''}
                        onChange={handleAnswerChange}
                        placeholder="답변을 입력해주세요"
                        rows="5"
                    />

                    <p style={{ fontSize: '13px', color: '#95a5a6', marginTop: '8px' }}>
                        간단히 입력하셔도 됩니다. AI가 전문적인 문장으로 작성해드립니다.
                    </p>
                </div>

                <div style={{ display: 'flex', gap: '12px', marginTop: '30px' }}>
                    <button
                        onClick={handlePrevious}
                        className="btn btn-secondary"
                        disabled={currentIndex === 0}
                        style={{ flex: 1 }}
                    >
                        이전
                    </button>

                    <button
                        onClick={handleNext}
                        className="btn btn-primary"
                        disabled={loading}
                        style={{ flex: 2 }}
                    >
                        {loading ? '저장 중...' : currentIndex === questions.length - 1 ? '완료' : '다음'}
                    </button>
                </div>
            </div>
        </div>
    );
}

export default QuestionPage;
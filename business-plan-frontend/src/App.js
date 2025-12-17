import React, { useState } from 'react';
import './App.css';
import ProjectForm from './pages/ProjectForm';
import QuestionPage from './pages/QuestionPage';
import ResultPage from './pages/ResultPage';

function App() {
  const [currentStep, setCurrentStep] = useState('form'); // form, questions, result
  const [projectData, setProjectData] = useState(null);

  const handleProjectCreated = (data) => {
    setProjectData(data);
    setCurrentStep('questions');
  };

  const handleQuestionsCompleted = () => {
    setCurrentStep('result');
  };

  // 진행 단계 정보
  const steps = [
    { id: 'form', label: '사업개요 입력', number: 1 },
    { id: 'questions', label: '질문 답변', number: 2 },
    { id: 'result', label: '완료', number: 3 }
  ];

  const getStepStatus = (stepId) => {
    const stepOrder = ['form', 'questions', 'result'];
    const currentIndex = stepOrder.indexOf(currentStep);
    const stepIndex = stepOrder.indexOf(stepId);

    if (stepIndex < currentIndex) return 'completed';
    if (stepIndex === currentIndex) return 'active';
    return 'pending';
  };

  return (
      <div className="App">
        <header className="header">
          <div className="header-content">
            <h1>사업계획서 작성 시스템</h1>
            <p>사업 실행계획서를 간편하게 작성하세요</p>
          </div>
        </header>

        {/* 🎯 진행 단계 표시 */}
        <div className="container">
          <div className="step-indicator fade-in">
            {steps.map((step, index) => (
                <React.Fragment key={step.id}>
                  <div className="step-item">
                    <div className={`step-number ${getStepStatus(step.id)}`}>
                      {getStepStatus(step.id) === 'completed' ? '✓' : step.number}
                    </div>
                    <div className={`step-text ${getStepStatus(step.id)}`}>
                      {step.label}
                    </div>
                  </div>
                  {index < steps.length - 1 && (
                      <div className="step-arrow">→</div>
                  )}
                </React.Fragment>
            ))}
          </div>
        </div>

        {currentStep === 'form' && (
            <ProjectForm onSuccess={handleProjectCreated} />
        )}

        {currentStep === 'questions' && projectData && (
            <QuestionPage
                projectData={projectData}
                onComplete={handleQuestionsCompleted}
            />
        )}

        {currentStep === 'result' && projectData && (
            <ResultPage projectData={projectData} />
        )}
      </div>
  );
}

export default App;
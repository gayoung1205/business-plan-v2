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

  return (
      <div className="App">
        <header className="header">
          <div className="header-content">
            <h1>사업계획서 작성 시스템</h1>
            <p>사업 실행계획서를 간편하게 작성하세요</p>
          </div>
        </header>

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
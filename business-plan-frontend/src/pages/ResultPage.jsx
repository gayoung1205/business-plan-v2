import React, { useEffect, useState } from 'react';
import { getProject } from '../services/api';

function ResultPage({ projectData }) {
    const [project, setProject] = useState(null);
    const [loading, setLoading] = useState(true);
    const [generating, setGenerating] = useState(false);

    useEffect(() => {
        const fetchProject = async () => {
            try {
                const result = await getProject(projectData.project.id);
                setProject(result.project);
            } catch (err) {
                console.error('프로젝트 조회 실패:', err);
            } finally {
                setLoading(false);
            }
        };

        fetchProject();
    }, [projectData]);

    const handleGeneratePlan = async () => {
        setGenerating(true);

        try {
            const response = await fetch(
                `http://localhost:8080/api/projects/${project.id}/generate`,
                { method: 'POST' }
            );

            const result = await response.json();

            if (result.success) {
                setProject(result.project);
                alert('✅ 사업계획서 생성 완료!');
            } else {
                alert('❌ 생성 실패: ' + result.message);
            }
        } catch (err) {
            console.error('생성 실패:', err);
            alert('사업계획서 생성 중 오류가 발생했습니다');
        } finally {
            setGenerating(false);
        }
    };

    if (loading) {
        return (
            <div className="container">
                <div className="loading">
                    <div className="loading-spinner"></div>
                    <p>사업계획서를 불러오는 중...</p>
                </div>
            </div>
        );
    }

    if (!project) {
        return (
            <div className="container">
                <div className="alert alert-error">프로젝트를 찾을 수 없습니다</div>
            </div>
        );
    }

    const isNotGenerated = !project.detailedPlan && !project.monthlyPlan && !project.expectedEffect;

    return (
        <div className="container">
            <div className="card">
                <h2 className="card-title">사업 실행계획서</h2>

                <div className="alert alert-success">
                    답변이 성공적으로 저장되었습니다
                </div>

                {/* 1. 사업개요 */}
                <div className="result-section">
                    <h3 className="result-section-title">1. 사업개요</h3>
                    <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                        <tbody>
                        <tr style={{ borderBottom: '1px solid #e0e0e0' }}>
                            <td style={{ padding: '12px', background: '#f8f9fa', width: '150px', fontWeight: '500' }}>공동체명</td>
                            <td style={{ padding: '12px' }}>{project.communityName}</td>
                        </tr>
                        <tr style={{ borderBottom: '1px solid #e0e0e0' }}>
                            <td style={{ padding: '12px', background: '#f8f9fa', fontWeight: '500' }}>사업명</td>
                            <td style={{ padding: '12px' }}>{project.projectName}</td>
                        </tr>
                        <tr style={{ borderBottom: '1px solid #e0e0e0' }}>
                            <td style={{ padding: '12px', background: '#f8f9fa', fontWeight: '500' }}>사업기간</td>
                            <td style={{ padding: '12px' }}>{project.projectPeriod}</td>
                        </tr>
                        <tr style={{ borderBottom: '1px solid #e0e0e0' }}>
                            <td style={{ padding: '12px', background: '#f8f9fa', fontWeight: '500' }}>사업위치</td>
                            <td style={{ padding: '12px' }}>{project.projectLocation}</td>
                        </tr>
                        <tr>
                            <td style={{ padding: '12px', background: '#f8f9fa', fontWeight: '500' }}>사업비</td>
                            <td style={{ padding: '12px' }}>
                                총 {project.totalBudget?.toLocaleString()}천원
                                (도비 {project.provincialFund?.toLocaleString()},
                                시군비 {project.cityFund?.toLocaleString()},
                                자부담 {project.selfFund?.toLocaleString()})
                            </td>
                        </tr>
                        </tbody>
                    </table>
                </div>

                {isNotGenerated ? (
                    <div style={{
                        marginTop: '40px',
                        padding: '40px',
                        background: '#f8f9fa',
                        borderRadius: '8px',
                        textAlign: 'center',
                        border: '2px solid #e0e0e0'
                    }}>
                        <p style={{ fontSize: '18px', color: '#2c3e50', marginBottom: '12px', fontWeight: '600' }}>
                            AI가 나머지 내용을 작성합니다
                        </p>
                        <p style={{ fontSize: '14px', color: '#7f8c8d', marginBottom: '24px' }}>
                            답변을 바탕으로 세부계획, 월별 추진계획, 기대효과를 생성합니다
                        </p>
                        <button
                            className="btn btn-primary"
                            onClick={handleGeneratePlan}
                            disabled={generating}
                            style={{ padding: '16px 40px', fontSize: '16px' }}
                        >
                            {generating ? '생성 중... (20초 소요)' : '사업계획서 생성하기'}
                        </button>
                    </div>
                ) : (
                    <>

                        {project.detailedPlan && (
                            <div className="result-section">
                                <h3 className="result-section-title">2. 세부계획</h3>
                                <div className="result-content" style={{ whiteSpace: 'pre-wrap' }}>
                                    {project.detailedPlan}
                                </div>
                            </div>
                        )}


                        {project.monthlyPlan && (
                            <div className="result-section">
                                <h3 className="result-section-title">3. 월별 추진계획</h3>
                                <div className="result-content" style={{ whiteSpace: 'pre-wrap' }}>
                                    {project.monthlyPlan}
                                </div>
                            </div>
                        )}


                        {project.expectedEffect && (
                            <div className="result-section">
                                <h3 className="result-section-title">4. 기대효과</h3>
                                <div className="result-content" style={{ whiteSpace: 'pre-wrap' }}>
                                    {project.expectedEffect}
                                </div>
                            </div>
                        )}

                        <div style={{
                            marginTop: '40px',
                            padding: '30px',
                            background: '#f8f9fa',
                            borderRadius: '8px',
                            textAlign: 'center'
                        }}>
                            <p style={{ fontSize: '16px', color: '#2c3e50', marginBottom: '20px', fontWeight: '500' }}>
                                사업계획서 작성이 완료되었습니다
                            </p>

                            <div style={{ display: 'flex', gap: '12px', justifyContent: 'center', flexWrap: 'wrap' }}>

                                <a
                                    href={`http://localhost:8080/api/projects/${project.id}/download`}
                                    download
                                    className="btn btn-primary"
                                    style={{ textDecoration: 'none', display: 'inline-block' }}
                                >
                                    📄 DOCX 다운로드
                                </a>


                                {project.budgetDetails && (
                                    <a
                                        href={`http://localhost:8080/api/projects/${project.id}/download-budget`}
                                        download
                                        className="btn btn-primary"
                                        style={{ textDecoration: 'none', display: 'inline-block' }}
                                    >
                                        📊 사업비 엑셀 다운로드
                                    </a>
                                )}

                                <button
                                    className="btn btn-secondary"
                                    onClick={() => window.location.reload()}
                                >
                                    새 사업계획서 작성
                                </button>
                            </div>
                        </div>
                    </>
                )}
            </div>
        </div>
    );
}

export default ResultPage;
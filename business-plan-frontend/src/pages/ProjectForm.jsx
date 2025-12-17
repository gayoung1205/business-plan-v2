import React, { useState } from 'react';
import BudgetTable from '../components/BudgetTable';

function ProjectForm({ onSuccess }) {
    const [step, setStep] = useState('input');
    const [savedProjectId, setSavedProjectId] = useState(null);

    const [formData, setFormData] = useState({
        communityName: '',
        projectName: '',
        projectPeriod: '',
        projectLocation: '',
        totalBudget: '',
        provincialFund: '',
        cityFund: '',
        selfFund: ''
    });

    const [budgetValidation, setBudgetValidation] = useState(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState('');
    const [showManualInput] = useState(false);
    const [excelData, setExcelData] = useState(null);
    const [showBudgetTable, setShowBudgetTable] = useState(false);
    const [tempExcelData, setTempExcelData] = useState(null);

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData(prev => ({
            ...prev,
            [name]: value
        }));

        if (['totalBudget', 'provincialFund', 'cityFund', 'selfFund'].includes(name)) {
            setBudgetValidation(null);
        }
    };

    const handleExcelUpload = async (e) => {
        const file = e.target.files[0];
        if (!file) return;

        if (!formData.totalBudget) {
            alert('⚠️ 먼저 총사업비를 입력해주세요!');
            e.target.value = '';
            return;
        }

        const uploadFormData = new FormData();
        uploadFormData.append('file', file);

        try {
            const response = await fetch('http://localhost:8080/api/projects/upload-excel', {
                method: 'POST',
                body: uploadFormData
            });

            const result = await response.json();

            if (result.success) {
                const inputTotal = parseInt(formData.totalBudget);
                const excelTotal = result.data.totalAmount;

                if (inputTotal !== excelTotal) {
                    setTempExcelData(result.data);
                    setShowBudgetTable(true);
                } else {
                    setExcelData(result.data);
                    setFormData(prev => ({
                        ...prev,
                        provincialFund: result.data.totalProvincial.toString(),
                        cityFund: result.data.totalCity.toString(),
                        selfFund: result.data.totalSelf.toString()
                    }));
                    alert('✅ 파일 업로드 완료!');
                }
            } else {
                alert('파일 업로드 실패: ' + result.message);
            }
        } catch (err) {
            console.error('엑셀 업로드 오류:', err);
            alert('파일 업로드 중 오류가 발생했습니다');
        }
    };

    const handleSaveBudget = (updatedExcelData) => {
        setExcelData(updatedExcelData);
        setFormData(prev => ({
            ...prev,
            totalBudget: updatedExcelData.totalAmount.toString(),
            provincialFund: updatedExcelData.totalProvincial.toString(),
            cityFund: updatedExcelData.totalCity.toString(),
            selfFund: updatedExcelData.totalSelf.toString()
        }));
        setShowBudgetTable(false);
        setTempExcelData(null);
        alert('✅ 사업비 저장 완료!');
    };

    const handleValidateBudget = async () => {
        try {
            const total = parseInt(formData.totalBudget) || 0;
            const provincial = parseInt(formData.provincialFund) || 0;
            const city = parseInt(formData.cityFund) || 0;
            const self = parseInt(formData.selfFund) || 0;
            const sum = provincial + city + self;
            const diff = total - sum;

            if (diff !== 0) {
                const message = diff > 0
                    ? `❌ ${Math.abs(diff).toLocaleString()}천원 부족\n\n자부담을 ${(self + diff).toLocaleString()}천원으로 수정하세요`
                    : `❌ ${Math.abs(diff).toLocaleString()}천원 초과\n\n자부담을 ${(self + diff).toLocaleString()}천원으로 수정하세요`;

                setBudgetValidation({
                    valid: false,
                    message: message
                });
            } else {
                setBudgetValidation({
                    valid: true,
                    message: '✅ 사업비가 정확합니다!'
                });
            }
        } catch (err) {
            setError('사업비 검증 실패');
        }
    };

    const handleSaveDraft = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            // 1️⃣ 필수 항목 체크
            if (!formData.communityName || !formData.projectName || !formData.projectLocation) {
                setError('필수 항목을 모두 입력해주세요');
                setLoading(false);
                return;
            }

            // 2️⃣ 엑셀 데이터 체크
            if (!excelData || !excelData.items || excelData.items.length === 0) {
                alert('⚠️ 사업비 산출내역(엑셀 파일)을 업로드해주세요!');
                setLoading(false);
                return;
            }

            // 3️⃣ 금액 검증 (오차 허용: ±10원)
            const total = parseInt(formData.totalBudget) || 0;
            const provincial = parseInt(formData.provincialFund) || 0;
            const city = parseInt(formData.cityFund) || 0;
            const self = parseInt(formData.selfFund) || 0;
            const sum = provincial + city + self;
            const diff = Math.abs(total - sum);

            // 🎯 10원 이상 차이나면 오류
            if (diff > 10) {
                alert(
                    `❌ 금액이 맞지 않습니다!\n\n` +
                    `총사업비: ${total.toLocaleString()}천원\n` +
                    `현재 합계: ${sum.toLocaleString()}천원\n` +
                    `차이: ${diff.toLocaleString()}천원 ${total > sum ? '부족' : '초과'}\n\n` +
                    `다시 수정해주세요.`
                );
                setLoading(false);
                return;
            }

            // 4️⃣ 금액이 맞으면 저장
            const projectData = {
                communityName: formData.communityName,
                projectName: formData.projectName,
                projectPeriod: formData.projectPeriod,
                projectLocation: formData.projectLocation,
                totalBudget: total,
                provincialFund: provincial,
                cityFund: city,
                selfFund: self,
                excelData: excelData
            };

            console.log('=== 임시저장 데이터 ===');
            console.log(projectData);

            const response = await fetch('http://localhost:8080/api/projects/save-draft', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(projectData)
            });

            const result = await response.json();
            console.log('=== 임시저장 응답 ===');
            console.log(result);

            if (result.success) {
                setSavedProjectId(result.projectId);
                setStep('preview');
            } else {
                setError(result.message);
            }
        } catch (err) {
            console.error('=== 임시저장 에러 ===');
            console.error(err);
            setError('임시저장에 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    const handleEdit = () => {
        setStep('input');
    };

    const handleGenerateQuestions = async () => {
        setLoading(true);
        setError('');

        try {
            console.log('=== 질문 생성 시작 ===');
            console.log('프로젝트 ID:', savedProjectId);

            const response = await fetch(`http://localhost:8080/api/projects/${savedProjectId}/generate-questions`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                }
            });

            const result = await response.json();
            console.log('=== 질문 생성 응답 ===');
            console.log(result);

            if (result.success) {
                onSuccess(result);
            } else {
                setError(result.message);
            }
        } catch (err) {
            console.error('=== 질문 생성 에러 ===');
            console.error(err);
            setError('질문 생성에 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    // ========== 입력 단계 ==========
    if (step === 'input') {
        return (
            <div className="container">
                <div className="card fade-in">
                    <h2 className="card-title">1. 사업개요</h2>

                    {error && (
                        <div className="alert alert-error">{error}</div>
                    )}

                    <form onSubmit={handleSaveDraft}>
                        <div className="form-group">
                            <label className="form-label">공동체명 *</label>
                            <input
                                type="text"
                                name="communityName"
                                value={formData.communityName}
                                onChange={handleChange}
                                className="form-input"
                                placeholder="행복나눔공동체"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">사업명 *</label>
                            <input
                                type="text"
                                name="projectName"
                                value={formData.projectName}
                                onChange={handleChange}
                                className="form-input"
                                placeholder="마을 공동체 활성화 사업"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">사업기간 *</label>
                            <input
                                type="text"
                                name="projectPeriod"
                                value={formData.projectPeriod}
                                onChange={handleChange}
                                className="form-input"
                                placeholder="2026. 3. ~ 11."
                                required
                            />
                            <p style={{ fontSize: '13px', color: '#7f8c8d', marginTop: '6px' }}>
                                예시: 2026. 3. ~ 11. / 2026년 연중
                            </p>
                        </div>

                        <div className="form-group">
                            <label className="form-label">사업위치 *</label>
                            <input
                                type="text"
                                name="projectLocation"
                                value={formData.projectLocation}
                                onChange={handleChange}
                                className="form-input"
                                placeholder="전라남도 나주시 빛가람동 123-45"
                                required
                            />
                        </div>

                        <div className="form-group">
                            <label className="form-label">총사업비 (단위: 천원) *</label>
                            <input
                                type="number"
                                name="totalBudget"
                                value={formData.totalBudget}
                                onChange={handleChange}
                                className="form-input"
                                placeholder="10000"
                                required
                            />
                            <p style={{ fontSize: '13px', color: '#7f8c8d', marginTop: '6px' }}>
                                엑셀 파일의 총 합계와 일치해야 합니다
                            </p>
                        </div>

                        {/* ========== 사업비 산출내역 ========== */}
                        <div className="card" style={{ background: '#f8f9fa', marginTop: '30px' }}>
                            <h3 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '20px' }}>
                                사업비 산출내역
                            </h3>

                            {formData.totalBudget && (
                                <div style={{
                                    background: '#e8f4f8',
                                    padding: '16px',
                                    borderRadius: '4px',
                                    marginBottom: '20px',
                                    border: '1px solid #b8dce8'
                                }}>
                                    <p style={{ fontSize: '14px', color: '#2c3e50', fontWeight: '500' }}>
                                        입력한 총사업비: <span style={{ fontSize: '18px', fontWeight: '600' }}>
                                            {parseInt(formData.totalBudget).toLocaleString()}천원
                                        </span>
                                    </p>
                                    <p style={{ fontSize: '13px', color: '#7f8c8d', marginTop: '6px' }}>
                                        엑셀 파일의 합계가 이 금액과 일치해야 합니다
                                    </p>
                                </div>
                            )}

                            {/* 엑셀 업로드 영역 */}
                            <div style={{
                                border: '2px dashed #ddd',
                                borderRadius: '8px',
                                padding: '30px',
                                textAlign: 'center',
                                background: 'white',
                                marginBottom: '20px',
                                cursor: 'pointer',
                                transition: 'border-color 0.2s'
                            }}
                                 onMouseEnter={(e) => e.currentTarget.style.borderColor = '#2c3e50'}
                                 onMouseLeave={(e) => e.currentTarget.style.borderColor = '#ddd'}
                            >
                                <input
                                    type="file"
                                    accept=".xlsx,.xls"
                                    onChange={handleExcelUpload}
                                    style={{ display: 'none' }}
                                    id="excel-upload"
                                />
                                <label htmlFor="excel-upload" style={{ cursor: 'pointer', display: 'block' }}>
                                    <div style={{ fontSize: '48px', marginBottom: '12px' }}>📊</div>
                                    <div style={{ fontSize: '16px', fontWeight: '500', marginBottom: '8px', color: '#2c3e50' }}>
                                        엑셀 파일 업로드
                                    </div>
                                    <div style={{ fontSize: '14px', color: '#7f8c8d' }}>
                                        .xlsx, .xls 파일만 가능 (최대 5MB)
                                    </div>
                                </label>
                            </div>

                            {/* ⭐ 수정 화면 */}
                            {showBudgetTable && tempExcelData && (
                                <BudgetTable
                                    initialData={tempExcelData}
                                    targetTotal={parseInt(formData.totalBudget)}
                                    onSave={handleSaveBudget}
                                    onCancel={() => {
                                        // 취소 시에는 아무것도 저장하지 않음
                                        setShowBudgetTable(false);
                                        setTempExcelData(null);
                                    }}
                                />
                            )}

                            {/* ⭐ 업로드 완료 표시 */}
                            {excelData && !showBudgetTable && (
                                <div style={{ marginBottom: '20px' }}>
                                    <div className="alert alert-success">
                                        <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                                            <span style={{ fontSize: '18px' }}>✅</span>
                                            <strong>파일 업로드 완료!</strong>
                                        </div>
                                        <div style={{ fontSize: '13px', lineHeight: '1.6' }}>
                                            • 항목 수: {excelData.items?.length || 0}개<br/>
                                            • 엑셀 합계: {excelData.totalAmount?.toLocaleString()}천원<br/>
                                            • 입력 총사업비: {parseInt(formData.totalBudget).toLocaleString()}천원<br/>
                                            • 도비: {excelData.totalProvincial?.toLocaleString()}천원<br/>
                                            • 시군비: {excelData.totalCity?.toLocaleString()}천원<br/>
                                            • 자부담: {excelData.totalSelf?.toLocaleString()}천원
                                        </div>
                                    </div>
                                </div>
                            )}

                            {/* 한글 안내 */}
                            <div style={{
                                marginTop: '24px',
                                padding: '16px',
                                background: '#fff3cd',
                                borderLeft: '4px solid #ffc107',
                                borderRadius: '4px'
                            }}>
                                <p style={{ fontSize: '14px', fontWeight: '500', marginBottom: '8px', color: '#856404' }}>
                                    💡 한글(.hwp) 파일을 사용 중인가요?
                                </p>
                                <div style={{ fontSize: '13px', color: '#856404', lineHeight: '1.6' }}>
                                    1. 한글 파일에서 표 전체 선택 (Ctrl+A)<br/>
                                    2. 복사 (Ctrl+C)<br/>
                                    3. 엑셀 새 파일 열기<br/>
                                    4. 붙여넣기 (Ctrl+V)<br/>
                                    5. 저장 후 위에서 업로드
                                </div>
                            </div>

                            {showManualInput && (
                                <div style={{
                                    background: 'white',
                                    padding: '20px',
                                    borderRadius: '8px',
                                    border: '1px solid #e0e0e0',
                                    marginTop: '20px'
                                }}>
                                    <h4 style={{ fontSize: '14px', fontWeight: '600', marginBottom: '16px' }}>
                                        보조금 및 자부담 (단위: 천원)
                                    </h4>

                                    <div className="form-row">
                                        <div className="form-group">
                                            <label className="form-label">도비 (30%)</label>
                                            <input
                                                type="number"
                                                name="provincialFund"
                                                value={formData.provincialFund}
                                                onChange={handleChange}
                                                className="form-input"
                                                placeholder="1500"
                                            />
                                        </div>

                                        <div className="form-group">
                                            <label className="form-label">시군비 (70%)</label>
                                            <input
                                                type="number"
                                                name="cityFund"
                                                value={formData.cityFund}
                                                onChange={handleChange}
                                                className="form-input"
                                                placeholder="3500"
                                            />
                                        </div>

                                        <div className="form-group">
                                            <label className="form-label">자부담</label>
                                            <input
                                                type="number"
                                                name="selfFund"
                                                value={formData.selfFund}
                                                onChange={handleChange}
                                                className="form-input"
                                                placeholder="0"
                                            />
                                        </div>
                                    </div>

                                    {(formData.provincialFund || formData.cityFund || formData.selfFund) && (
                                        <div style={{
                                            padding: '12px',
                                            background: '#f8f9fa',
                                            borderRadius: '4px',
                                            marginTop: '12px',
                                            marginBottom: '12px'
                                        }}>
                                            <div style={{ fontSize: '13px', color: '#555' }}>
                                                계산된 합계: <strong>
                                                {(
                                                    (parseInt(formData.provincialFund) || 0) +
                                                    (parseInt(formData.cityFund) || 0) +
                                                    (parseInt(formData.selfFund) || 0)
                                                ).toLocaleString()}천원
                                            </strong>
                                            </div>
                                        </div>
                                    )}

                                    <button
                                        type="button"
                                        onClick={handleValidateBudget}
                                        className="btn btn-secondary"
                                    >
                                        사업비 검증
                                    </button>

                                    {budgetValidation && (
                                        <div className={`alert ${budgetValidation.valid ? 'alert-success' : 'alert-warning'}`}
                                             style={{ marginTop: '16px' }}>
                                            {budgetValidation.message}
                                        </div>
                                    )}
                                </div>
                            )}
                        </div>

                        <button
                            type="submit"
                            className="btn btn-primary btn-full"
                            style={{ marginTop: '30px' }}
                            disabled={loading}
                        >
                            {loading ? '💾 저장 중...' : '💾 저장하고 계속하기'}
                        </button>
                    </form>
                </div>
            </div>
        );
    }

    // ========== 미리보기 단계 ==========
    if (step === 'preview') {
        return (
            <div className="container">
                <div className="card fade-in">
                    <h2 className="card-title">사업개요 확인</h2>

                    <div className="alert alert-info">
                        ✅ 사업개요가 저장되었습니다. 내용을 확인하고 질문 생성을 시작하세요.
                    </div>

                    <div style={{ background: '#f8f9fa', padding: '24px', borderRadius: '8px', marginBottom: '24px' }}>
                        <div style={{ marginBottom: '16px' }}>
                            <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>공동체명</strong>
                            <p style={{ fontSize: '16px', marginTop: '4px' }}>{formData.communityName}</p>
                        </div>

                        <div style={{ marginBottom: '16px' }}>
                            <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>사업명</strong>
                            <p style={{ fontSize: '16px', marginTop: '4px' }}>{formData.projectName}</p>
                        </div>

                        <div style={{ marginBottom: '16px' }}>
                            <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>사업기간</strong>
                            <p style={{ fontSize: '16px', marginTop: '4px' }}>{formData.projectPeriod}</p>
                        </div>

                        <div style={{ marginBottom: '16px' }}>
                            <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>사업위치</strong>
                            <p style={{ fontSize: '16px', marginTop: '4px' }}>{formData.projectLocation}</p>
                        </div>

                        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '12px', marginTop: '24px', padding: '16px', background: 'white', borderRadius: '6px' }}>
                            <div>
                                <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>총사업비</strong>
                                <p style={{ fontSize: '18px', fontWeight: '600', color: '#2c3e50', marginTop: '4px' }}>
                                    {formData.totalBudget ? parseInt(formData.totalBudget).toLocaleString() : '0'}천원
                                </p>
                            </div>
                            <div>
                                <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>도비</strong>
                                <p style={{ fontSize: '16px', marginTop: '4px' }}>
                                    {formData.provincialFund ? parseInt(formData.provincialFund).toLocaleString() : '0'}천원
                                </p>
                            </div>
                            <div>
                                <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>시군비</strong>
                                <p style={{ fontSize: '16px', marginTop: '4px' }}>
                                    {formData.cityFund ? parseInt(formData.cityFund).toLocaleString() : '0'}천원
                                </p>
                            </div>
                            <div>
                                <strong style={{ color: '#7f8c8d', fontSize: '13px' }}>자부담</strong>
                                <p style={{ fontSize: '16px', marginTop: '4px' }}>
                                    {formData.selfFund ? parseInt(formData.selfFund).toLocaleString() : '0'}천원
                                </p>
                            </div>
                        </div>
                    </div>

                    <div style={{ display: 'flex', gap: '12px' }}>
                        <button
                            onClick={handleEdit}
                            className="btn btn-secondary"
                            style={{ flex: 1 }}
                        >
                            ✏️ 수정하기
                        </button>

                        <button
                            onClick={handleGenerateQuestions}
                            className="btn btn-primary"
                            style={{ flex: 2 }}
                            disabled={loading}
                        >
                            {loading ? '⏳ 질문 생성 중... (약 15초 소요)' : '✨ 질문 생성하기'}
                        </button>
                    </div>

                    <p style={{ fontSize: '13px', color: '#7f8c8d', textAlign: 'center', marginTop: '16px' }}>
                        💡 질문 생성 후에는 사업개요를 수정할 수 없습니다
                    </p>
                </div>
            </div>
        );
    }

    return null;
}

export default ProjectForm;
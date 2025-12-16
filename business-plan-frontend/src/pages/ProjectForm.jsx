import React, { useState } from 'react';
// import { validateBudget } from '../services/api';
import BudgetTable from '../components/BudgetTable';

function ProjectForm({ onSuccess }) {
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

                // ⭐ 금액 불일치 시 수정 화면 표시
                if (inputTotal !== excelTotal) {
                    setTempExcelData(result.data);
                    setShowBudgetTable(true);
                } else {
                    // 일치하면 바로 저장
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
                alert('❌ 파일 업로드 실패: ' + result.message);
            }
        } catch (err) {
            console.error('업로드 실패:', err);
            alert('파일 업로드 중 오류가 발생했습니다');
        }
    };

    const handleSaveBudget = (updatedData) => {
        setExcelData(updatedData);
        setFormData(prev => ({
            ...prev,
            provincialFund: updatedData.totalProvincial.toString(),
            cityFund: updatedData.totalCity.toString(),
            selfFund: updatedData.totalSelf.toString()
        }));
        setShowBudgetTable(false);
        setTempExcelData(null);

    };

    const handleValidateBudget = async () => {
        try {
            const inputTotal = parseInt(formData.totalBudget) || 0;
            const provincial = parseInt(formData.provincialFund) || 0;
            const city = parseInt(formData.cityFund) || 0;
            const self = parseInt(formData.selfFund) || 0;

            const calculatedTotal = provincial + city + self;

            if (inputTotal !== calculatedTotal) {
                const diff = inputTotal - calculatedTotal;
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

    const handleSubmit = async (e) => {
        e.preventDefault();
        setLoading(true);
        setError('');

        try {
            if (!formData.communityName || !formData.projectName || !formData.projectLocation) {
                setError('필수 항목을 모두 입력해주세요');
                setLoading(false);
                return;
            }

            const projectData = {
                communityName: formData.communityName,
                projectName: formData.projectName,
                projectPeriod: formData.projectPeriod,
                projectLocation: formData.projectLocation,
                totalBudget: parseInt(formData.totalBudget) || 0,
                provincialFund: parseInt(formData.provincialFund) || 0,
                cityFund: parseInt(formData.cityFund) || 0,
                selfFund: parseInt(formData.selfFund) || 0,
                excelData: excelData
            };

            console.log('=== 전송할 데이터 ===');
            console.log(projectData);

            const response = await fetch('http://localhost:8080/api/projects/create', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(projectData)
            });

            console.log('=== 응답 상태 ===');
            console.log('Status:', response.status);

            const result = await response.json();
            console.log('=== 응답 데이터 ===');
            console.log(result);

            if (result.success) {
                onSuccess(result);
            } else {
                if (result.validationFailed) {
                    alert(result.message);
                } else {
                    setError(result.message);
                }
            }
        } catch (err) {
            console.error('=== 전체 에러 ===');
            console.error(err);
            setError('프로젝트 생성에 실패했습니다');
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="container">
            <div className="card">
                <h2 className="card-title">1. 사업개요</h2>

                {error && (
                    <div className="alert alert-error">{error}</div>
                )}

                <form onSubmit={handleSubmit}>
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

                    {/* ========== 사업비 산출내역 섹션 시작 ========== */}
                    <div className="card" style={{ background: '#f8f9fa', marginTop: '30px' }}>
                        <h3 style={{ fontSize: '16px', fontWeight: '600', marginBottom: '20px' }}>
                            사업비 산출내역
                        </h3>

                        {/* 총사업비 표시 */}
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
                            <label
                                htmlFor="excel-upload"
                                style={{ cursor: 'pointer', display: 'block' }}
                            >
                                <div style={{ fontSize: '48px', marginBottom: '12px' }}>📊</div>
                                <div style={{ fontSize: '16px', fontWeight: '500', marginBottom: '8px', color: '#2c3e50' }}>
                                    엑셀 파일 업로드
                                </div>
                                <div style={{ fontSize: '14px', color: '#7f8c8d' }}>
                                    .xlsx, .xls 파일만 가능 (최대 5MB)
                                </div>
                            </label>
                        </div>

                        {showBudgetTable && tempExcelData && (
                            <BudgetTable
                                initialData={tempExcelData}
                                targetTotal={parseInt(formData.totalBudget)}
                                onSave={handleSaveBudget}
                                onCancel={() => {
                                    setShowBudgetTable(false);
                                    setTempExcelData(null);
                                }}
                            />
                        )}

                        {/* 업로드된 파일 정보 표시 (금액 일치 시) */}
                        {excelData && !showBudgetTable && (
                            <div style={{ marginBottom: '20px' }}>
                                <div className="alert alert-success">
                                    <div style={{ display: 'flex', alignItems: 'center', gap: '8px', marginBottom: '8px' }}>
                                        <span style={{ fontSize: '18px' }}>✅</span>
                                        <strong>파일 업로드 완료!</strong>
                                    </div>
                                    <div style={{ fontSize: '13px', lineHeight: '1.6' }}>
                                        • 항목 수: {excelData.itemCount}개<br/>
                                        • 엑셀 합계: {excelData.totalAmount.toLocaleString()}천원<br/>
                                        • 입력 총사업비: {parseInt(formData.totalBudget).toLocaleString()}천원<br/>
                                        <strong style={{ color: '#28a745' }}>✓ 금액 일치</strong>
                                    </div>
                                </div>

                                {/* ⭐ 수정된 엑셀 다운로드 버튼 */}
                                <div style={{ textAlign: 'center' }}>
                                    <p style={{ fontSize: '13px', color: '#7f8c8d', marginBottom: '8px' }}>
                                        수정된 사업비 산출내역을 다운로드할 수 있습니다
                                    </p>
                                    <button
                                        type="button"
                                        onClick={() => {
                                            // 임시 프로젝트 ID 사용 (실제로는 저장 후 받은 ID 사용)
                                            // 여기서는 미리보기이므로 localStorage에 저장
                                            localStorage.setItem('tempBudgetData', JSON.stringify(excelData));
                                            alert('💡 사업계획서 생성 완료 후 최종 화면에서 다운로드 가능합니다');
                                        }}
                                        className="btn btn-secondary"
                                        style={{ fontSize: '14px' }}
                                    >
                                        📥 엑셀 파일로 저장 (최종 화면에서 가능)
                                    </button>
                                </div>
                            </div>
                        )}

                        {/* 한글 파일 사용자 가이드 */}
                        <div style={{
                            background: '#fff3cd',
                            border: '1px solid #ffeeba',
                            borderRadius: '4px',
                            padding: '16px',
                            fontSize: '13px',
                            color: '#856404',
                            marginBottom: '20px'
                        }}>
                            <div style={{ fontWeight: '600', marginBottom: '8px' }}>
                                💡 한글(.hwp) 파일을 사용 중이신가요?
                            </div>
                            <div style={{ lineHeight: '1.6' }}>
                                1. 한글 파일에서 표 전체 선택 (Ctrl+A)<br/>
                                2. 복사 (Ctrl+C)<br/>
                                3. 엑셀 새 파일 열기<br/>
                                4. 붙여넣기 (Ctrl+V)<br/>
                                5. 저장 후 위에서 업로드
                            </div>
                        </div>

                        {/* 직접 입력 토글 버튼 */}
                        {showManualInput && (
                            <div style={{
                                background: 'white',
                                padding: '20px',
                                borderRadius: '8px',
                                border: '1px solid #e0e0e0'
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

                                {/* 합계 표시 */}
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
                    {/* ========== 사업비 입력 섹션 끝 ========== */}

                    <button
                        type="submit"
                        className="btn btn-primary btn-full"
                        style={{ marginTop: '30px' }}
                        disabled={loading}
                    >
                        {loading ? '질문 생성 중...' : '다음 단계로'}
                    </button>
                </form>
            </div>
        </div>
    );
}

export default ProjectForm;
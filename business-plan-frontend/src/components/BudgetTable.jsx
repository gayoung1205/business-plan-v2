import React, { useState } from 'react';

function BudgetTable({
                         initialData,
                         targetTotal,
                         onSave,
                         onCancel
                     }) {
    const [items, setItems] = useState(initialData.items || []);
    const [saved, setSaved] = useState(false);

    const currentTotal = items.reduce((sum, item) => sum + (item.amount || 0), 0);
    const difference = targetTotal - currentTotal;

    const handleAmountChange = (index, newAmount) => {
        const updated = [...items];
        updated[index].amount = parseInt(newAmount) || 0;

        const amount = updated[index].amount;


        const provincialFund = Math.round(amount * 0.3);
        const cityFund = Math.round(amount * 0.7);
        const selfFund = amount - provincialFund - cityFund;  // 나머지를 자부담에

        updated[index].provincialFund = provincialFund;
        updated[index].cityFund = cityFund;
        updated[index].selfFund = selfFund;

        setItems(updated);
    };

    const handleAutoAdjust = () => {
        if (items.length === 0) return;

        const updated = [...items];

        if (difference > 0) {

            const lastIndex = items.length - 1;
            updated[lastIndex].amount += difference;

            const amount = updated[lastIndex].amount;
            const provincialFund = Math.round(amount * 0.3);
            const cityFund = Math.round(amount * 0.7);
            const selfFund = amount - provincialFund - cityFund;

            updated[lastIndex].provincialFund = provincialFund;
            updated[lastIndex].cityFund = cityFund;
            updated[lastIndex].selfFund = selfFund;

        } else if (difference < 0) {
            const maxIndex = items.reduce((maxIdx, item, idx, arr) =>
                item.amount > arr[maxIdx].amount ? idx : maxIdx, 0);

            const reduceAmount = Math.abs(difference);

            if (updated[maxIndex].amount >= reduceAmount) {
                updated[maxIndex].amount -= reduceAmount;

                const amount = updated[maxIndex].amount;
                const provincialFund = Math.round(amount * 0.3);
                const cityFund = Math.round(amount * 0.7);
                const selfFund = amount - provincialFund - cityFund;

                updated[maxIndex].provincialFund = provincialFund;
                updated[maxIndex].cityFund = cityFund;
                updated[maxIndex].selfFund = selfFund;
            } else {
                alert('⚠️ 자동 조정 실패\n\n가장 큰 항목의 금액이 차감할 금액보다 작습니다.\n직접 수정해주세요.');
                return;
            }
        }

        setItems(updated);
    };

    const handleDeleteRow = (index) => {
        if (items.length === 1) {
            alert('⚠️ 최소 1개 항목은 있어야 합니다');
            return;
        }
        setItems(items.filter((_, i) => i !== index));
    };

    const handleAddRow = () => {
        setItems([...items, {
            subProject: '',
            budgetItem: '',
            calculation: '',
            amount: 0,
            provincialFund: 0,
            cityFund: 0,
            selfFund: 0
        }]);
    };

    const handleSave = () => {

        const newTotal = items.reduce((sum, item) => sum + (item.amount || 0), 0);
        const newProvincial = items.reduce((sum, item) => sum + (item.provincialFund || 0), 0);
        const newCity = items.reduce((sum, item) => sum + (item.cityFund || 0), 0);
        const newSelf = items.reduce((sum, item) => sum + (item.selfFund || 0), 0);


        if (newTotal !== targetTotal) {
            alert(`⚠️ 오류: 합계가 목표 금액과 일치하지 않습니다.\n\n현재 합계: ${newTotal.toLocaleString()}천원\n목표 금액: ${targetTotal.toLocaleString()}천원\n\n자동 조정 버튼을 눌러주세요.`);
            return;
        }

        const calculatedSum = newProvincial + newCity + newSelf;
        if (calculatedSum !== newTotal) {
            alert(`⚠️ 오류: 보조금 합계가 맞지 않습니다.\n\n총사업비: ${newTotal.toLocaleString()}천원\n도비+시군비+자부담: ${calculatedSum.toLocaleString()}천원\n차이: ${Math.abs(newTotal - calculatedSum).toLocaleString()}천원`);
            return;
        }

        console.log('=== BudgetTable 저장 데이터 ===');
        console.log('총사업비:', newTotal);
        console.log('도비:', newProvincial);
        console.log('시군비:', newCity);
        console.log('자부담:', newSelf);
        console.log('검증 합계:', calculatedSum);

        setSaved(true);

        setTimeout(() => {
            onSave({
                items,
                totalAmount: newTotal,
                totalProvincial: newProvincial,
                totalCity: newCity,
                totalSelf: newSelf,
                itemCount: items.length
            });
        }, 500);
    };

    return (
        <div style={{ marginTop: '20px' }}>
            <div style={{
                background: saved ? '#d4edda' : (difference === 0 ? '#d4edda' : '#fff3cd'),
                padding: '16px',
                borderRadius: '4px',
                marginBottom: '16px',
                border: `2px solid ${saved ? '#28a745' : (difference === 0 ? '#c3e6cb' : '#ffeeba')}`
            }}>
                {saved ? (
                    <div style={{ textAlign: 'center', color: '#155724', fontSize: '16px', fontWeight: '600' }}>
                        ✅ 저장 중...
                    </div>
                ) : (
                    <>
                        <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: '12px' }}>
                            <div>
                                <div style={{ fontSize: '15px', marginBottom: '8px' }}>
                                    <strong>입력 총사업비:</strong> {targetTotal.toLocaleString()}천원
                                </div>
                                <div style={{ fontSize: '15px', marginBottom: '8px' }}>
                                    <strong>현재 합계:</strong> {currentTotal.toLocaleString()}천원
                                </div>
                                {difference !== 0 && (
                                    <div style={{ fontSize: '16px', fontWeight: '600', color: '#856404', marginTop: '8px' }}>
                                        차이: {Math.abs(difference).toLocaleString()}천원 {difference > 0 ? '부족' : '초과'}
                                    </div>
                                )}
                                {difference === 0 && (
                                    <div style={{ fontSize: '16px', fontWeight: '600', color: '#155724', marginTop: '8px' }}>
                                        ✅ 금액 일치!
                                    </div>
                                )}
                            </div>

                            {difference !== 0 && (
                                <button
                                    type="button"
                                    onClick={handleAutoAdjust}
                                    className="btn btn-secondary"
                                    style={{ whiteSpace: 'nowrap' }}
                                >
                                    💡 자동 조정
                                </button>
                            )}
                        </div>

                        {difference < 0 && (
                            <div style={{
                                fontSize: '13px',
                                color: '#856404',
                                background: '#fffbf0',
                                padding: '8px',
                                borderRadius: '4px',
                                marginTop: '8px'
                            }}>
                                ℹ️ 자동 조정 시 가장 큰 금액 항목에서 {Math.abs(difference).toLocaleString()}천원을 차감합니다
                            </div>
                        )}

                        {difference > 0 && (
                            <div style={{
                                fontSize: '13px',
                                color: '#856404',
                                background: '#fffbf0',
                                padding: '8px',
                                borderRadius: '4px',
                                marginTop: '8px'
                            }}>
                                ℹ️ 자동 조정 시 마지막 항목에 {difference.toLocaleString()}천원을 추가합니다
                            </div>
                        )}
                    </>
                )}
            </div>

            {!saved && (
                <>
                    <div style={{ overflowX: 'auto' }}>
                        <table style={{
                            width: '100%',
                            borderCollapse: 'collapse',
                            background: 'white',
                            fontSize: '13px'
                        }}>
                            <thead>
                            <tr style={{ background: '#f8f9fa' }}>
                                <th style={thStyle}>세부사업</th>
                                <th style={thStyle}>사업비목</th>
                                <th style={thStyle}>산출근거</th>
                                <th style={thStyle}>계</th>
                                <th style={thStyle}>도비</th>
                                <th style={thStyle}>시군비</th>
                                <th style={thStyle}>자부담</th>
                                <th style={thStyle}>삭제</th>
                            </tr>
                            </thead>
                            <tbody>
                            {items.map((item, index) => (
                                <tr key={index} style={{
                                    background: item.amount === Math.max(...items.map(i => i.amount)) && difference < 0
                                        ? '#fff8e1'
                                        : 'white'
                                }}>
                                    <td style={tdStyle}>
                                        <input
                                            type="text"
                                            value={item.subProject}
                                            onChange={(e) => {
                                                const updated = [...items];
                                                updated[index].subProject = e.target.value;
                                                setItems(updated);
                                            }}
                                            style={inputStyle}
                                        />
                                    </td>
                                    <td style={tdStyle}>
                                        <input
                                            type="text"
                                            value={item.budgetItem}
                                            onChange={(e) => {
                                                const updated = [...items];
                                                updated[index].budgetItem = e.target.value;
                                                setItems(updated);
                                            }}
                                            style={inputStyle}
                                        />
                                    </td>
                                    <td style={tdStyle}>
                                        <input
                                            type="text"
                                            value={item.calculation}
                                            onChange={(e) => {
                                                const updated = [...items];
                                                updated[index].calculation = e.target.value;
                                                setItems(updated);
                                            }}
                                            style={inputStyle}
                                        />
                                    </td>
                                    <td style={tdStyle}>
                                        <input
                                            type="number"
                                            value={item.amount}
                                            onChange={(e) => handleAmountChange(index, e.target.value)}
                                            style={inputStyle}
                                        />
                                    </td>
                                    <td style={tdStyle}>{item.provincialFund?.toLocaleString()}</td>
                                    <td style={tdStyle}>{item.cityFund?.toLocaleString()}</td>
                                    <td style={tdStyle}>{item.selfFund?.toLocaleString()}</td>
                                    <td style={{ ...tdStyle, textAlign: 'center' }}>
                                        <button
                                            type="button"
                                            onClick={() => handleDeleteRow(index)}
                                            style={{
                                                background: 'none',
                                                border: 'none',
                                                color: '#dc3545',
                                                cursor: 'pointer',
                                                fontSize: '18px',
                                                fontWeight: 'bold'
                                            }}
                                            title="삭제"
                                        >
                                            ×
                                        </button>
                                    </td>
                                </tr>
                            ))}
                            </tbody>
                        </table>
                    </div>

                    <div style={{ marginTop: '16px', display: 'flex', gap: '12px', justifyContent: 'space-between' }}>
                        <button
                            type="button"
                            onClick={handleAddRow}
                            className="btn btn-secondary"
                        >
                            + 항목 추가
                        </button>

                        <div style={{ display: 'flex', gap: '12px' }}>
                            <button
                                type="button"
                                onClick={onCancel}
                                className="btn btn-secondary"
                            >
                                취소
                            </button>
                            <button
                                type="button"
                                onClick={handleSave}
                                className="btn btn-primary"
                                disabled={difference !== 0}
                            >
                                저장하기
                            </button>
                        </div>
                    </div>
                </>
            )}
        </div>
    );
}

const thStyle = {
    padding: '12px 8px',
    borderBottom: '2px solid #dee2e6',
    fontWeight: '600',
    textAlign: 'left',
    whiteSpace: 'nowrap'
};

const tdStyle = {
    padding: '8px',
    borderBottom: '1px solid #dee2e6'
};

const inputStyle = {
    width: '100%',
    padding: '6px',
    border: '1px solid #ddd',
    borderRadius: '4px',
    fontSize: '13px'
};

export default BudgetTable;
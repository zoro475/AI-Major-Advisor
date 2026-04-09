import './StudentInputs.css';

export function SurveySuccess() {
  return (
    <div className="survey-success">
      <div className="success-icon">
        <svg viewBox="0 0 24 24" width="80" height="80" fill="none" stroke="currentColor" strokeWidth="2">
          <circle cx="12" cy="12" r="10" />
          <path d="M8 12l2.5 2.5L16 9" strokeLinecap="round" strokeLinejoin="round" />
        </svg>
      </div>
      <h2>Gửi khảo sát thành công! 🎉</h2>
      <p>Cảm ơn bạn đã hoàn thành khảo sát. Phản hồi của bạn rất có giá trị!</p>
      <button className="btn-primary" onClick={() => window.location.reload()}>
        Làm khảo sát mới
      </button>
    </div>
  );
}

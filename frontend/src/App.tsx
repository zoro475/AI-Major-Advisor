import { useState } from 'react'
import { SurveyForm } from './components/survey/student/SurveyForm'
import { QuestionManager } from './components/survey/admin/QuestionManager'
import { SurveyHistory } from './components/survey/history/SurveyHistory'
import './App.css'

type Page = 'survey' | 'admin' | 'history'

function App() {
  const [currentPage, setCurrentPage] = useState<Page>('survey')

  const renderPage = () => {
    switch (currentPage) {
      case 'survey':
        return <SurveyForm />
      case 'admin':
        return <QuestionManager />
      case 'history':
        return <SurveyHistory />
    }
  }

  return (
    <div className="app-container">
      {/* Navigation */}
      <nav className="app-nav">
        <div className="nav-brand">
          <span className="brand-icon">🎓</span>
          <span className="brand-text">Khảo sát Hướng nghiệp</span>
        </div>
        <div className="nav-links">
          <button
            className={`nav-link ${currentPage === 'survey' ? 'active' : ''}`}
            onClick={() => setCurrentPage('survey')}
          >
            📋 Khảo sát
          </button>
          <button
            className={`nav-link ${currentPage === 'history' ? 'active' : ''}`}
            onClick={() => setCurrentPage('history')}
          >
            📜 Lịch sử
          </button>
          <button
            className={`nav-link ${currentPage === 'admin' ? 'active' : ''}`}
            onClick={() => setCurrentPage('admin')}
          >
            ⚙️ Quản lý
          </button>
        </div>
      </nav>

      {/* Page Content */}
      <main className="app-main">
        {renderPage()}
      </main>
    </div>
  )
}

export default App

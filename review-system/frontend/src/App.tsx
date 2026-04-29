import React from 'react';
import { BrowserRouter, Routes, Route, Navigate } from 'react-router-dom';
import { ConfigProvider } from 'antd';
import zhCN from 'antd/locale/zh_CN';
import MainLayout from './layouts/MainLayout';
import Dashboard from './pages/Dashboard';
import ReviewSubmit from './pages/review/ReviewSubmit';
import ReviewList from './pages/review/ReviewList';
import ReviewDetail from './pages/review/ReviewDetail';
import ReviewQueue from './pages/review/ReviewQueue';
import LawArticles from './pages/knowledge/LawArticles';
import LawSources from './pages/knowledge/LawSources';
import ContractTemplates from './pages/knowledge/ContractTemplates';
import KnowledgeConfirmation from './pages/knowledge/KnowledgeConfirmation';
import LawSourceParse from './pages/knowledge/LawSourceParse';
import CustomRules from './pages/rules/CustomRules';
import Checklists from './pages/rules/Checklists';
import CaseList from './pages/cases/CaseList';
import ConsistencyDashboard from './pages/dashboard/ConsistencyDashboard';
import GoldenTests from './pages/golden/GoldenTests';
import IndustryKnowledge from './pages/knowledge/IndustryKnowledge';

const App: React.FC = () => {
  return (
    <ConfigProvider
      locale={zhCN}
      theme={{
        token: {
          colorPrimary: '#1890ff',
          borderRadius: 6,
        },
      }}
    >
      <BrowserRouter>
        <Routes>
          <Route path="/" element={<MainLayout />}>
            <Route index element={<Navigate to="/dashboard" replace />} />
            <Route path="dashboard" element={<Dashboard />} />
            <Route path="dashboard/consistency" element={<ConsistencyDashboard />} />
            <Route path="review/submit" element={<ReviewSubmit />} />
            <Route path="review/list" element={<ReviewList />} />
            <Route path="review/queue" element={<ReviewQueue />} />
            <Route path="review/:id" element={<ReviewDetail />} />
            <Route path="knowledge/law-articles" element={<LawArticles />} />
            <Route path="knowledge/law-sources" element={<LawSources />} />
            <Route path="knowledge/contract-templates" element={<ContractTemplates />} />
            <Route path="knowledge/confirmation" element={<KnowledgeConfirmation />} />
            <Route path="knowledge/law-source-parse" element={<LawSourceParse />} />
            <Route path="rules/custom" element={<CustomRules />} />
            <Route path="rules/checklists" element={<Checklists />} />
            <Route path="cases" element={<CaseList />} />
            <Route path="golden" element={<GoldenTests />} />
            <Route path="knowledge/industry" element={<IndustryKnowledge />} />
            <Route path="*" element={<Navigate to="/dashboard" replace />} />
          </Route>
        </Routes>
      </BrowserRouter>
    </ConfigProvider>
  );
};

export default App;

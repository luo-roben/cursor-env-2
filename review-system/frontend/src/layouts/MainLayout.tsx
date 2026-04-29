import React, { useState } from 'react';
import { Layout, Menu } from 'antd';
import {
  DashboardOutlined,
  FileSearchOutlined,
  SendOutlined,
  UnorderedListOutlined,
  BookOutlined,
  FileTextOutlined,
  BankOutlined,
  FileProtectOutlined,
  ToolOutlined,
  CheckSquareOutlined,
  FolderOpenOutlined,
  SettingOutlined,
  TeamOutlined,
  UserOutlined,
  AuditOutlined,
  SolutionOutlined,
  ExperimentOutlined,
  LineChartOutlined,
} from '@ant-design/icons';
import { Outlet, useNavigate, useLocation } from 'react-router-dom';
import type { MenuProps } from 'antd';

const { Header, Sider, Content } = Layout;

type MenuItem = Required<MenuProps>['items'][number];

const menuItems: MenuItem[] = [
  {
    key: 'dashboard',
    icon: <DashboardOutlined />,
    label: '仪表盘',
    children: [
      {
        key: '/dashboard',
        icon: <DashboardOutlined />,
        label: '总览',
      },
      {
        key: '/dashboard/consistency',
        icon: <LineChartOutlined />,
        label: '复核一致性',
      },
    ],
  },
  {
    key: 'review',
    icon: <FileSearchOutlined />,
    label: '审查任务',
    children: [
      {
        key: '/review/submit',
        icon: <SendOutlined />,
        label: '提交审查',
      },
      {
        key: '/review/list',
        icon: <UnorderedListOutlined />,
        label: '审查列表',
      },
      {
        key: '/review/queue',
        icon: <AuditOutlined />,
        label: '复核工作台',
      },
    ],
  },
  {
    key: 'knowledge',
    icon: <BookOutlined />,
    label: '知识库',
    children: [
      {
        key: '/knowledge/law-articles',
        icon: <FileTextOutlined />,
        label: '法条管理',
      },
      {
        key: '/knowledge/law-sources',
        icon: <BankOutlined />,
        label: '法规来源',
      },
      {
        key: '/knowledge/contract-templates',
        icon: <FileProtectOutlined />,
        label: '合同模板',
      },
      {
        key: '/knowledge/confirmation',
        icon: <SolutionOutlined />,
        label: '知识确认',
      },
      {
        key: '/knowledge/law-source-parse',
        icon: <ExperimentOutlined />,
        label: '法规解析',
      },
      {
        key: '/knowledge/industry',
        icon: <BookOutlined />,
        label: '行业知识库',
      },
    ],
  },
  {
    key: 'rules',
    icon: <ToolOutlined />,
    label: '规则管理',
    children: [
      {
        key: '/rules/custom',
        icon: <ToolOutlined />,
        label: '自定义规则',
      },
      {
        key: '/rules/checklists',
        icon: <CheckSquareOutlined />,
        label: '自检清单',
      },
    ],
  },
  {
    key: '/cases',
    icon: <FolderOpenOutlined />,
    label: '案例库',
  },
  {
    key: '/golden',
    icon: <ExperimentOutlined />,
    label: '金标准测试',
  },
  {
    key: 'system',
    icon: <SettingOutlined />,
    label: '系统管理',
    children: [
      {
        key: '/system/tenants',
        icon: <TeamOutlined />,
        label: '租户',
      },
      {
        key: '/system/users',
        icon: <UserOutlined />,
        label: '用户',
      },
    ],
  },
];

const MainLayout: React.FC = () => {
  const [collapsed, setCollapsed] = useState(false);
  const navigate = useNavigate();
  const location = useLocation();

  const selectedKeys = [location.pathname];
  const openKeys = (() => {
    const path = location.pathname;
    if (path.startsWith('/dashboard')) return ['dashboard'];
    if (path.startsWith('/review')) return ['review'];
    if (path.startsWith('/knowledge')) return ['knowledge'];
    if (path.startsWith('/rules')) return ['rules'];
    if (path.startsWith('/golden')) return [];
    if (path.startsWith('/system')) return ['system'];
    return [];
  })();

  const handleMenuClick: MenuProps['onClick'] = ({ key }) => {
    navigate(key);
  };

  return (
    <Layout style={{ minHeight: '100vh' }}>
      <Sider
        collapsible
        collapsed={collapsed}
        onCollapse={setCollapsed}
        style={{ background: '#001529' }}
        width={220}
      >
        <div
          style={{
            height: 64,
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'center',
            color: '#fff',
            fontSize: collapsed ? 14 : 18,
            fontWeight: 700,
            borderBottom: '1px solid rgba(255,255,255,0.1)',
            whiteSpace: 'nowrap',
            overflow: 'hidden',
          }}
        >
          {collapsed ? '审查' : '智能审查系统'}
        </div>
        <Menu
          theme="dark"
          mode="inline"
          selectedKeys={selectedKeys}
          defaultOpenKeys={openKeys}
          items={menuItems}
          onClick={handleMenuClick}
        />
      </Sider>
      <Layout>
        <Header
          style={{
            background: '#fff',
            padding: '0 24px',
            display: 'flex',
            alignItems: 'center',
            justifyContent: 'space-between',
            boxShadow: '0 1px 4px rgba(0,0,0,0.08)',
          }}
        >
          <span style={{ fontSize: 16, fontWeight: 600, color: '#1a1a1a' }}>
            通用智能审查系统
          </span>
          <span style={{ color: '#666' }}>管理员</span>
        </Header>
        <Content
          style={{
            margin: 24,
            padding: 24,
            background: '#f5f5f5',
            minHeight: 280,
            overflow: 'auto',
          }}
        >
          <Outlet />
        </Content>
      </Layout>
    </Layout>
  );
};

export default MainLayout;

import React, { useEffect, useState } from 'react';
import { Row, Col, Card, Statistic, Table, Tag, Spin, message } from 'antd';
import {
  FileSearchOutlined,
  WarningOutlined,
  CheckCircleOutlined,
  DashboardOutlined,
} from '@ant-design/icons';
import { getDashboardOverview } from '../services/api';
import { useNavigate } from 'react-router-dom';

interface DashboardData {
  totalReviews: number;
  violationCount: number;
  compliantCount: number;
  averageRiskScore: number;
  categoryDistribution: Record<string, number>;
  riskLevelDistribution: Record<string, number>;
  recentTasks: any[];
}

const riskLevelColors: Record<string, string> = {
  critical: '#ff4d4f',
  high: '#ff7a45',
  medium: '#ffa940',
  low: '#52c41a',
};

const Dashboard: React.FC = () => {
  const [data, setData] = useState<DashboardData | null>(null);
  const [loading, setLoading] = useState(true);
  const navigate = useNavigate();

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await getDashboardOverview();
      setData(res.data?.data || res.data);
    } catch (err) {
      message.error('加载仪表盘数据失败');
      setData({
        totalReviews: 0,
        violationCount: 0,
        compliantCount: 0,
        averageRiskScore: 0,
        categoryDistribution: {},
        riskLevelDistribution: {},
        recentTasks: [],
      });
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '文档类型', dataIndex: 'documentType', key: 'documentType' },
    { title: '内容类型', dataIndex: 'contentType', key: 'contentType' },
    {
      title: '结论',
      dataIndex: 'verdict',
      key: 'verdict',
      render: (v: string) => (
        <Tag color={v === 'compliant' ? 'green' : v === 'violation' ? 'red' : 'orange'}>
          {v === 'compliant' ? '合规' : v === 'violation' ? '违规' : v || '-'}
        </Tag>
      ),
    },
    {
      title: '风险分',
      dataIndex: 'riskScore',
      key: 'riskScore',
      render: (v: number) => <span style={{ fontWeight: 600 }}>{v ?? '-'}</span>,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (v: string) => (
        <Tag color={v === 'completed' ? 'green' : v === 'processing' ? 'blue' : 'default'}>
          {v || '-'}
        </Tag>
      ),
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      render: (v: string) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
  ];

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  const categoryDist = data?.categoryDistribution || {};
  const categoryTotal = Object.values(categoryDist).reduce((a, b) => a + b, 0) || 1;
  const categoryColors = ['#1890ff', '#52c41a', '#faad14', '#ff4d4f', '#722ed1'];

  const riskDist = data?.riskLevelDistribution || {};
  const maxRiskCount = Math.max(...Object.values(riskDist), 1);

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>仪表盘</h2>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={6}>
          <Card hoverable>
            <Statistic
              title="总审查数"
              value={data?.totalReviews || 0}
              prefix={<FileSearchOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic
              title="违规数"
              value={data?.violationCount || 0}
              prefix={<WarningOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic
              title="合规数"
              value={data?.compliantCount || 0}
              prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
              valueStyle={{ color: '#52c41a' }}
            />
          </Card>
        </Col>
        <Col span={6}>
          <Card hoverable>
            <Statistic
              title="平均风险分"
              value={data?.averageRiskScore || 0}
              precision={1}
              prefix={<DashboardOutlined style={{ color: '#faad14' }} />}
              valueStyle={{ color: '#faad14' }}
            />
          </Card>
        </Col>
      </Row>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={12}>
          <Card title="按卡片类别分布">
            <div style={{ padding: '8px 0' }}>
              {Object.entries(categoryDist).length === 0 ? (
                <div style={{ textAlign: 'center', color: '#999', padding: 20 }}>暂无数据</div>
              ) : (
                Object.entries(categoryDist).map(([key, value], idx) => (
                  <div key={key} style={{ marginBottom: 12 }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginBottom: 4 }}>
                      <span>{key}</span>
                      <span style={{ color: '#666' }}>
                        {value} ({((value / categoryTotal) * 100).toFixed(1)}%)
                      </span>
                    </div>
                    <div
                      style={{
                        height: 20,
                        background: '#f0f0f0',
                        borderRadius: 4,
                        overflow: 'hidden',
                      }}
                    >
                      <div
                        style={{
                          width: `${(value / categoryTotal) * 100}%`,
                          height: '100%',
                          background: categoryColors[idx % categoryColors.length],
                          borderRadius: 4,
                          transition: 'width 0.5s',
                        }}
                      />
                    </div>
                  </div>
                ))
              )}
            </div>
          </Card>
        </Col>
        <Col span={12}>
          <Card title="按风险等级分布">
            <div style={{ display: 'flex', alignItems: 'flex-end', height: 200, gap: 24, padding: '0 20px' }}>
              {Object.entries(riskDist).length === 0 ? (
                <div style={{ textAlign: 'center', color: '#999', width: '100%', paddingTop: 80 }}>
                  暂无数据
                </div>
              ) : (
                Object.entries(riskDist).map(([level, count]) => (
                  <div
                    key={level}
                    style={{
                      flex: 1,
                      display: 'flex',
                      flexDirection: 'column',
                      alignItems: 'center',
                    }}
                  >
                    <span style={{ fontSize: 14, fontWeight: 600, marginBottom: 4 }}>{count}</span>
                    <div
                      style={{
                        width: '100%',
                        maxWidth: 60,
                        height: `${(count / maxRiskCount) * 160}px`,
                        minHeight: 8,
                        background: riskLevelColors[level] || '#1890ff',
                        borderRadius: '4px 4px 0 0',
                        transition: 'height 0.5s',
                      }}
                    />
                    <span style={{ marginTop: 8, fontSize: 12, color: '#666' }}>{level}</span>
                  </div>
                ))
              )}
            </div>
          </Card>
        </Col>
      </Row>

      <Card title="近期审查任务">
        <Table
          columns={columns}
          dataSource={data?.recentTasks || []}
          rowKey="id"
          pagination={false}
          size="middle"
          onRow={(record) => ({
            onClick: () => navigate(`/review/${record.id}`),
            style: { cursor: 'pointer' },
          })}
        />
      </Card>
    </div>
  );
};

export default Dashboard;

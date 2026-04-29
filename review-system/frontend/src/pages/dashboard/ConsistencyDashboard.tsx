import React, { useEffect, useState } from 'react';
import { Card, Row, Col, Statistic, Progress, Table, Spin, message, Tag } from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  BarChartOutlined,
} from '@ant-design/icons';
import { getConsistency } from '../../services/api';

interface DisagreementItem {
  taskId: number;
  humanVerdict: string;
  aiVerdict: string;
  category?: string;
  description?: string;
}

interface ConsistencyData {
  agreementRate: number;
  totalPairs: number;
  agreementCount?: number;
  disagreementCount?: number;
  disagreements: DisagreementItem[];
}

const ConsistencyDashboard: React.FC = () => {
  const [data, setData] = useState<ConsistencyData | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await getConsistency(1);
      setData(res.data?.data || res.data);
    } catch {
      message.error('加载一致性数据失败');
      setData(null);
    } finally {
      setLoading(false);
    }
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  const agreementRate = data?.agreementRate ?? 0;
  const totalPairs = data?.totalPairs ?? 0;
  const disagreements = data?.disagreements || [];
  const agreementCount = data?.agreementCount ?? Math.round(totalPairs * agreementRate / 100);
  const disagreementCount = data?.disagreementCount ?? (totalPairs - agreementCount);

  const columns = [
    {
      title: '任务ID',
      dataIndex: 'taskId',
      key: 'taskId',
      width: 80,
    },
    {
      title: '人工判定',
      dataIndex: 'humanVerdict',
      key: 'humanVerdict',
      width: 120,
      render: (v: string) => (
        <Tag color={v === 'compliant' ? 'green' : v === 'violation' ? 'red' : 'orange'}>
          {v || '-'}
        </Tag>
      ),
    },
    {
      title: 'AI判定',
      dataIndex: 'aiVerdict',
      key: 'aiVerdict',
      width: 120,
      render: (v: string) => (
        <Tag color={v === 'compliant' ? 'green' : v === 'violation' ? 'red' : 'orange'}>
          {v || '-'}
        </Tag>
      ),
    },
    {
      title: '类别',
      dataIndex: 'category',
      key: 'category',
      width: 150,
    },
    {
      title: '差异说明',
      dataIndex: 'description',
      key: 'description',
      ellipsis: true,
    },
  ];

  const progressColor = agreementRate >= 90 ? '#52c41a' : agreementRate >= 70 ? '#faad14' : '#ff4d4f';

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>复核一致性仪表盘</h2>

      <Row gutter={16} style={{ marginBottom: 24 }}>
        <Col span={8}>
          <Card>
            <div style={{ textAlign: 'center' }}>
              <Progress
                type="dashboard"
                percent={agreementRate}
                strokeColor={progressColor}
                format={(percent) => (
                  <span style={{ fontSize: 24, fontWeight: 700 }}>{percent}%</span>
                )}
              />
              <div style={{ marginTop: 8, fontSize: 14, color: '#666' }}>一致性比率</div>
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="总复核对数"
              value={totalPairs}
              prefix={<BarChartOutlined style={{ color: '#1890ff' }} />}
              valueStyle={{ color: '#1890ff' }}
            />
            <div style={{ marginTop: 12 }}>
              <Statistic
                title="一致数"
                value={agreementCount}
                prefix={<CheckCircleOutlined style={{ color: '#52c41a' }} />}
                valueStyle={{ color: '#52c41a', fontSize: 20 }}
              />
            </div>
          </Card>
        </Col>
        <Col span={8}>
          <Card>
            <Statistic
              title="不一致数"
              value={disagreementCount}
              prefix={<CloseCircleOutlined style={{ color: '#ff4d4f' }} />}
              valueStyle={{ color: '#ff4d4f' }}
            />
          </Card>
        </Col>
      </Row>

      <Card title="不一致详情列表">
        <Table
          columns={columns}
          dataSource={disagreements}
          rowKey={(record) => record.taskId?.toString() || Math.random().toString()}
          pagination={{ showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
          size="middle"
          locale={{ emptyText: '暂无不一致记录' }}
        />
      </Card>
    </div>
  );
};

export default ConsistencyDashboard;

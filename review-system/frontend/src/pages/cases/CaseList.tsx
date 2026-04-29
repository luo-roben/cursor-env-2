import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, message } from 'antd';
import { getCases } from '../../services/api';

const verdictColors: Record<string, string> = {
  compliant: 'green',
  violation: 'red',
  needs_review: 'orange',
};

const severityColors: Record<string, string> = {
  critical: 'red',
  major: 'orange',
  minor: 'gold',
  info: 'blue',
};

const CaseList: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await getCases(1);
      const result = res.data?.data || res.data;
      setData(Array.isArray(result) ? result : result?.content || []);
    } catch {
      message.error('加载案例数据失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '来源', dataIndex: 'source', key: 'source', width: 120 },
    { title: '内容类型', dataIndex: 'contentType', key: 'contentType', width: 120 },
    {
      title: '结论',
      dataIndex: 'verdict',
      key: 'verdict',
      width: 90,
      render: (v: string) => (
        <Tag color={verdictColors[v] || 'default'}>
          {v === 'compliant' ? '合规' : v === 'violation' ? '违规' : v || '-'}
        </Tag>
      ),
    },
    {
      title: '严重程度',
      dataIndex: 'severity',
      key: 'severity',
      width: 90,
      render: (v: string) => <Tag color={severityColors[v] || 'default'}>{v || '-'}</Tag>,
    },
    {
      title: '学习价值分',
      dataIndex: 'learningScore',
      key: 'learningScore',
      width: 100,
      render: (v: number) => (
        <span style={{ fontWeight: 600, color: v >= 80 ? '#52c41a' : v >= 60 ? '#faad14' : '#ff4d4f' }}>
          {v ?? '-'}
        </span>
      ),
    },
    {
      title: '是否典型',
      dataIndex: 'isTypical',
      key: 'isTypical',
      width: 80,
      render: (v: boolean) => (
        <Tag color={v ? 'green' : 'default'}>{v ? '是' : '否'}</Tag>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>案例库</h2>
      <Card>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{ showSizeChanger: true, showTotal: (t) => `共 ${t} 条` }}
          size="middle"
        />
      </Card>
    </div>
  );
};

export default CaseList;

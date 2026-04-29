import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, message, Button } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import { getReviewList } from '../../services/api';
import { useNavigate } from 'react-router-dom';

const verdictMap: Record<string, { text: string; color: string }> = {
  compliant: { text: '合规', color: 'green' },
  violation: { text: '违规', color: 'red' },
  needs_review: { text: '需复核', color: 'orange' },
};

const riskLevelMap: Record<string, { text: string; color: string }> = {
  critical: { text: '严重', color: 'red' },
  high: { text: '高', color: 'orange' },
  medium: { text: '中', color: 'gold' },
  low: { text: '低', color: 'green' },
};

const statusMap: Record<string, { text: string; color: string }> = {
  completed: { text: '已完成', color: 'green' },
  processing: { text: '处理中', color: 'blue' },
  pending: { text: '待处理', color: 'default' },
  failed: { text: '失败', color: 'red' },
};

const ReviewList: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [pagination, setPagination] = useState({ current: 1, pageSize: 20, total: 0 });
  const navigate = useNavigate();

  useEffect(() => {
    fetchData(0, 20);
  }, []);

  const fetchData = async (page: number, size: number) => {
    try {
      setLoading(true);
      const res = await getReviewList(1, page, size);
      const result = res.data?.data || res.data;
      if (Array.isArray(result)) {
        setData(result);
        setPagination((prev) => ({ ...prev, total: result.length }));
      } else if (result?.content) {
        setData(result.content);
        setPagination((prev) => ({
          ...prev,
          total: result.totalElements || result.content.length,
        }));
      } else {
        setData([]);
      }
    } catch (err) {
      message.error('加载审查列表失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleTableChange = (pag: any) => {
    setPagination(pag);
    fetchData(pag.current - 1, pag.pageSize);
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '文档类型', dataIndex: 'documentType', key: 'documentType', width: 120 },
    { title: '内容类型', dataIndex: 'contentType', key: 'contentType', width: 120 },
    {
      title: '结论',
      dataIndex: 'verdict',
      key: 'verdict',
      width: 90,
      render: (v: string) => {
        const info = verdictMap[v] || { text: v || '-', color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '风险分',
      dataIndex: 'riskScore',
      key: 'riskScore',
      width: 80,
      render: (v: number) => <span style={{ fontWeight: 600 }}>{v ?? '-'}</span>,
    },
    {
      title: '风险等级',
      dataIndex: 'riskLevel',
      key: 'riskLevel',
      width: 90,
      render: (v: string) => {
        const info = riskLevelMap[v] || { text: v || '-', color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (v: string) => {
        const info = statusMap[v] || { text: v || '-', color: 'default' };
        return <Tag color={info.color}>{info.text}</Tag>;
      },
    },
    {
      title: '创建时间',
      dataIndex: 'createdAt',
      key: 'createdAt',
      width: 170,
      render: (v: string) => (v ? new Date(v).toLocaleString('zh-CN') : '-'),
    },
    {
      title: '操作',
      key: 'actions',
      width: 80,
      render: (_: any, record: any) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => navigate(`/review/${record.id}`)}
        >
          查看
        </Button>
      ),
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>审查列表</h2>
      <Card>
        <Table
          columns={columns}
          dataSource={data}
          rowKey="id"
          loading={loading}
          pagination={{
            ...pagination,
            showSizeChanger: true,
            showQuickJumper: true,
            showTotal: (total) => `共 ${total} 条`,
          }}
          onChange={handleTableChange}
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

export default ReviewList;

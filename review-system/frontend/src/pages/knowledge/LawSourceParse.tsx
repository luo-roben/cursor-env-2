import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Button, message, Space, Popconfirm } from 'antd';
import {
  PlayCircleOutlined,
  CheckCircleOutlined,
  ReloadOutlined,
} from '@ant-design/icons';
import { getLawSources, parseSource, confirmSource } from '../../services/api';

const parseStatusConfig: Record<string, { text: string; color: string }> = {
  pending: { text: '待解析', color: 'blue' },
  parsing: { text: '解析中', color: 'orange' },
  parsed: { text: '已解析', color: 'cyan' },
  confirmed: { text: '已确认', color: 'green' },
  failed: { text: '失败', color: 'red' },
  completed: { text: '已完成', color: 'green' },
};

const LawSourceParse: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [actionLoading, setActionLoading] = useState<Record<number, boolean>>({});

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await getLawSources();
      const result = res.data?.data || res.data;
      setData(Array.isArray(result) ? result : result?.content || []);
    } catch {
      message.error('加载法规来源失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleParse = async (id: number) => {
    try {
      setActionLoading((prev) => ({ ...prev, [id]: true }));
      await parseSource(id);
      message.success('已开始解析');
      fetchData();
    } catch {
      message.error('解析启动失败');
    } finally {
      setActionLoading((prev) => ({ ...prev, [id]: false }));
    }
  };

  const handleConfirm = async (id: number) => {
    try {
      setActionLoading((prev) => ({ ...prev, [id]: true }));
      await confirmSource(id);
      message.success('已确认全部');
      fetchData();
    } catch {
      message.error('确认失败');
    } finally {
      setActionLoading((prev) => ({ ...prev, [id]: false }));
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '来源ID', dataIndex: 'sourceId', key: 'sourceId', width: 120 },
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '发布机构', dataIndex: 'publisher', key: 'publisher', width: 150 },
    { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
    {
      title: '解析状态',
      dataIndex: 'parseStatus',
      key: 'parseStatus',
      width: 100,
      render: (v: string) => {
        const cfg = parseStatusConfig[v] || { text: v || '-', color: 'default' };
        return <Tag color={cfg.color}>{cfg.text}</Tag>;
      },
    },
    {
      title: '解析法条数',
      dataIndex: 'parsedArticleCount',
      key: 'parsedArticleCount',
      width: 100,
      render: (v: number) => v ?? '-',
    },
    {
      title: '操作',
      key: 'actions',
      width: 200,
      render: (_: any, record: any) => {
        const status = record.parseStatus;
        return (
          <Space>
            {(status === 'pending' || !status) && (
              <Popconfirm title="确认开始解析？" onConfirm={() => handleParse(record.id)}>
                <Button
                  type="primary"
                  size="small"
                  icon={<PlayCircleOutlined />}
                  loading={actionLoading[record.id]}
                >
                  开始解析
                </Button>
              </Popconfirm>
            )}
            {status === 'parsed' && (
              <Popconfirm title="确认全部法条？" onConfirm={() => handleConfirm(record.id)}>
                <Button
                  type="primary"
                  size="small"
                  icon={<CheckCircleOutlined />}
                  loading={actionLoading[record.id]}
                  style={{ background: '#52c41a', borderColor: '#52c41a' }}
                >
                  确认全部
                </Button>
              </Popconfirm>
            )}
            {status === 'parsing' && (
              <Tag color="orange">解析中...</Tag>
            )}
            {status === 'confirmed' && (
              <Tag color="green" icon={<CheckCircleOutlined />}>
                已确认
              </Tag>
            )}
            {status === 'failed' && (
              <Popconfirm title="重新解析？" onConfirm={() => handleParse(record.id)}>
                <Button
                  size="small"
                  danger
                  icon={<ReloadOutlined />}
                  loading={actionLoading[record.id]}
                >
                  重新解析
                </Button>
              </Popconfirm>
            )}
          </Space>
        );
      },
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>法规解析</h2>
        <Button icon={<ReloadOutlined />} onClick={fetchData} loading={loading}>
          刷新
        </Button>
      </div>

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

export default LawSourceParse;

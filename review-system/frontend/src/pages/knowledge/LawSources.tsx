import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Button, Modal, Form, Input, Select, message } from 'antd';
import { PlusOutlined, EyeOutlined } from '@ant-design/icons';
import { getLawSources, createLawSource } from '../../services/api';

const { Option } = Select;

const parseStatusConfig: Record<string, { text: string; color: string }> = {
  pending: { text: '待解析', color: 'default' },
  parsing: { text: '解析中', color: 'processing' },
  completed: { text: '已完成', color: 'success' },
  failed: { text: '失败', color: 'error' },
};

const LawSources: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [detailVisible, setDetailVisible] = useState(false);
  const [currentRecord, setCurrentRecord] = useState<any>(null);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

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

  const handleCreate = async (values: any) => {
    try {
      setSubmitting(true);
      await createLawSource(values);
      message.success('创建成功');
      setModalVisible(false);
      form.resetFields();
      fetchData();
    } catch {
      message.error('创建失败');
    } finally {
      setSubmitting(false);
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
      title: '操作',
      key: 'actions',
      width: 80,
      render: (_: any, record: any) => (
        <Button
          type="link"
          icon={<EyeOutlined />}
          onClick={() => {
            setCurrentRecord(record);
            setDetailVisible(true);
          }}
        >
          查看
        </Button>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>法规来源</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
          新增来源
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

      <Modal
        title="新增法规来源"
        open={modalVisible}
        onCancel={() => { setModalVisible(false); form.resetFields(); }}
        footer={null}
        width={640}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="sourceId" label="来源ID" rules={[{ required: true, message: '请输入来源ID' }]}>
            <Input placeholder="请输入来源ID" />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true, message: '请输入标题' }]}>
            <Input placeholder="请输入标题" />
          </Form.Item>
          <Form.Item name="publisher" label="发布机构">
            <Input placeholder="请输入发布机构" />
          </Form.Item>
          <Form.Item name="type" label="类型">
            <Select placeholder="请选择类型">
              <Option value="law">法律</Option>
              <Option value="regulation">法规</Option>
              <Option value="rule">规章</Option>
              <Option value="notice">通知</Option>
              <Option value="guideline">指引</Option>
            </Select>
          </Form.Item>
          <Form.Item name="url" label="来源URL">
            <Input placeholder="请输入来源URL" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              创建
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="来源详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={640}
      >
        {currentRecord && (
          <div>
            <p><strong>来源ID：</strong>{currentRecord.sourceId}</p>
            <p><strong>标题：</strong>{currentRecord.title}</p>
            <p><strong>发布机构：</strong>{currentRecord.publisher}</p>
            <p><strong>类型：</strong>{currentRecord.type}</p>
            <p><strong>解析状态：</strong>{currentRecord.parseStatus}</p>
            {currentRecord.url && (
              <p><strong>URL：</strong><a href={currentRecord.url} target="_blank" rel="noopener noreferrer">{currentRecord.url}</a></p>
            )}
          </div>
        )}
      </Modal>
    </div>
  );
};

export default LawSources;

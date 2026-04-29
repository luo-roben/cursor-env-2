import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Button, Modal, Form, Input, Select, message, Space, Popconfirm } from 'antd';
import { PlusOutlined, EyeOutlined, CheckOutlined, StopOutlined } from '@ant-design/icons';
import { getLawArticles, createLawArticle, publishLawArticle, deprecateLawArticle } from '../../services/api';

const { Option } = Select;
const { TextArea } = Input;

const statusConfig: Record<string, { text: string; color: string }> = {
  draft: { text: '草稿', color: 'default' },
  published: { text: '已发布', color: 'green' },
  deprecated: { text: '已废弃', color: 'red' },
};

const LawArticles: React.FC = () => {
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
      const res = await getLawArticles();
      const result = res.data?.data || res.data;
      setData(Array.isArray(result) ? result : result?.content || []);
    } catch {
      message.error('加载法条数据失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (values: any) => {
    try {
      setSubmitting(true);
      await createLawArticle(values);
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

  const handlePublish = async (id: number) => {
    try {
      await publishLawArticle(id);
      message.success('发布成功');
      fetchData();
    } catch {
      message.error('发布失败');
    }
  };

  const handleDeprecate = async (id: number) => {
    try {
      await deprecateLawArticle(id);
      message.success('已废弃');
      fetchData();
    } catch {
      message.error('操作失败');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '法律名称', dataIndex: 'lawName', key: 'lawName', ellipsis: true },
    { title: '法条编号', dataIndex: 'articleNumber', key: 'articleNumber', width: 120 },
    { title: '类型', dataIndex: 'type', key: 'type', width: 100 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 90,
      render: (v: string) => {
        const cfg = statusConfig[v] || { text: v || '-', color: 'default' };
        return <Tag color={cfg.color}>{cfg.text}</Tag>;
      },
    },
    { title: '效力层级', dataIndex: 'effectLevel', key: 'effectLevel', width: 120 },
    {
      title: '操作',
      key: 'actions',
      width: 220,
      render: (_: any, record: any) => (
        <Space>
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
          {record.status !== 'published' && (
            <Popconfirm title="确认发布？" onConfirm={() => handlePublish(record.id)}>
              <Button type="link" icon={<CheckOutlined />} style={{ color: '#52c41a' }}>
                发布
              </Button>
            </Popconfirm>
          )}
          {record.status !== 'deprecated' && (
            <Popconfirm title="确认废弃？" onConfirm={() => handleDeprecate(record.id)}>
              <Button type="link" icon={<StopOutlined />} danger>
                废弃
              </Button>
            </Popconfirm>
          )}
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>法条管理</h2>
        <Button type="primary" icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
          新增法条
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
        title="新增法条"
        open={modalVisible}
        onCancel={() => { setModalVisible(false); form.resetFields(); }}
        footer={null}
        width={640}
      >
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="lawName" label="法律名称" rules={[{ required: true, message: '请输入法律名称' }]}>
            <Input placeholder="请输入法律名称" />
          </Form.Item>
          <Form.Item name="articleNumber" label="法条编号" rules={[{ required: true, message: '请输入法条编号' }]}>
            <Input placeholder="请输入法条编号" />
          </Form.Item>
          <Form.Item name="type" label="类型" rules={[{ required: true, message: '请选择类型' }]}>
            <Select placeholder="请选择类型">
              <Option value="law">法律</Option>
              <Option value="regulation">法规</Option>
              <Option value="rule">规章</Option>
              <Option value="guideline">指引</Option>
            </Select>
          </Form.Item>
          <Form.Item name="effectLevel" label="效力层级">
            <Select placeholder="请选择效力层级">
              <Option value="national">国家级</Option>
              <Option value="ministerial">部级</Option>
              <Option value="local">地方级</Option>
              <Option value="industry">行业级</Option>
            </Select>
          </Form.Item>
          <Form.Item name="content" label="法条内容">
            <TextArea rows={4} placeholder="请输入法条内容" />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              创建
            </Button>
          </Form.Item>
        </Form>
      </Modal>

      <Modal
        title="法条详情"
        open={detailVisible}
        onCancel={() => setDetailVisible(false)}
        footer={null}
        width={640}
      >
        {currentRecord && (
          <div>
            <p><strong>法律名称：</strong>{currentRecord.lawName}</p>
            <p><strong>法条编号：</strong>{currentRecord.articleNumber}</p>
            <p><strong>类型：</strong>{currentRecord.type}</p>
            <p><strong>状态：</strong>{currentRecord.status}</p>
            <p><strong>效力层级：</strong>{currentRecord.effectLevel}</p>
            <p><strong>内容：</strong></p>
            <div style={{ background: '#fafafa', padding: 16, borderRadius: 4, whiteSpace: 'pre-wrap' }}>
              {currentRecord.content || '暂无内容'}
            </div>
          </div>
        )}
      </Modal>
    </div>
  );
};

export default LawArticles;

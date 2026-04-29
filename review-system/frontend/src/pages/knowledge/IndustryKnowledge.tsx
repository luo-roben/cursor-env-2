import React, { useState, useEffect } from 'react';
import { Table, Button, Card, Space, Tag, Modal, Form, Input, Select, message, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getIndustryKnowledge, createIndustryKnowledge, updateIndustryKnowledge, deleteIndustryKnowledge } from '../../services/api';

const { TextArea } = Input;

const IndustryKnowledge: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [modalVisible, setModalVisible] = useState(false);
  const [editingId, setEditingId] = useState<number | null>(null);
  const [form] = Form.useForm();

  const loadData = async () => {
    setLoading(true);
    try {
      const res = await getIndustryKnowledge();
      setData(res.data?.data || []);
    } catch (e) {
      message.error('加载行业知识失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadData(); }, []);

  const handleSubmit = async (values: any) => {
    try {
      if (editingId) {
        await updateIndustryKnowledge(editingId, values);
        message.success('更新成功');
      } else {
        await createIndustryKnowledge(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      setEditingId(null);
      form.resetFields();
      loadData();
    } catch (e) {
      message.error('操作失败');
    }
  };

  const handleEdit = (record: any) => {
    setEditingId(record.id);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteIndustryKnowledge(id);
      message.success('删除成功');
      loadData();
    } catch (e) {
      message.error('删除失败');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '行业', dataIndex: 'industry', key: 'industry', width: 100 },
    { title: '类型', dataIndex: 'knowledgeType', key: 'knowledgeType', width: 120,
      render: (v: string) => {
        const map: Record<string, { color: string; label: string }> = {
          norm: { color: 'blue', label: '规范' },
          best_practice: { color: 'green', label: '最佳实践' },
          risk_pattern: { color: 'red', label: '风险模式' },
        };
        const item = map[v] || { color: 'default', label: v };
        return <Tag color={item.color}>{item.label}</Tag>;
      },
    },
    { title: '标题', dataIndex: 'title', key: 'title', ellipsis: true },
    { title: '来源', dataIndex: 'source', key: 'source', width: 120, ellipsis: true },
    { title: '状态', dataIndex: 'status', key: 'status', width: 80,
      render: (v: string) => <Tag color={v === 'published' ? 'green' : 'default'}>{v}</Tag>,
    },
    { title: '操作', key: 'action', width: 120,
      render: (_: any, record: any) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)} />
          <Popconfirm title="确定删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" danger icon={<DeleteOutlined />} />
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <Card
        title="行业知识库"
        extra={
          <Button type="primary" icon={<PlusOutlined />} onClick={() => { setEditingId(null); form.resetFields(); setModalVisible(true); }}>
            新建
          </Button>
        }
      >
        <Table dataSource={data} columns={columns} rowKey="id" loading={loading} size="small" />
      </Card>

      <Modal
        title={editingId ? '编辑行业知识' : '新建行业知识'}
        open={modalVisible}
        onCancel={() => { setModalVisible(false); setEditingId(null); }}
        onOk={() => form.submit()}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="industry" label="行业" rules={[{ required: true }]}>
            <Input placeholder="例如：金融、医药、互联网" />
          </Form.Item>
          <Form.Item name="knowledgeType" label="知识类型" rules={[{ required: true }]}>
            <Select options={[
              { value: 'norm', label: '行业规范' },
              { value: 'best_practice', label: '最佳实践' },
              { value: 'risk_pattern', label: '风险模式' },
            ]} />
          </Form.Item>
          <Form.Item name="title" label="标题" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="content" label="内容" rules={[{ required: true }]}>
            <TextArea rows={4} />
          </Form.Item>
          <Form.Item name="source" label="来源">
            <Input />
          </Form.Item>
          <Form.Item name="status" label="状态" initialValue="published">
            <Select options={[
              { value: 'published', label: '已发布' },
              { value: 'draft', label: '草稿' },
            ]} />
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default IndustryKnowledge;

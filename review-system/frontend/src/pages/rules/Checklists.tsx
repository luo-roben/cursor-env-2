import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Button, Modal, Form, Input, Select, Switch, message, Space, Popconfirm } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined } from '@ant-design/icons';
import { getChecklists, createChecklist, updateChecklist, deleteChecklist } from '../../services/api';

const { Option } = Select;

const severityColors: Record<string, string> = {
  critical: 'red',
  major: 'orange',
  minor: 'gold',
  info: 'blue',
};

const Checklists: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<any>(null);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await getChecklists();
      const result = res.data?.data || res.data;
      setData(Array.isArray(result) ? result : result?.content || []);
    } catch {
      message.error('加载自检清单失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      setSubmitting(true);
      if (editRecord) {
        await updateChecklist(editRecord.id, values);
        message.success('更新成功');
      } else {
        await createChecklist(values);
        message.success('创建成功');
      }
      setModalVisible(false);
      setEditRecord(null);
      form.resetFields();
      fetchData();
    } catch {
      message.error(editRecord ? '更新失败' : '创建失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleDelete = async (id: number) => {
    try {
      await deleteChecklist(id);
      message.success('删除成功');
      fetchData();
    } catch {
      message.error('删除失败');
    }
  };

  const handleEdit = (record: any) => {
    setEditRecord(record);
    form.setFieldsValue(record);
    setModalVisible(true);
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '内容类型', dataIndex: 'contentType', key: 'contentType', width: 120 },
    { title: '检查项', dataIndex: 'checkItem', key: 'checkItem', ellipsis: true },
    { title: '检查方法', dataIndex: 'checkMethod', key: 'checkMethod', width: 120 },
    {
      title: '严重程度',
      dataIndex: 'severity',
      key: 'severity',
      width: 90,
      render: (v: string) => <Tag color={severityColors[v] || 'default'}>{v || '-'}</Tag>,
    },
    {
      title: '启用',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 70,
      render: (v: boolean) => <Tag color={v ? 'green' : 'default'}>{v ? '是' : '否'}</Tag>,
    },
    {
      title: '操作',
      key: 'actions',
      width: 140,
      render: (_: any, record: any) => (
        <Space>
          <Button type="link" icon={<EditOutlined />} onClick={() => handleEdit(record)}>
            编辑
          </Button>
          <Popconfirm title="确认删除？" onConfirm={() => handleDelete(record.id)}>
            <Button type="link" icon={<DeleteOutlined />} danger>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ];

  return (
    <div>
      <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', marginBottom: 24 }}>
        <h2 style={{ margin: 0 }}>自检清单</h2>
        <Button
          type="primary"
          icon={<PlusOutlined />}
          onClick={() => {
            setEditRecord(null);
            form.resetFields();
            setModalVisible(true);
          }}
        >
          新增检查项
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
        title={editRecord ? '编辑检查项' : '新增检查项'}
        open={modalVisible}
        onCancel={() => { setModalVisible(false); setEditRecord(null); form.resetFields(); }}
        footer={null}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="contentType" label="内容类型" rules={[{ required: true, message: '请输入内容类型' }]}>
            <Input placeholder="请输入内容类型" />
          </Form.Item>
          <Form.Item name="checkItem" label="检查项" rules={[{ required: true, message: '请输入检查项' }]}>
            <Input.TextArea rows={3} placeholder="请输入检查项内容" />
          </Form.Item>
          <Form.Item name="checkMethod" label="检查方法" rules={[{ required: true, message: '请选择检查方法' }]}>
            <Select placeholder="请选择检查方法">
              <Option value="keyword">关键词检查</Option>
              <Option value="regex">正则匹配</Option>
              <Option value="semantic">语义分析</Option>
              <Option value="manual">人工检查</Option>
            </Select>
          </Form.Item>
          <Form.Item name="severity" label="严重程度" rules={[{ required: true, message: '请选择严重程度' }]}>
            <Select placeholder="请选择严重程度">
              <Option value="critical">严重</Option>
              <Option value="major">重要</Option>
              <Option value="minor">轻微</Option>
              <Option value="info">提示</Option>
            </Select>
          </Form.Item>
          <Form.Item name="enabled" label="启用" valuePropName="checked" initialValue={true}>
            <Switch />
          </Form.Item>
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              {editRecord ? '更新' : '创建'}
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default Checklists;

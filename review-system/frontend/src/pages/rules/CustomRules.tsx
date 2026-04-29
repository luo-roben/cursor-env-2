import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Button, Modal, Form, Input, Select, Switch, message, Space, Popconfirm, Alert } from 'antd';
import { PlusOutlined, EditOutlined, DeleteOutlined, ThunderboltOutlined } from '@ant-design/icons';
import { getTenantRules, createTenantRule, updateTenantRule, deleteTenantRule, detectConflicts } from '../../services/api';

const { Option } = Select;

const ruleTypeColors: Record<string, string> = {
  keyword: 'blue',
  regex: 'purple',
  semantic: 'cyan',
  template: 'geekblue',
};

const severityColors: Record<string, string> = {
  critical: 'red',
  major: 'orange',
  minor: 'gold',
  info: 'blue',
};

const CustomRules: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [modalVisible, setModalVisible] = useState(false);
  const [editRecord, setEditRecord] = useState<any>(null);
  const [form] = Form.useForm();
  const [submitting, setSubmitting] = useState(false);
  const [conflicts, setConflicts] = useState<any[] | null>(null);
  const [conflictLoading, setConflictLoading] = useState(false);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await getTenantRules(1);
      const result = res.data?.data || res.data;
      setData(Array.isArray(result) ? result : result?.content || []);
    } catch {
      message.error('加载规则失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleSubmit = async (values: any) => {
    try {
      setSubmitting(true);
      if (editRecord) {
        await updateTenantRule(editRecord.id, { ...values, tenantId: 1 });
        message.success('更新成功');
      } else {
        await createTenantRule({ ...values, tenantId: 1 });
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
      await deleteTenantRule(id);
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

  const handleToggleEnable = async (record: any) => {
    try {
      await updateTenantRule(record.id, { ...record, enabled: !record.enabled });
      message.success(record.enabled ? '已禁用' : '已启用');
      fetchData();
    } catch {
      message.error('操作失败');
    }
  };

  const handleDetectConflicts = async () => {
    try {
      setConflictLoading(true);
      const res = await detectConflicts(1);
      const result = res.data?.data || res.data;
      const list = Array.isArray(result) ? result : [];
      setConflicts(list);
      if (list.length === 0) {
        message.success('未发现规则冲突');
      }
    } catch {
      message.error('冲突检测失败');
      setConflicts(null);
    } finally {
      setConflictLoading(false);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    {
      title: '规则类型',
      dataIndex: 'ruleType',
      key: 'ruleType',
      width: 100,
      render: (v: string) => <Tag color={ruleTypeColors[v] || 'default'}>{v || '-'}</Tag>,
    },
    { title: '内容', dataIndex: 'content', key: 'content', ellipsis: true },
    { title: '匹配方式', dataIndex: 'matchType', key: 'matchType', width: 100 },
    {
      title: '严重程度',
      dataIndex: 'severity',
      key: 'severity',
      width: 90,
      render: (v: string) => <Tag color={severityColors[v] || 'default'}>{v || '-'}</Tag>,
    },
    {
      title: '启用状态',
      dataIndex: 'enabled',
      key: 'enabled',
      width: 90,
      render: (v: boolean, record: any) => (
        <Switch checked={v} onChange={() => handleToggleEnable(record)} size="small" />
      ),
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
        <h2 style={{ margin: 0 }}>自定义规则</h2>
        <Space>
          <Button
            icon={<ThunderboltOutlined />}
            onClick={handleDetectConflicts}
            loading={conflictLoading}
          >
            检测冲突
          </Button>
          <Button
            type="primary"
            icon={<PlusOutlined />}
            onClick={() => {
              setEditRecord(null);
              form.resetFields();
              setModalVisible(true);
            }}
          >
            新增规则
          </Button>
        </Space>
      </div>

      {conflicts !== null && (
        conflicts.length === 0 ? (
          <Alert
            message="未发现规则冲突"
            type="success"
            showIcon
            closable
            onClose={() => setConflicts(null)}
            style={{ marginBottom: 16 }}
          />
        ) : (
          <Alert
            message="发现规则冲突"
            type="warning"
            showIcon
            closable
            onClose={() => setConflicts(null)}
            style={{ marginBottom: 16 }}
            description={
              <ul style={{ margin: 0, paddingLeft: 20 }}>
                {conflicts.map((c: any, idx: number) => (
                  <li key={idx}>
                    {c.description || c.message || JSON.stringify(c)}
                    {c.ruleIds && (
                      <span style={{ marginLeft: 8 }}>
                        (规则: {Array.isArray(c.ruleIds) ? c.ruleIds.join(', ') : c.ruleIds})
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            }
          />
        )
      )}

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
        title={editRecord ? '编辑规则' : '新增规则'}
        open={modalVisible}
        onCancel={() => { setModalVisible(false); setEditRecord(null); form.resetFields(); }}
        footer={null}
        width={600}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmit}>
          <Form.Item name="ruleType" label="规则类型" rules={[{ required: true, message: '请选择规则类型' }]}>
            <Select placeholder="请选择规则类型">
              <Option value="keyword">关键词</Option>
              <Option value="regex">正则表达式</Option>
              <Option value="semantic">语义规则</Option>
              <Option value="template">模板规则</Option>
            </Select>
          </Form.Item>
          <Form.Item name="content" label="规则内容" rules={[{ required: true, message: '请输入规则内容' }]}>
            <Input.TextArea rows={3} placeholder="请输入规则内容" />
          </Form.Item>
          <Form.Item name="matchType" label="匹配方式" rules={[{ required: true, message: '请选择匹配方式' }]}>
            <Select placeholder="请选择匹配方式">
              <Option value="exact">精确匹配</Option>
              <Option value="contains">包含匹配</Option>
              <Option value="regex">正则匹配</Option>
              <Option value="semantic">语义匹配</Option>
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

export default CustomRules;

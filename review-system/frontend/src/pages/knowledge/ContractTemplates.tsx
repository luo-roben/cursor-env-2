import React, { useEffect, useState } from 'react';
import { Table, Tag, Card, Button, Modal, message, Spin } from 'antd';
import { EyeOutlined } from '@ant-design/icons';
import { getContractTemplates, getTemplateClauses } from '../../services/api';

const statusConfig: Record<string, { text: string; color: string }> = {
  active: { text: '启用', color: 'green' },
  inactive: { text: '停用', color: 'default' },
  draft: { text: '草稿', color: 'orange' },
};

const ContractTemplates: React.FC = () => {
  const [data, setData] = useState<any[]>([]);
  const [loading, setLoading] = useState(true);
  const [clauseModalVisible, setClauseModalVisible] = useState(false);
  const [clauses, setClauses] = useState<any[]>([]);
  const [clauseLoading, setClauseLoading] = useState(false);
  const [selectedTemplate, setSelectedTemplate] = useState<any>(null);

  useEffect(() => {
    fetchData();
  }, []);

  const fetchData = async () => {
    try {
      setLoading(true);
      const res = await getContractTemplates();
      const result = res.data?.data || res.data;
      setData(Array.isArray(result) ? result : result?.content || []);
    } catch {
      message.error('加载合同模板失败');
      setData([]);
    } finally {
      setLoading(false);
    }
  };

  const handleViewClauses = async (record: any) => {
    setSelectedTemplate(record);
    setClauseModalVisible(true);
    try {
      setClauseLoading(true);
      const res = await getTemplateClauses(record.id);
      const result = res.data?.data || res.data;
      setClauses(Array.isArray(result) ? result : []);
    } catch {
      message.error('加载条款失败');
      setClauses([]);
    } finally {
      setClauseLoading(false);
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '模板名称', dataIndex: 'templateName', key: 'templateName', ellipsis: true },
    { title: '合同类型', dataIndex: 'contractType', key: 'contractType', width: 120 },
    { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
    { title: '条款数', dataIndex: 'clauseCount', key: 'clauseCount', width: 80 },
    { title: '行业', dataIndex: 'industry', key: 'industry', width: 120 },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      width: 80,
      render: (v: string) => {
        const cfg = statusConfig[v] || { text: v || '-', color: 'default' };
        return <Tag color={cfg.color}>{cfg.text}</Tag>;
      },
    },
    {
      title: '操作',
      key: 'actions',
      width: 100,
      render: (_: any, record: any) => (
        <Button type="link" icon={<EyeOutlined />} onClick={() => handleViewClauses(record)}>
          查看条款
        </Button>
      ),
    },
  ];

  const clauseColumns = [
    { title: '序号', dataIndex: 'orderIndex', key: 'orderIndex', width: 60 },
    { title: '条款名称', dataIndex: 'clauseName', key: 'clauseName', width: 150 },
    { title: '条款内容', dataIndex: 'content', key: 'content', ellipsis: true },
    {
      title: '是否必需',
      dataIndex: 'required',
      key: 'required',
      width: 80,
      render: (v: boolean) => <Tag color={v ? 'red' : 'default'}>{v ? '必需' : '可选'}</Tag>,
    },
  ];

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>合同模板</h2>

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
        title={`条款列表 - ${selectedTemplate?.templateName || ''}`}
        open={clauseModalVisible}
        onCancel={() => setClauseModalVisible(false)}
        footer={null}
        width={800}
      >
        {clauseLoading ? (
          <div style={{ textAlign: 'center', padding: 40 }}><Spin /></div>
        ) : (
          <Table
            columns={clauseColumns}
            dataSource={clauses}
            rowKey="id"
            pagination={false}
            size="small"
          />
        )}
      </Modal>
    </div>
  );
};

export default ContractTemplates;

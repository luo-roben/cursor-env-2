import React, { useState, useEffect } from 'react';
import { Table, Button, Card, Space, Tag, Modal, Form, Input, Select, InputNumber, message, Spin, Collapse } from 'antd';
import { PlayCircleOutlined, PlusOutlined, CheckCircleOutlined, CloseCircleOutlined } from '@ant-design/icons';
import { getGoldenTests, createGoldenTest, runAllGoldenTests, runOneGoldenTest } from '../../services/api';

const { TextArea } = Input;
const { Panel } = Collapse;

const GoldenTests: React.FC = () => {
  const [tests, setTests] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [runLoading, setRunLoading] = useState(false);
  const [report, setReport] = useState<any>(null);
  const [modalVisible, setModalVisible] = useState(false);
  const [form] = Form.useForm();

  const loadTests = async () => {
    setLoading(true);
    try {
      const res = await getGoldenTests();
      setTests(res.data?.data || []);
    } catch (e) {
      message.error('加载测试用例失败');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadTests(); }, []);

  const handleRunAll = async () => {
    setRunLoading(true);
    try {
      const res = await runAllGoldenTests();
      setReport(res.data?.data);
      message.success('所有测试运行完成');
    } catch (e) {
      message.error('运行测试失败');
    } finally {
      setRunLoading(false);
    }
  };

  const handleRunOne = async (id: number) => {
    try {
      const res = await runOneGoldenTest(id);
      const result = res.data?.data;
      if (result?.passed) {
        message.success(`测试 "${result.name}" 通过`);
      } else {
        message.warning(`测试 "${result.name}" 未通过`);
      }
    } catch (e) {
      message.error('运行测试失败');
    }
  };

  const handleCreate = async (values: any) => {
    try {
      await createGoldenTest(values);
      message.success('测试用例创建成功');
      setModalVisible(false);
      form.resetFields();
      loadTests();
    } catch (e) {
      message.error('创建失败');
    }
  };

  const columns = [
    { title: 'ID', dataIndex: 'id', key: 'id', width: 60 },
    { title: '名称', dataIndex: 'name', key: 'name' },
    { title: '文档类型', dataIndex: 'documentType', key: 'documentType', width: 100 },
    { title: '内容类型', dataIndex: 'contentType', key: 'contentType', width: 100 },
    { title: '期望结果', dataIndex: 'expectedVerdict', key: 'expectedVerdict', width: 120,
      render: (v: string) => (
        <Tag color={v === 'COMPLIANT' ? 'green' : v === 'VIOLATION' ? 'red' : 'orange'}>{v}</Tag>
      ),
    },
    { title: '风险分范围', key: 'riskRange', width: 120,
      render: (_: any, record: any) => `${record.expectedMinRiskScore ?? '-'} ~ ${record.expectedMaxRiskScore ?? '-'}`,
    },
    { title: '启用', dataIndex: 'enabled', key: 'enabled', width: 60,
      render: (v: number) => v === 1 ? <Tag color="green">是</Tag> : <Tag>否</Tag>,
    },
    { title: '操作', key: 'action', width: 100,
      render: (_: any, record: any) => (
        <Button type="link" icon={<PlayCircleOutlined />} onClick={() => handleRunOne(record.id)}>
          运行
        </Button>
      ),
    },
  ];

  return (
    <div>
      <Card
        title="金标准测试用例"
        extra={
          <Space>
            <Button type="primary" icon={<PlayCircleOutlined />} loading={runLoading} onClick={handleRunAll}>
              运行所有
            </Button>
            <Button icon={<PlusOutlined />} onClick={() => setModalVisible(true)}>
              新建
            </Button>
          </Space>
        }
      >
        <Spin spinning={loading}>
          <Table dataSource={tests} columns={columns} rowKey="id" size="small" />
        </Spin>
      </Card>

      {report && (
        <Card title="测试报告" style={{ marginTop: 16 }}>
          <Space size="large" style={{ marginBottom: 16 }}>
            <span>总数: {report.totalTests}</span>
            <span style={{ color: '#52c41a' }}>通过: {report.passedTests}</span>
            <span style={{ color: '#f5222d' }}>失败: {report.failedTests}</span>
            <span>通过率: {report.passRate?.toFixed(1)}%</span>
          </Space>
          <Collapse>
            {report.results?.map((r: any, idx: number) => (
              <Panel
                key={idx}
                header={
                  <Space>
                    {r.passed ? <CheckCircleOutlined style={{ color: '#52c41a' }} /> : <CloseCircleOutlined style={{ color: '#f5222d' }} />}
                    <span>{r.name}</span>
                    <Tag color={r.passed ? 'green' : 'red'}>{r.passed ? 'PASS' : 'FAIL'}</Tag>
                  </Space>
                }
              >
                <p>期望结果: {r.expectedVerdict} | 实际结果: {r.actualVerdict}</p>
                <p>风险分: {r.actualRiskScore} (期望: {r.expectedMinRiskScore} ~ {r.expectedMaxRiskScore})</p>
                {r.mismatches?.length > 0 && (
                  <div>
                    <strong>不匹配项:</strong>
                    <ul>{r.mismatches.map((m: string, i: number) => <li key={i}>{m}</li>)}</ul>
                  </div>
                )}
              </Panel>
            ))}
          </Collapse>
        </Card>
      )}

      <Modal title="新建测试用例" open={modalVisible} onCancel={() => setModalVisible(false)} onOk={() => form.submit()} width={600}>
        <Form form={form} layout="vertical" onFinish={handleCreate}>
          <Form.Item name="name" label="名称" rules={[{ required: true }]}>
            <Input />
          </Form.Item>
          <Form.Item name="documentType" label="文档类型" rules={[{ required: true }]}>
            <Select options={[
              { value: 'MARKETING', label: '营销材料' },
              { value: 'CONTRACT', label: '合同' },
              { value: 'PROSPECTUS', label: '招募说明书' },
              { value: 'REPORT', label: '报告' },
              { value: 'OTHER', label: '其他' },
            ]} />
          </Form.Item>
          <Form.Item name="contentType" label="内容类型">
            <Input />
          </Form.Item>
          <Form.Item name="contractType" label="合同类型">
            <Input />
          </Form.Item>
          <Form.Item name="inputContent" label="输入内容" rules={[{ required: true }]}>
            <TextArea rows={4} />
          </Form.Item>
          <Form.Item name="expectedVerdict" label="期望结果" rules={[{ required: true }]}>
            <Select options={[
              { value: 'COMPLIANT', label: '合规' },
              { value: 'VIOLATION', label: '违规' },
              { value: 'NEEDS_REVIEW', label: '需人工审查' },
            ]} />
          </Form.Item>
          <Space>
            <Form.Item name="expectedMinRiskScore" label="最低风险分">
              <InputNumber min={0} max={100} />
            </Form.Item>
            <Form.Item name="expectedMaxRiskScore" label="最高风险分">
              <InputNumber min={0} max={100} />
            </Form.Item>
          </Space>
        </Form>
      </Modal>
    </div>
  );
};

export default GoldenTests;

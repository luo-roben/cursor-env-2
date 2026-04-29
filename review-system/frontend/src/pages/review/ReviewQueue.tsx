import React, { useEffect, useState } from 'react';
import {
  Layout,
  List,
  Card,
  Tabs,
  Tag,
  Badge,
  Descriptions,
  Spin,
  Button,
  Modal,
  Form,
  Input,
  Select,
  message,
  Empty,
} from 'antd';
import {
  CheckCircleOutlined,
  CloseCircleOutlined,
  ExclamationCircleOutlined,
  InfoCircleOutlined,
  WarningOutlined,
  FileTextOutlined,
  EditOutlined,
  PlusOutlined,
} from '@ant-design/icons';
import { getReviewList, getReviewDetail, submitFeedback } from '../../services/api';

const { Sider, Content } = Layout;
const { TextArea } = Input;

interface ReviewIssue {
  id: number;
  severity: string;
  description: string;
  originalText: string;
  suggestion: string;
  suggestionType: string;
  citationStatus: string;
  ruleId?: string;
  cardCategory?: string;
}

interface ReviewCard {
  category: string;
  categoryName: string;
  issues: ReviewIssue[];
  status: string;
  score?: number;
}

interface MissingElement {
  element: string;
  description: string;
  severity: string;
}

interface ReviewData {
  id: number;
  documentType: string;
  contentType: string;
  productType?: string;
  contractType?: string;
  channel?: string;
  content: string;
  verdict: string;
  riskScore: number;
  riskLevel: string;
  status: string;
  createdAt: string;
  reviewCards: ReviewCard[];
  missingElements: MissingElement[];
  issues: ReviewIssue[];
}

interface TaskSummary {
  id: number;
  documentType: string;
  contentType: string;
  verdict: string;
  riskScore: number;
  riskLevel: string;
  status: string;
  createdAt: string;
}

const severityConfig: Record<string, { color: string; icon: React.ReactNode; label: string }> = {
  critical: { color: '#ff4d4f', icon: <CloseCircleOutlined />, label: '严重' },
  major: { color: '#fa8c16', icon: <WarningOutlined />, label: '重要' },
  minor: { color: '#faad14', icon: <ExclamationCircleOutlined />, label: '轻微' },
  info: { color: '#1890ff', icon: <InfoCircleOutlined />, label: '提示' },
};

const cardCategories = [
  { key: 'text_basic_check', name: '文本基础核对', index: 1 },
  { key: 'redline_sensitive', name: '红线与敏感词', index: 2 },
  { key: 'form_element_review', name: '形式与要素审查', index: 3 },
  { key: 'semantic_compliance', name: '语义与合规审查', index: 4 },
  { key: 'logic_clause_review', name: '逻辑与条款审查', index: 5 },
];

const verdictConfig: Record<string, { text: string; color: string }> = {
  compliant: { text: '合规', color: 'green' },
  violation: { text: '违规', color: 'red' },
  needs_review: { text: '需复核', color: 'orange' },
};

type ActionType = 'confirm' | 'reject' | 'modify' | 'supplement' | null;

const ReviewQueue: React.FC = () => {
  const [tasks, setTasks] = useState<TaskSummary[]>([]);
  const [tasksLoading, setTasksLoading] = useState(true);
  const [selectedTaskId, setSelectedTaskId] = useState<number | null>(null);
  const [detail, setDetail] = useState<ReviewData | null>(null);
  const [detailLoading, setDetailLoading] = useState(false);

  const [actionType, setActionType] = useState<ActionType>(null);
  const [actionIssue, setActionIssue] = useState<ReviewIssue | null>(null);
  const [actionModalVisible, setActionModalVisible] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchTasks();
  }, []);

  const fetchTasks = async () => {
    try {
      setTasksLoading(true);
      const res = await getReviewList(1, 0, 100);
      const result = res.data?.data || res.data;
      let list: TaskSummary[] = Array.isArray(result) ? result : result?.content || [];
      list = list
        .filter((t) => t.status?.toLowerCase() === 'completed')
        .sort((a, b) => (b.riskScore ?? 0) - (a.riskScore ?? 0));
      setTasks(list);
      if (list.length > 0 && !selectedTaskId) {
        handleSelectTask(list[0].id);
      }
    } catch {
      message.error('加载复核队列失败');
      setTasks([]);
    } finally {
      setTasksLoading(false);
    }
  };

  const handleSelectTask = async (id: number) => {
    setSelectedTaskId(id);
    try {
      setDetailLoading(true);
      const res = await getReviewDetail(id);
      setDetail(res.data?.data || res.data);
    } catch {
      message.error('加载任务详情失败');
      setDetail(null);
    } finally {
      setDetailLoading(false);
    }
  };

  const openActionModal = (type: ActionType, issue: ReviewIssue | null) => {
    setActionType(type);
    setActionIssue(issue);
    form.resetFields();
    if (type === 'modify' && issue) {
      form.setFieldsValue({ severity: issue.severity, reason: issue.description });
    }
    setActionModalVisible(true);
  };

  const handleSubmitFeedback = async (values: any) => {
    if (!detail || !actionType) return;
    try {
      setSubmitting(true);
      const payload: any = {
        taskId: detail.id,
        tenantId: 1,
        actionType: actionType,
        issueId: actionIssue?.id,
      };
      if (actionType === 'reject') {
        payload.rejectReason = values.reason;
      } else if (actionType === 'modify') {
        payload.modifiedSeverity = values.severity;
        payload.modifiedReason = values.reason;
      } else if (actionType === 'supplement') {
        payload.newIssue = {
          severity: values.severity,
          description: values.description,
          suggestion: values.suggestion,
          cardCategory: values.cardCategory,
        };
      }
      await submitFeedback(payload);
      message.success('反馈提交成功');
      setActionModalVisible(false);
      setActionType(null);
      setActionIssue(null);
      form.resetFields();
      moveToNextTask();
    } catch {
      message.error('反馈提交失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleConfirmIssue = async (issue: ReviewIssue) => {
    if (!detail) return;
    try {
      setSubmitting(true);
      await submitFeedback({
        taskId: detail.id,
        tenantId: 1,
        actionType: 'confirm',
        issueId: issue.id,
      });
      message.success('已确认该问题');
    } catch {
      message.error('确认失败');
    } finally {
      setSubmitting(false);
    }
  };

  const moveToNextTask = () => {
    const idx = tasks.findIndex((t) => t.id === selectedTaskId);
    const remaining = tasks.filter((t) => t.id !== selectedTaskId);
    setTasks(remaining);
    if (remaining.length > 0) {
      const nextIdx = Math.min(idx, remaining.length - 1);
      handleSelectTask(remaining[nextIdx].id);
    } else {
      setSelectedTaskId(null);
      setDetail(null);
    }
  };

  const getCardIssues = (categoryKey: string): ReviewIssue[] => {
    if (!detail) return [];
    if (detail.reviewCards) {
      const card = detail.reviewCards.find(
        (c) => c.category === categoryKey || c.categoryName === categoryKey
      );
      return card?.issues || [];
    }
    if (detail.issues) {
      return detail.issues.filter((i) => i.cardCategory === categoryKey);
    }
    return [];
  };

  const verdict = detail
    ? verdictConfig[detail.verdict] || { text: detail.verdict || '-', color: 'default' }
    : { text: '-', color: 'default' };

  const renderIssueActions = (issue: ReviewIssue) => (
    <div style={{ display: 'flex', gap: 8, marginTop: 8 }}>
      <Button
        type="primary"
        size="small"
        style={{ background: '#52c41a', borderColor: '#52c41a' }}
        icon={<CheckCircleOutlined />}
        loading={submitting}
        onClick={() => handleConfirmIssue(issue)}
      >
        确认
      </Button>
      <Button
        danger
        size="small"
        icon={<CloseCircleOutlined />}
        onClick={() => openActionModal('reject', issue)}
      >
        驳回
      </Button>
      <Button
        size="small"
        style={{ color: '#faad14', borderColor: '#faad14' }}
        icon={<EditOutlined />}
        onClick={() => openActionModal('modify', issue)}
      >
        修改
      </Button>
      <Button
        type="primary"
        size="small"
        icon={<PlusOutlined />}
        onClick={() => openActionModal('supplement', issue)}
      >
        补充
      </Button>
    </div>
  );

  const tabItems = cardCategories.map((cat) => {
    const issues = getCardIssues(cat.key);
    return {
      key: cat.key,
      label: (
        <Badge count={issues.length} size="small" offset={[6, -2]}>
          <span>
            {'\u2460\u2461\u2462\u2463\u2464'[cat.index - 1]} {cat.name}
          </span>
        </Badge>
      ),
      children: (
        <div>
          {issues.length === 0 ? (
            <div style={{ textAlign: 'center', padding: 40, color: '#52c41a', fontSize: 16 }}>
              <CheckCircleOutlined style={{ fontSize: 32, marginBottom: 8 }} />
              <div>该类别无问题发现</div>
            </div>
          ) : (
            issues.map((issue, idx) => {
              const sev = severityConfig[issue.severity] || severityConfig.info;
              return (
                <Card
                  key={issue.id || idx}
                  size="small"
                  style={{ marginBottom: 12, borderLeft: `4px solid ${sev.color}` }}
                >
                  <div style={{ display: 'flex', alignItems: 'flex-start', gap: 12 }}>
                    <Tag color={sev.color} icon={sev.icon} style={{ flexShrink: 0, marginTop: 2 }}>
                      {sev.label}
                    </Tag>
                    <div style={{ flex: 1 }}>
                      <div style={{ fontWeight: 600, fontSize: 14, marginBottom: 8 }}>
                        {issue.description}
                      </div>
                      {issue.originalText && (
                        <div
                          style={{
                            background: '#fff2f0',
                            border: '1px solid #ffccc7',
                            borderRadius: 4,
                            padding: '8px 12px',
                            marginBottom: 8,
                            fontSize: 13,
                          }}
                        >
                          <span style={{ color: '#999', marginRight: 8 }}>原文：</span>
                          <span style={{ background: '#ffa39e', padding: '1px 4px', borderRadius: 2 }}>
                            {issue.originalText}
                          </span>
                        </div>
                      )}
                      {issue.suggestion && (
                        <div
                          style={{
                            background: '#f6ffed',
                            border: '1px solid #b7eb8f',
                            borderRadius: 4,
                            padding: '8px 12px',
                            marginBottom: 8,
                            fontSize: 13,
                          }}
                        >
                          <span style={{ color: '#999', marginRight: 8 }}>建议：</span>
                          {issue.suggestion}
                        </div>
                      )}
                      <div style={{ display: 'flex', gap: 8, flexWrap: 'wrap' }}>
                        {issue.suggestionType && <Tag color="purple">{issue.suggestionType}</Tag>}
                        {issue.citationStatus && (
                          <Tag
                            color={
                              issue.citationStatus === 'verified'
                                ? 'green'
                                : issue.citationStatus === 'not_found'
                                ? 'red'
                                : 'default'
                            }
                          >
                            引用: {issue.citationStatus}
                          </Tag>
                        )}
                        {issue.ruleId && <Tag color="geekblue">规则: {issue.ruleId}</Tag>}
                      </div>
                      {renderIssueActions(issue)}
                    </div>
                  </div>
                </Card>
              );
            })
          )}
          <div style={{ textAlign: 'center', marginTop: 16 }}>
            <Button
              type="primary"
              icon={<PlusOutlined />}
              onClick={() => openActionModal('supplement', null)}
            >
              补充新问题
            </Button>
          </div>
        </div>
      ),
    };
  });

  const getActionModalTitle = () => {
    switch (actionType) {
      case 'reject':
        return '驳回问题';
      case 'modify':
        return '修改问题';
      case 'supplement':
        return '补充新问题';
      default:
        return '';
    }
  };

  return (
    <div>
      <h2 style={{ marginBottom: 16 }}>复核工作台</h2>
      <Layout style={{ background: '#fff', borderRadius: 8, minHeight: 'calc(100vh - 200px)' }}>
        <Sider
          width={320}
          style={{
            background: '#fafafa',
            borderRight: '1px solid #f0f0f0',
            overflow: 'auto',
            borderRadius: '8px 0 0 8px',
          }}
        >
          <div
            style={{
              padding: '16px',
              fontWeight: 600,
              fontSize: 15,
              borderBottom: '1px solid #f0f0f0',
            }}
          >
            待复核任务 ({tasks.length})
          </div>
          {tasksLoading ? (
            <div style={{ textAlign: 'center', padding: 40 }}>
              <Spin />
            </div>
          ) : tasks.length === 0 ? (
            <Empty description="暂无待复核任务" style={{ padding: 40 }} />
          ) : (
            <List
              dataSource={tasks}
              renderItem={(task) => (
                <List.Item
                  onClick={() => handleSelectTask(task.id)}
                  style={{
                    padding: '12px 16px',
                    cursor: 'pointer',
                    background: selectedTaskId === task.id ? '#e6f7ff' : 'transparent',
                    borderLeft: selectedTaskId === task.id ? '3px solid #1890ff' : '3px solid transparent',
                    transition: 'all 0.2s',
                  }}
                >
                  <div style={{ width: '100%' }}>
                    <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                      <span style={{ fontWeight: 600 }}>#{task.id}</span>
                      <Tag
                        color={
                          task.verdict === 'violation'
                            ? 'red'
                            : task.verdict === 'compliant'
                            ? 'green'
                            : 'orange'
                        }
                      >
                        {verdictConfig[task.verdict]?.text || task.verdict || '-'}
                      </Tag>
                    </div>
                    <div style={{ display: 'flex', justifyContent: 'space-between', marginTop: 4, fontSize: 12, color: '#666' }}>
                      <span>{task.documentType}</span>
                      <span style={{ color: '#ff4d4f', fontWeight: 600 }}>
                        风险: {task.riskScore ?? '-'}
                      </span>
                    </div>
                  </div>
                </List.Item>
              )}
            />
          )}
        </Sider>
        <Content style={{ padding: 24, overflow: 'auto' }}>
          {detailLoading ? (
            <div style={{ textAlign: 'center', padding: 100 }}>
              <Spin size="large" />
            </div>
          ) : !detail ? (
            <Empty description="请从左侧选择一个任务" style={{ padding: 100 }} />
          ) : (
            <div>
              <Card style={{ marginBottom: 16 }}>
                <Descriptions column={4} size="small">
                  <Descriptions.Item label="任务ID">{detail.id}</Descriptions.Item>
                  <Descriptions.Item label="文档类型">{detail.documentType}</Descriptions.Item>
                  <Descriptions.Item label="结论">
                    <Tag color={verdict.color} style={{ fontWeight: 600 }}>
                      {verdict.text}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="风险分">
                    <span style={{ fontWeight: 700, fontSize: 16, color: '#ff4d4f' }}>
                      {detail.riskScore ?? '-'}
                    </span>
                  </Descriptions.Item>
                  <Descriptions.Item label="风险等级">
                    <Tag
                      color={
                        detail.riskLevel === 'critical'
                          ? 'red'
                          : detail.riskLevel === 'high'
                          ? 'orange'
                          : detail.riskLevel === 'medium'
                          ? 'gold'
                          : 'green'
                      }
                    >
                      {detail.riskLevel || '-'}
                    </Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="内容类型">{detail.contentType}</Descriptions.Item>
                  <Descriptions.Item label="状态">
                    <Tag color="green">{detail.status || '-'}</Tag>
                  </Descriptions.Item>
                  <Descriptions.Item label="创建时间">
                    {detail.createdAt ? new Date(detail.createdAt).toLocaleString('zh-CN') : '-'}
                  </Descriptions.Item>
                </Descriptions>
              </Card>

              <Card
                title={
                  <span>
                    <FileTextOutlined style={{ marginRight: 8 }} />
                    原始内容
                  </span>
                }
                style={{ marginBottom: 16 }}
              >
                <div
                  style={{
                    maxHeight: 200,
                    overflow: 'auto',
                    whiteSpace: 'pre-wrap',
                    wordBreak: 'break-all',
                    fontSize: 13,
                    lineHeight: 1.8,
                    color: '#333',
                    background: '#fafafa',
                    padding: 16,
                    borderRadius: 4,
                  }}
                >
                  {detail.content || '暂无内容'}
                </div>
              </Card>

              <Card title="审查卡片">
                <Tabs items={tabItems} type="card" />
              </Card>
            </div>
          )}
        </Content>
      </Layout>

      <Modal
        title={getActionModalTitle()}
        open={actionModalVisible}
        onCancel={() => {
          setActionModalVisible(false);
          setActionType(null);
          setActionIssue(null);
          form.resetFields();
        }}
        footer={null}
        width={520}
      >
        <Form form={form} layout="vertical" onFinish={handleSubmitFeedback}>
          {actionType === 'reject' && (
            <Form.Item
              name="reason"
              label="驳回原因"
              rules={[{ required: true, message: '请输入驳回原因' }]}
            >
              <TextArea rows={4} placeholder="请输入驳回原因" />
            </Form.Item>
          )}
          {actionType === 'modify' && (
            <>
              <Form.Item
                name="severity"
                label="修改后严重程度"
                rules={[{ required: true, message: '请选择严重程度' }]}
              >
                <Select placeholder="请选择严重程度">
                  <Select.Option value="critical">严重</Select.Option>
                  <Select.Option value="major">重要</Select.Option>
                  <Select.Option value="minor">轻微</Select.Option>
                  <Select.Option value="info">提示</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item
                name="reason"
                label="修改原因"
                rules={[{ required: true, message: '请输入修改原因' }]}
              >
                <TextArea rows={3} placeholder="请输入修改原因" />
              </Form.Item>
            </>
          )}
          {actionType === 'supplement' && (
            <>
              <Form.Item
                name="cardCategory"
                label="所属卡片"
                rules={[{ required: true, message: '请选择卡片类别' }]}
              >
                <Select placeholder="请选择卡片类别">
                  {cardCategories.map((cat) => (
                    <Select.Option key={cat.key} value={cat.key}>
                      {cat.name}
                    </Select.Option>
                  ))}
                </Select>
              </Form.Item>
              <Form.Item
                name="severity"
                label="严重程度"
                rules={[{ required: true, message: '请选择严重程度' }]}
              >
                <Select placeholder="请选择严重程度">
                  <Select.Option value="critical">严重</Select.Option>
                  <Select.Option value="major">重要</Select.Option>
                  <Select.Option value="minor">轻微</Select.Option>
                  <Select.Option value="info">提示</Select.Option>
                </Select>
              </Form.Item>
              <Form.Item
                name="description"
                label="问题描述"
                rules={[{ required: true, message: '请输入问题描述' }]}
              >
                <TextArea rows={3} placeholder="请输入问题描述" />
              </Form.Item>
              <Form.Item name="suggestion" label="修改建议">
                <TextArea rows={2} placeholder="请输入修改建议（选填）" />
              </Form.Item>
            </>
          )}
          <Form.Item>
            <Button type="primary" htmlType="submit" loading={submitting}>
              提交反馈
            </Button>
          </Form.Item>
        </Form>
      </Modal>
    </div>
  );
};

export default ReviewQueue;

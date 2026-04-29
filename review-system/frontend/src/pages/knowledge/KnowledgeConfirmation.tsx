import React, { useEffect, useState } from 'react';
import {
  Card,
  Row,
  Col,
  Button,
  Form,
  Input,
  Select,
  Tag,
  Steps,
  Spin,
  Empty,
  message,
  Popconfirm,
} from 'antd';
import {
  CheckOutlined,
  EditOutlined,
  RollbackOutlined,
} from '@ant-design/icons';
import { getLawArticles, updateLawArticle, publishLawArticle } from '../../services/api';

const { TextArea } = Input;

interface Article {
  id: number;
  lawName: string;
  articleNumber: string;
  type: string;
  status: string;
  effectLevel: string;
  content: string;
  normType?: string;
  subject?: string;
  behavior?: string;
  keyPhrases?: string;
  violationExamples?: string;
  [key: string]: any;
}

const statusSteps = [
  { title: '草稿', key: 'draft' },
  { title: '待确认', key: 'pending' },
  { title: '已发布', key: 'published' },
];

const getStepIndex = (status: string) => {
  const idx = statusSteps.findIndex((s) => s.key === status);
  return idx >= 0 ? idx : 0;
};

const KnowledgeConfirmation: React.FC = () => {
  const [articles, setArticles] = useState<Article[]>([]);
  const [loading, setLoading] = useState(true);
  const [selectedId, setSelectedId] = useState<number | null>(null);
  const [editing, setEditing] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [form] = Form.useForm();

  useEffect(() => {
    fetchArticles();
  }, []);

  const fetchArticles = async () => {
    try {
      setLoading(true);
      const res = await getLawArticles({ status: 'draft' });
      const result = res.data?.data || res.data;
      const list = Array.isArray(result) ? result : result?.content || [];
      setArticles(list);
      if (list.length > 0 && !selectedId) {
        setSelectedId(list[0].id);
      }
    } catch {
      message.error('加载草稿法条失败');
      setArticles([]);
    } finally {
      setLoading(false);
    }
  };

  const selected = articles.find((a) => a.id === selectedId) || null;

  const handlePublish = async () => {
    if (!selected) return;
    try {
      setSubmitting(true);
      await publishLawArticle(selected.id);
      message.success('确认发布成功');
      const remaining = articles.filter((a) => a.id !== selected.id);
      setArticles(remaining);
      setSelectedId(remaining.length > 0 ? remaining[0].id : null);
    } catch {
      message.error('发布失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEditAndPublish = async () => {
    if (!selected) return;
    try {
      const values = await form.validateFields();
      setSubmitting(true);
      await updateLawArticle(selected.id, { ...selected, ...values });
      await publishLawArticle(selected.id);
      message.success('修改并发布成功');
      setEditing(false);
      const remaining = articles.filter((a) => a.id !== selected.id);
      setArticles(remaining);
      setSelectedId(remaining.length > 0 ? remaining[0].id : null);
    } catch {
      message.error('修改发布失败');
    } finally {
      setSubmitting(false);
    }
  };

  const handleReject = async () => {
    if (!selected) return;
    message.info('已退回重解析，状态保持为草稿');
  };

  const startEditing = () => {
    if (!selected) return;
    form.setFieldsValue({
      normType: selected.normType || '',
      subject: selected.subject || '',
      behavior: selected.behavior || '',
      keyPhrases: selected.keyPhrases || '',
      violationExamples: selected.violationExamples || '',
    });
    setEditing(true);
  };

  if (loading) {
    return (
      <div style={{ textAlign: 'center', padding: 100 }}>
        <Spin size="large" />
      </div>
    );
  }

  return (
    <div>
      <h2 style={{ marginBottom: 24 }}>知识确认工作台</h2>

      <Steps
        current={getStepIndex('draft')}
        items={statusSteps.map((s) => ({ title: s.title }))}
        style={{ marginBottom: 24, maxWidth: 400 }}
        size="small"
      />

      {articles.length === 0 ? (
        <Empty description="暂无待确认的草稿法条" />
      ) : (
        <Row gutter={16}>
          <Col span={6}>
            <Card title={`草稿列表 (${articles.length})`} size="small">
              {articles.map((article) => (
                <div
                  key={article.id}
                  onClick={() => {
                    setSelectedId(article.id);
                    setEditing(false);
                  }}
                  style={{
                    padding: '10px 12px',
                    cursor: 'pointer',
                    background: selectedId === article.id ? '#e6f7ff' : 'transparent',
                    borderLeft: selectedId === article.id ? '3px solid #1890ff' : '3px solid transparent',
                    borderBottom: '1px solid #f0f0f0',
                    transition: 'all 0.2s',
                  }}
                >
                  <div style={{ fontWeight: 600, fontSize: 13 }}>{article.lawName}</div>
                  <div style={{ fontSize: 12, color: '#666', marginTop: 2 }}>
                    {article.articleNumber} · <Tag color="default" style={{ fontSize: 11 }}>草稿</Tag>
                  </div>
                </div>
              ))}
            </Card>
          </Col>

          <Col span={18}>
            {selected ? (
              <Row gutter={16}>
                <Col span={12}>
                  <Card title="原始内容" size="small">
                    <div style={{ marginBottom: 12 }}>
                      <strong>{selected.lawName}</strong> - {selected.articleNumber}
                    </div>
                    <div
                      style={{
                        background: '#fafafa',
                        padding: 16,
                        borderRadius: 4,
                        whiteSpace: 'pre-wrap',
                        maxHeight: 500,
                        overflow: 'auto',
                        fontSize: 13,
                        lineHeight: 1.8,
                      }}
                    >
                      {selected.content || '暂无内容'}
                    </div>
                  </Card>
                </Col>
                <Col span={12}>
                  <Card
                    title="解析字段"
                    size="small"
                    extra={
                      !editing && (
                        <Button type="link" icon={<EditOutlined />} onClick={startEditing}>
                          编辑
                        </Button>
                      )
                    }
                  >
                    {editing ? (
                      <Form form={form} layout="vertical" size="small">
                        <Form.Item name="normType" label="规范类型">
                          <Select placeholder="请选择规范类型">
                            <Select.Option value="prohibitive">禁止性</Select.Option>
                            <Select.Option value="mandatory">强制性</Select.Option>
                            <Select.Option value="permissive">授权性</Select.Option>
                            <Select.Option value="guidance">指引性</Select.Option>
                          </Select>
                        </Form.Item>
                        <Form.Item name="subject" label="适用主体">
                          <Input placeholder="适用主体" />
                        </Form.Item>
                        <Form.Item name="behavior" label="规范行为">
                          <TextArea rows={2} placeholder="规范行为" />
                        </Form.Item>
                        <Form.Item name="keyPhrases" label="关键词组">
                          <TextArea rows={2} placeholder="关键词组，逗号分隔" />
                        </Form.Item>
                        <Form.Item name="violationExamples" label="违规示例">
                          <TextArea rows={3} placeholder="违规示例" />
                        </Form.Item>
                      </Form>
                    ) : (
                      <div style={{ fontSize: 13 }}>
                        <p><strong>规范类型：</strong>{selected.normType || '-'}</p>
                        <p><strong>适用主体：</strong>{selected.subject || '-'}</p>
                        <p><strong>规范行为：</strong>{selected.behavior || '-'}</p>
                        <p><strong>关键词组：</strong>{selected.keyPhrases || '-'}</p>
                        <p><strong>违规示例：</strong>{selected.violationExamples || '-'}</p>
                        <p><strong>类型：</strong>{selected.type || '-'}</p>
                        <p><strong>效力层级：</strong>{selected.effectLevel || '-'}</p>
                      </div>
                    )}
                  </Card>

                  <div style={{ marginTop: 16, display: 'flex', gap: 12 }}>
                    <Popconfirm title="确认直接发布？" onConfirm={handlePublish}>
                      <Button
                        type="primary"
                        icon={<CheckOutlined />}
                        loading={submitting}
                        style={{ background: '#52c41a', borderColor: '#52c41a' }}
                      >
                        确认发布
                      </Button>
                    </Popconfirm>
                    {editing && (
                      <Button
                        type="primary"
                        icon={<EditOutlined />}
                        loading={submitting}
                        onClick={handleEditAndPublish}
                      >
                        修改后发布
                      </Button>
                    )}
                    <Button icon={<RollbackOutlined />} onClick={handleReject}>
                      退回重解析
                    </Button>
                  </div>
                </Col>
              </Row>
            ) : (
              <Empty description="请从左侧选择一条法条" />
            )}
          </Col>
        </Row>
      )}
    </div>
  );
};

export default KnowledgeConfirmation;
